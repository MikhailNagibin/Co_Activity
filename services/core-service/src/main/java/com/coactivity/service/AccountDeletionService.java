package com.coactivity.service;

import com.coactivity.controller.dto.request.AccountDeletionMode;
import com.coactivity.controller.dto.request.AccountDeletionRequest;
import com.coactivity.controller.dto.request.AccountDeletionRoomActionRequest;
import com.coactivity.controller.dto.response.AccountDeletionPreviewResponse;
import com.coactivity.controller.dto.response.AccountDeletionTransferCandidateResponse;
import com.coactivity.controller.dto.response.OwnedRoomDeletionPreviewResponse;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;
import com.coactivity.repository.RoomRepository;
import com.coactivity.repository.UserRepository;
import com.coactivity.service.exception.ConflictException;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.ValidationException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountDeletionService {

  private final UserRepository userRepository;
  private final RoomRepository roomRepository;
  private final NotificationService notificationService;
  private final UserAvatarService userAvatarService;
  private final RoomImageService roomImageService;

  public AccountDeletionService(UserRepository userRepository, RoomRepository roomRepository,
      NotificationService notificationService,
      UserAvatarService userAvatarService,
      RoomImageService roomImageService) {
    this.userRepository = userRepository;
    this.roomRepository = roomRepository;
    this.notificationService = notificationService;
    this.userAvatarService = userAvatarService;
    this.roomImageService = roomImageService;
  }

  @Transactional(readOnly = true)
  public AccountDeletionPreviewResponse getDeletionPreview(Integer userId) {
    requireExistingUser(userId);
    List<OwnedRoomDeletionPreviewResponse> ownedRooms = loadOwnedRoomPreviews(userId);
    return new AccountDeletionPreviewResponse(ownedRooms.isEmpty(), ownedRooms);
  }

  @Transactional
  public void deleteAccountIfNoOwnedRooms(Integer userId) {
    requireExistingUser(userId);
    List<Room> ownedRooms = loadOwnedRooms(userId);
    if (!ownedRooms.isEmpty()) {
      throw new ConflictException("OWNED_ROOMS_RESOLUTION_REQUIRED",
          "Resolve owned rooms before deleting the account");
    }
    userAvatarService.deleteAvatar(userId);
    userRepository.deleteUser(userId);
  }

  @Transactional
  public void deleteAccount(Integer userId, AccountDeletionRequest request) {
    requireExistingUser(userId);
    if (request == null || request.getActions() == null) {
      throw new ValidationException("Account deletion actions are required");
    }

    Map<Integer, Room> ownedRoomsById = loadOwnedRoomsById(userId);
    if (ownedRoomsById.isEmpty()) {
      throw new ValidationException("This account can be deleted without room actions");
    }

    Map<Integer, AccountDeletionRoomActionRequest> actionsByRoomId =
        mapAndValidateActions(request.getActions());

    if (actionsByRoomId.size() != ownedRoomsById.size()
        || !actionsByRoomId.keySet().equals(ownedRoomsById.keySet())) {
      throw new ValidationException("Deletion actions must cover every owned room exactly once");
    }

    List<Integer> orderedRoomIds = ownedRoomsById.keySet().stream()
        .sorted()
        .toList();

    for (Integer roomId : orderedRoomIds) {
      AccountDeletionRoomActionRequest action = actionsByRoomId.get(roomId);
      Room room = ownedRoomsById.get(roomId);
      if (action.getMode() == AccountDeletionMode.DELETE_ROOM) {
        validateDeleteRoomAction(action);
        deleteOwnedRoom(userId, room);
        continue;
      }
      if (action.getMode() == AccountDeletionMode.TRANSFER_OWNERSHIP) {
        transferOwnership(userId, roomId, action.getTransferToUserId());
        continue;
      }
      throw new ValidationException("Unsupported account deletion mode");
    }

    userAvatarService.deleteAvatar(userId);
    userRepository.deleteUser(userId);
  }

  private Map<Integer, Room> loadOwnedRoomsById(Integer userId) {
    Map<Integer, Room> ownedRoomsById = new LinkedHashMap<>();
    for (Room room : loadOwnedRooms(userId)) {
      if (room != null && room.getId() != null) {
        ownedRoomsById.put(room.getId(), room);
      }
    }
    return ownedRoomsById;
  }

  private List<OwnedRoomDeletionPreviewResponse> loadOwnedRoomPreviews(Integer userId) {
    List<OwnedRoomDeletionPreviewResponse> previews = new ArrayList<>();
    for (Room room : loadOwnedRooms(userId)) {
      if (room == null || room.getId() == null) {
        continue;
      }
      Map<User, Role> roomUsers = loadRoomUsers(room.getId());
      List<AccountDeletionTransferCandidateResponse> candidates = roomUsers.entrySet().stream()
          .filter(entry -> isTransferCandidate(userId, entry))
          .sorted(Comparator
              .comparing((Entry<User, Role> entry) -> entry.getValue() == Role.ADMIN ? 0 : 1)
              .thenComparing(entry -> safeUserName(entry.getKey())))
          .map(entry -> new AccountDeletionTransferCandidateResponse(
              entry.getKey().getId(),
              entry.getKey().getUserName(),
              entry.getValue()))
          .toList();
      previews.add(new OwnedRoomDeletionPreviewResponse(
          room.getId(),
          room.getName(),
          roomUsers.size(),
          candidates));
    }

    previews.sort(Comparator.comparing(OwnedRoomDeletionPreviewResponse::getRoomId));
    return previews;
  }

  private boolean isTransferCandidate(Integer currentUserId, Entry<User, Role> entry) {
    User user = entry.getKey();
    Role role = entry.getValue();
    return user != null
        && user.getId() != null
        && !user.getId().equals(currentUserId)
        && (role == Role.ADMIN || role == Role.PARTICIPANT);
  }

  private String safeUserName(User user) {
    return user != null && user.getUserName() != null
        ? user.getUserName().toLowerCase(Locale.ROOT)
        : "";
  }

  private Map<Integer, AccountDeletionRoomActionRequest> mapAndValidateActions(
      List<AccountDeletionRoomActionRequest> actions) {
    Map<Integer, AccountDeletionRoomActionRequest> actionsByRoomId = new LinkedHashMap<>();
    for (AccountDeletionRoomActionRequest action : actions) {
      if (action == null || action.getRoomId() == null || action.getMode() == null) {
        throw new ValidationException("Each deletion action must include roomId and mode");
      }
      if (actionsByRoomId.putIfAbsent(action.getRoomId(), action) != null) {
        throw new ValidationException("Deletion actions contain duplicate room IDs");
      }
    }
    return actionsByRoomId;
  }

  private void validateDeleteRoomAction(AccountDeletionRoomActionRequest action) {
    if (action.getTransferToUserId() != null) {
      throw new ValidationException("Delete room action must not include transfer target");
    }
  }

  private void transferOwnership(Integer currentUserId, Integer roomId, Integer transferToUserId) {
    if (transferToUserId == null) {
      throw new ValidationException("Transfer ownership action requires transferToUserId");
    }
    if (transferToUserId.equals(currentUserId)) {
      throw new ConflictException("INVALID_OWNERSHIP_TRANSFER",
          "Ownership cannot be transferred to the deleting user");
    }
    if (!roomRepository.isUserInMembers(roomId, transferToUserId)) {
      throw new ConflictException("INVALID_OWNERSHIP_TRANSFER",
          "Ownership can only be transferred to an existing room participant");
    }

    Role targetRole = roomRepository.getUserRoleByRoomId(roomId, transferToUserId);
    if (targetRole != Role.ADMIN && targetRole != Role.PARTICIPANT) {
      throw new ConflictException("INVALID_OWNERSHIP_TRANSFER",
          "Ownership can only be transferred to a room admin or participant");
    }

    roomRepository.setRoleByUserIdAndRoomId(transferToUserId, roomId, Role.OWNER);
    roomRepository.setRoleByUserIdAndRoomId(currentUserId, roomId, Role.PARTICIPANT);
  }

  private void deleteOwnedRoom(Integer currentUserId, Room room) {
    if (room == null || room.getId() == null) {
      throw new ResourceNotFoundException("Room not found");
    }

    List<Integer> participantIds = loadRoomUsers(room.getId()).keySet().stream()
        .filter(user -> user != null && user.getId() != null && !user.getId().equals(currentUserId))
        .map(User::getId)
        .distinct()
        .toList();

    roomImageService.deleteAllImagesForRoom(room.getId());
    roomRepository.deleteRoom(room.getId());

    for (Integer participantId : participantIds) {
      notificationService.sendActivityClosed(participantId, room.getName());
    }
  }

  private void requireExistingUser(Integer userId) {
    if (userId == null) {
      throw new ValidationException("User id is required");
    }
    if (userRepository.getUserById(userId) == null) {
      throw new ResourceNotFoundException("User not found");
    }
  }

  private Map<User, Role> loadRoomUsers(Integer roomId) {
    Map<User, Role> roomUsers = roomRepository.getUsersInRoom(roomId);
    return roomUsers != null ? roomUsers : Map.of();
  }

  private List<Room> loadOwnedRooms(Integer userId) {
    List<Room> ownedRooms = roomRepository.getRoomsOwnedByUser(userId);
    return ownedRooms != null ? ownedRooms : List.of();
  }
}
