package com.coactivity.service;

import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.controller.dto.response.JoinRequestResponse;
import com.coactivity.domain.RequestStatus;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.domain.RoomsRequest;
import com.coactivity.domain.User;
import com.coactivity.repository.impl.RoomRepositoryImpl;
import com.coactivity.repository.impl.RoomsRequestRepositoryImpl;
import com.coactivity.repository.impl.UserRepositoryImpl;
import com.coactivity.service.exception.AuthorizationException;
import com.coactivity.service.exception.JoinRequestValidationException;
import com.coactivity.service.exception.ResourceNotFoundException;
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

  private final RoomRepositoryImpl roomRepository;
  private final RoomsRequestRepositoryImpl roomsRequestRepository;
  private final UserRepositoryImpl userRepository;

  public JoinRequestService(RoomRepositoryImpl roomRepository,
      RoomsRequestRepositoryImpl roomsRequestRepository,
      UserRepositoryImpl userRepository) {
    this.roomRepository = roomRepository;
    this.roomsRequestRepository = roomsRequestRepository;
    this.userRepository = userRepository;
  }

  /**
   * Lists pending join requests across every private room moderated by the given user.
   *
   * @param userId administrator identifier
   * @return {@link ApiResponse} containing pending requests or an error status
   */
  public ApiResponse<List<JoinRequestResponse>> getPendingRequests(Integer userId) {
    if (userId == null) {
      throw new JoinRequestValidationException("User ID is required");
    }

    User admin = userRepository.getUserById(userId);
    if (admin == null) {
      throw new ResourceNotFoundException("Administrator not found");
    }

    List<Room> managedRooms = admin.getRooms();
    if (managedRooms == null || managedRooms.isEmpty()) {
      return ApiResponse.success(Collections.emptyList());
    }

    List<JoinRequestResponse> responses = new ArrayList<>();
    for (Room room : managedRooms) {
      if (room == null || room.isPublic() || !hasModerationRights(userId, room.getId())) {
        continue;
      }
      collectPendingForRoom(room.getId(), responses);
    }

    return ApiResponse.success(responses);
  }

  /**
   * Lists pending requests for a particular room, verifying moderator rights.
   *
   * @param userId moderator identifier
   * @param roomId room identifier
   * @return {@link ApiResponse} with pending requests or an error status
   */
  public ApiResponse<List<JoinRequestResponse>> getPendingRequestsForRoom(Integer userId,
      Integer roomId) {
    if (userId == null || roomId == null) {
      throw new JoinRequestValidationException("User ID and Room ID are required");
    }

    Room room = roomRepository.getRoomById(roomId);
    if (room == null) {
      throw new ResourceNotFoundException("Room not found");
    }
    if (!hasModerationRights(userId, roomId)) {
      throw new AuthorizationException("User lacks moderation rights");
    }

    List<JoinRequestResponse> responses = new ArrayList<>();
    collectPendingForRoom(roomId, responses);
    return ApiResponse.success(responses);
  }

  /**
   * Applies the requested action to a join request (accept, refuse, refuse with ban).
   *
   * @param userId    moderator identifier
   * @param requestId join request identifier
   * @param action    desired outcome
   * @return empty {@link ApiResponse} confirming success or failure
   */
  public ApiResponse<Void> processJoinRequest(Integer userId, Integer requestId,
      RequestStatus action) {
    if (userId == null || requestId == null || action == null) {
      throw new JoinRequestValidationException("Missing required parameters");
    }
    if (action == RequestStatus.CONSIDERATION) {
      throw new JoinRequestValidationException("Cannot process consideration status");
    }

    RoomsRequest request = roomsRequestRepository.getRequestById(requestId);
    if (request == null || request.getRoom() == null || request.getUser() == null) {
      throw new ResourceNotFoundException("Join request not found");
    }
    Integer roomId = request.getRoom().getId();
    if (!hasModerationRights(userId, roomId)) {
      throw new AuthorizationException("User lacks moderation rights");
    }
    if (request.getStatus() != RequestStatus.CONSIDERATION) {
      throw new JoinRequestValidationException("Request already processed");
    }

    Integer requesterId = request.getUser().getId();
    if (requesterId == null) {
      throw new JoinRequestValidationException("Requester ID missing");
    }

    return switch (action) {
      case ACCEPTED -> acceptRequest(requestId, roomId, requesterId);
      case REFUSED -> {
        roomsRequestRepository.updateRequest(requestId, RequestStatus.REFUSED);
        yield ApiResponse.success(null);
      }
      case REFUSED_WITH_BAN -> refuseWithBan(requestId, roomId, requesterId);
      default -> throw new JoinRequestValidationException("Unsupported action");
    };
  }

  /**
   * Returns all join requests created by the specified user.
   *
   * @param userId requester identifier
   * @return {@link ApiResponse} with the request history
   */
  public ApiResponse<List<JoinRequestResponse>> getSentRequests(Integer userId) {
    if (userId == null) {
      throw new JoinRequestValidationException("User ID is required");
    }

    User requester = userRepository.getUserById(userId);
    if (requester == null) {
      throw new ResourceNotFoundException("User not found");
    }

    List<RoomsRequest> requests = roomsRequestRepository.getRequestsByUser(userId);
    if (requests == null || requests.isEmpty()) {
      return ApiResponse.success(Collections.emptyList());
    }

    List<JoinRequestResponse> responses = new ArrayList<>();
    for (RoomsRequest request : requests) {
      if (request != null) {
        responses.add(mapToResponse(request));
      }
    }
    return ApiResponse.success(responses);
  }

  /**
   * Cancels a pending join request initiated by the user.
   *
   * @param userId    requester identifier
   * @param requestId join request identifier
   * @return empty {@link ApiResponse} indicating the outcome
   */
  public ApiResponse<Void> cancelRequest(Integer userId, Integer requestId) {
    if (userId == null || requestId == null) {
      throw new JoinRequestValidationException("User ID and Request ID are required");
    }

    RoomsRequest request = roomsRequestRepository.getRequestById(requestId);
    if (request == null || request.getUser() == null) {
      throw new ResourceNotFoundException("Join request not found");
    }
    if (!Objects.equals(request.getUser().getId(), userId)) {
      throw new AuthorizationException("Cannot cancel request created by another user");
    }
    if (request.getStatus() != RequestStatus.CONSIDERATION) {
      throw new JoinRequestValidationException("Only pending requests can be cancelled");
    }

    roomsRequestRepository.deleteRequest(requestId);
    return ApiResponse.success(null);
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

  private ApiResponse<Void> acceptRequest(Integer requestId, Integer roomId, Integer requesterId) {
    Room room = roomRepository.getRoomById(roomId);
    if (room == null) {
      throw new ResourceNotFoundException("Room not found");
    }

    int currentParticipants = room.getUsers() != null ? room.getUsers().size() : 0;
    if (currentParticipants >= room.getMaximumNumberOfPeople()) {
      throw new JoinRequestValidationException("Room capacity exceeded");
    }

    if (!roomRepository.isUserInMembers(roomId, requesterId)) {
      roomRepository.addUserToRoom(roomId, requesterId, Role.PARTICIPANT);
    }
    roomsRequestRepository.updateRequest(requestId, RequestStatus.ACCEPTED);
    return ApiResponse.success(null);
  }

  private ApiResponse<Void> refuseWithBan(Integer requestId, Integer roomId, Integer requesterId) {
    roomRepository.addUserBan(roomId, requesterId);
    roomsRequestRepository.updateRequest(requestId, RequestStatus.REFUSED_WITH_BAN);
    return ApiResponse.success(null);
  }

  private boolean hasModerationRights(Integer userId, Integer roomId) {
    if (userId == null || roomId == null) {
      throw new JoinRequestValidationException("User ID and Room ID are required");
    }
    try {
      Role role = roomRepository.getUserRoleByRoomId(roomId, userId);
      return role == Role.OWNER || role == Role.ADMIN;
    } catch (Exception e) {
      throw new AuthorizationException("Unable to determine user role", e);
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
