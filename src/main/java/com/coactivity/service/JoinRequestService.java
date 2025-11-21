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
      return ApiResponse.error("400");
    }

    try {
      User admin = userRepository.getUserById(userId);
      if (admin == null) {
        return ApiResponse.error("404");
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
    } catch (IllegalArgumentException e) {
      return ApiResponse.error("400");
    } catch (Exception e) {
      return ApiResponse.error("500");
    }
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
      return ApiResponse.error("400");
    }

    try {
      Room room = roomRepository.getRoomById(roomId);
      if (room == null) {
        return ApiResponse.error("404");
      }
      if (!hasModerationRights(userId, roomId)) {
        return ApiResponse.error("403");
      }

      List<JoinRequestResponse> responses = new ArrayList<>();
      collectPendingForRoom(roomId, responses);
      return ApiResponse.success(responses);
    } catch (IllegalArgumentException e) {
      return ApiResponse.error("400");
    } catch (Exception e) {
      return ApiResponse.error("500");
    }
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
      return ApiResponse.error("400");
    }
    if (action == RequestStatus.CONSIDERATION) {
      return ApiResponse.error("400");
    }

    try {
      RoomsRequest request = roomsRequestRepository.getRequestById(requestId);
      if (request == null || request.getRoom() == null || request.getUser() == null) {
        return ApiResponse.error("404");
      }
      Integer roomId = request.getRoom().getId();
      if (!hasModerationRights(userId, roomId)) {
        return ApiResponse.error("403");
      }
      if (request.getStatus() != RequestStatus.CONSIDERATION) {
        return ApiResponse.error("409");
      }

      Integer requesterId = request.getUser().getId();
      if (requesterId == null) {
        return ApiResponse.error("400");
      }

      return switch (action) {
        case ACCEPTED -> acceptRequest(requestId, roomId, requesterId);
        case REFUSED -> {
          roomsRequestRepository.updateRequest(requestId, RequestStatus.REFUSED);
          yield ApiResponse.success(null);
        }
        case REFUSED_WITH_BAN -> refuseWithBan(requestId, roomId, requesterId);
        default -> ApiResponse.error("400");
      };
    } catch (IllegalArgumentException e) {
      return ApiResponse.error("400");
    } catch (Exception e) {
      return ApiResponse.error("500");
    }
  }

  /**
   * Returns all join requests created by the specified user.
   *
   * @param userId requester identifier
   * @return {@link ApiResponse} with the request history
   */
  public ApiResponse<List<JoinRequestResponse>> getSentRequests(Integer userId) {
    if (userId == null) {
      return ApiResponse.error("400");
    }

    try {
      User requester = userRepository.getUserById(userId);
      if (requester == null) {
        return ApiResponse.error("404");
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
    } catch (IllegalArgumentException e) {
      return ApiResponse.error("400");
    } catch (Exception e) {
      return ApiResponse.error("500");
    }
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
      return ApiResponse.error("400");
    }

    try {
      RoomsRequest request = roomsRequestRepository.getRequestById(requestId);
      if (request == null || request.getUser() == null) {
        return ApiResponse.error("404");
      }
      if (!Objects.equals(request.getUser().getId(), userId)) {
        return ApiResponse.error("403");
      }
      if (request.getStatus() != RequestStatus.CONSIDERATION) {
        return ApiResponse.error("409");
      }

      roomsRequestRepository.deleteRequest(requestId);
      return ApiResponse.success(null);
    } catch (IllegalArgumentException e) {
      return ApiResponse.error("400");
    } catch (Exception e) {
      return ApiResponse.error("500");
    }
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
      return ApiResponse.error("404");
    }

    int currentParticipants = room.getUsers() != null ? room.getUsers().size() : 0;
    if (currentParticipants >= room.getMaximumNumberOfPeople()) {
      return ApiResponse.error("409");
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
      return false;
    }
    try {
      Role role = roomRepository.getUserRoleByRoomId(roomId, userId);
      return role == Role.OWNER || role == Role.ADMIN;
    } catch (Exception e) {
      return false;
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