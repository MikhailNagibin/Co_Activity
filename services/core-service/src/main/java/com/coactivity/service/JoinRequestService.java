package com.coactivity.service;

import com.coactivity.DataRepository;
import com.coactivity.controller.dto.response.JoinRequestResponse;
import com.coactivity.domain.RequestStatus;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.domain.RoomsRequest;
import com.coactivity.domain.User;
import com.coactivity.repository.RoomRepository;
import com.coactivity.repository.RoomsRequestRepository;
import com.coactivity.repository.UserRepository;
import com.coactivity.service.exception.AuthorizationException;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.ValidationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Manages administrative review and user tracking of room membership requests.
 * <p>
 * Couples repository-layer access with business validation: administrators can inspect and process
 * pending requests, while users can monitor and cancel their own submissions.
 * </p>
 */
@Service
public class JoinRequestService {

  private final DataRepository dataRepository;
  private final RoomRepository roomRepository;
  private final RoomsRequestRepository roomsRequestRepository;
  private final UserRepository userRepository;
  private final NotificationService notificationService;

  public JoinRequestService(DataRepository dataRepository,
      RoomRepository roomRepository,
      RoomsRequestRepository roomsRequestRepository,
      UserRepository userRepository,
      NotificationService notificationService) {
    this.dataRepository = dataRepository;
    this.roomRepository = roomRepository;
    this.roomsRequestRepository = roomsRequestRepository;
    this.userRepository = userRepository;
    this.notificationService = notificationService;
  }

  /**
   * Lists pending join requests across every private room moderated by the given user.
   *
   * @param userId administrator identifier
   * @return pending requests for all moderated rooms
   */
  public List<JoinRequestResponse> getPendingRequests(Integer userId) {
    Integer adminId = requireUserId(userId);
    User admin = getExistingUser(adminId);

    List<Room> managedRooms = admin.getRooms();

    if (managedRooms == null || managedRooms.isEmpty()) {
      return Collections.emptyList();
    }

    List<JoinRequestResponse> responses = new ArrayList<>();
    for (Room room : managedRooms) {
      if (room == null || room.isPublic()) {
        continue;
      }
      if (!hasModerationRights(adminId, room.getId())) {
        continue;
      }
      collectPendingForRoom(room.getId(), responses);
    }

    return responses;
  }

  /**
   * Lists pending requests for a particular room, verifying moderator rights.
   *
   * @param userId moderator identifier
   * @param roomId room identifier
   * @return pending requests for the room
   */
  public List<JoinRequestResponse> getPendingRequestsForRoom(Integer userId,
      Integer roomId) {
    Integer moderatorId = requireUserId(userId);
    Room room = getExistingRoom(roomId);
    ensureModerationRights(moderatorId, room.getId());

    List<JoinRequestResponse> responses = new ArrayList<>();
    collectPendingForRoom(room.getId(), responses);
    return responses;
  }

  /**
   * Applies the requested action to a join request (accept, refuse, refuse with ban).
   *
   * @param userId    moderator identifier
   * @param requestId join request identifier
   * @param action    desired outcome
   */
  public void processJoinRequest(Integer userId, Integer requestId,
      RequestStatus action) {
    Integer moderatorId = requireUserId(userId);
    Integer effectiveRequestId = requireRequestId(requestId);
    RequestStatus effectiveAction = requireAction(action);

    RoomsRequest request = getExistingRequest(effectiveRequestId);
    ensurePending(request);

    Integer roomId = request.getRoom().getId();
    ensureModerationRights(moderatorId, roomId);

    Integer requesterId = request.getUser().getId();

    switch (effectiveAction) {
      case ACCEPTED -> acceptRequest(effectiveRequestId, roomId, requesterId);
      case REFUSED -> {
        roomsRequestRepository.updateRequest(effectiveRequestId, RequestStatus.REFUSED);
        Room room = getExistingRoom(roomId);
        notificationService.sendMembershipRejected(requesterId, room.getName());
      }
      case REFUSED_WITH_BAN -> refuseWithBan(effectiveRequestId, roomId, requesterId);
      default -> throw new ValidationException("Unsupported join request action");
    }
  }

  /**
   * Returns all join requests created by the specified user.
   *
   * @param userId requester identifier
   * @return request history for the user
   */
  public List<JoinRequestResponse> getSentRequests(Integer userId) {
    Integer requesterId = requireUserId(userId);
    getExistingUser(requesterId);

    List<RoomsRequest> requests = roomsRequestRepository.getRequestsByUser(requesterId);
    if (requests == null || requests.isEmpty()) {
      return Collections.emptyList();
    }

    List<JoinRequestResponse> responses = new ArrayList<>();
    for (RoomsRequest request : requests) {
      if (request != null) {
        responses.add(mapToResponse(request));
      }
    }
    return responses;
  }

  /**
   * Cancels a pending join request initiated by the user.
   *
   * @param userId    requester identifier
   * @param requestId join request identifier
   */
  public void cancelRequest(Integer userId, Integer requestId) {
    Integer requesterId = requireUserId(userId);
    Integer effectiveRequestId = requireRequestId(requestId);

    RoomsRequest request = getExistingRequest(effectiveRequestId);
    if (!Objects.equals(request.getUser().getId(), requesterId)) {
      throw new AuthorizationException("Cannot cancel request created by another user");
    }
    ensurePending(request);

    roomsRequestRepository.deleteRequest(effectiveRequestId);
  }

  private void collectPendingForRoom(Integer roomId, List<JoinRequestResponse> target) {
    List<RoomsRequest> roomRequests = roomsRequestRepository.getRoomRequests(roomId);
    if (roomRequests == null || roomRequests.isEmpty()) {
      return;
    }
    for (RoomsRequest request : roomRequests) {
      if (request != null && request.getStatus() == RequestStatus.CONSIDERATION) {
        target.add(mapToResponse(request));
      }
    }
  }

  private void acceptRequest(Integer requestId, Integer roomId, Integer requesterId) {
    Room room = getExistingRoom(roomId);

    dataRepository.inTransaction(connection -> {
      int currentParticipants = roomRepository.getRoomParticipantCountInTransaction(connection,
          roomId);
      if (currentParticipants >= room.getMaximumNumberOfPeople()) {
        throw new ValidationException("Room capacity exceeded");
      }

      if (!roomRepository.isUserInMembersInTransaction(connection, roomId, requesterId)) {
        roomRepository.addUserToRoomInTransaction(connection, roomId, requesterId,
            Role.PARTICIPANT);
      }

      roomsRequestRepository.updateRequestInTransaction(connection, requestId,
          RequestStatus.ACCEPTED);
      return null;
    });

    notificationService.sendMembershipAccepted(requesterId, room.getName());
  }

  private void refuseWithBan(Integer requestId, Integer roomId, Integer requesterId) {
    Room room = getExistingRoom(roomId);

    dataRepository.inTransaction(connection -> {
      roomRepository.addUserBanInTransaction(connection, roomId, requesterId);
      roomsRequestRepository.updateRequestInTransaction(connection, requestId,
          RequestStatus.REFUSED_WITH_BAN);
      return null;
    });

    notificationService.sendMembershipRejected(requesterId, room.getName());
  }

  /**
   * Returns true if the user has moderation rights (OWNER or ADMIN) in the room. Never throws;
   * returns false if the role cannot be determined.
   */
  private boolean hasModerationRights(Integer userId, Integer roomId) {
    if (!roomRepository.isUserInMembers(roomId, userId)) {
      return false;
    }

    Role role = roomRepository.getUserRoleByRoomId(roomId, userId);
    return role == Role.OWNER || role == Role.ADMIN;
  }

  private void ensureModerationRights(Integer userId, Integer roomId) {
    if (!hasModerationRights(userId, roomId)) {
      throw new AuthorizationException("User lacks moderation rights");
    }
  }

  private Integer requireUserId(Integer userId) {
    if (userId == null) {
      throw new ValidationException("User ID is required");
    }
    return userId;
  }

  private Integer requireRequestId(Integer requestId) {
    if (requestId == null) {
      throw new ValidationException("Request ID is required");
    }
    return requestId;
  }

  private RequestStatus requireAction(RequestStatus action) {
    if (action == null) {
      throw new ValidationException("Action is required");
    }
    if (action == RequestStatus.CONSIDERATION) {
      throw new ValidationException("Cannot process consideration status");
    }
    return action;
  }

  private User getExistingUser(Integer userId) {
    User user = userRepository.getUserById(userId);
    if (user == null) {
      throw new ResourceNotFoundException("User not found");
    }
    return user;
  }

  private Room getExistingRoom(Integer roomId) {
    if (roomId == null) {
      throw new ValidationException("Room ID is required");
    }
    Room room = roomRepository.getRoomById(roomId);
    if (room == null) {
      throw new ResourceNotFoundException("Room not found");
    }
    return room;
  }

  private RoomsRequest getExistingRequest(Integer requestId) {
    RoomsRequest request = roomsRequestRepository.getRequestById(requestId);
    if (request == null || request.getRoom() == null || request.getUser() == null) {
      throw new ResourceNotFoundException("Join request not found");
    }
    return request;
  }

  private void ensurePending(RoomsRequest request) {
    if (request.getStatus() != RequestStatus.CONSIDERATION) {
      throw new ValidationException("Join request already processed");
    }
  }

  private JoinRequestResponse mapToResponse(RoomsRequest request) {
    if (request == null) {
      return null;
    }
    User applicant = request.getUser();
    Room room = request.getRoom();

    return new JoinRequestResponse(
        request.getId(),
        applicant != null ? applicant.getId() : null,
        applicant != null ? applicant.getUserName() : null,
        room != null ? room.getId() : null,
        room != null ? room.getName() : null,
        request.getStatus(),
        request.getCreatedAt());
  }
}
