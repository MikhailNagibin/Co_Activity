package com.coactivity.service;

import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;
import com.coactivity.repository.RoomInvitationRepository;
import com.coactivity.repository.RoomRepository;
import com.coactivity.repository.UserRepository;
import com.coactivity.service.exception.AuthorizationException;
import com.coactivity.service.exception.ConflictException;
import com.coactivity.service.exception.NotificationDeliveryException;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomInvitationService {

  private final RoomRepository roomRepository;
  private final UserRepository userRepository;
  private final RoomInvitationRepository roomInvitationRepository;
  private final NotificationService notificationService;

  public RoomInvitationService(RoomRepository roomRepository,
      UserRepository userRepository,
      RoomInvitationRepository roomInvitationRepository,
      NotificationService notificationService) {
    this.roomRepository = roomRepository;
    this.userRepository = userRepository;
    this.roomInvitationRepository = roomInvitationRepository;
    this.notificationService = notificationService;
  }

  @Transactional
  public void inviteUserToRoom(Integer inviterId, Integer roomId, Integer invitedUserId) {
    Integer effectiveInviterId = requireUserId(inviterId, "Inviter id is required");
    Integer effectiveRoomId = requireRoomId(roomId);
    Integer effectiveInvitedUserId = requireUserId(invitedUserId, "Invited user id is required");

    Room room = getExistingRoom(effectiveRoomId);
    ensureOwnerPrivileges(effectiveInviterId, effectiveRoomId);
    User inviter = getExistingUser(effectiveInviterId);
    getExistingUser(effectiveInvitedUserId);

    if (roomRepository.isUserInMembers(effectiveRoomId, effectiveInvitedUserId)) {
      throw new ConflictException("ALREADY_MEMBER", "User is already a member of room");
    }
    if (roomRepository.isUserBannedInRoom(effectiveRoomId, effectiveInvitedUserId)) {
      throw new ConflictException("USER_BANNED", "User is banned in room");
    }

    roomInvitationRepository.createIfAbsent(effectiveRoomId, effectiveInvitedUserId,
        effectiveInviterId);

    boolean delivered = notificationService.sendRoomInvitationSync(
        effectiveInvitedUserId,
        room.getName(),
        inviter.getUserName(),
        effectiveRoomId);
    if (!delivered) {
      throw new NotificationDeliveryException("Failed to deliver room invitation email");
    }
  }

  private void ensureOwnerPrivileges(Integer inviterId, Integer roomId) {
    if (!roomRepository.isUserInMembers(roomId, inviterId)) {
      throw new AuthorizationException("ONLY_ROOM_OWNER", "Only room owner can invite users");
    }

    Role role = roomRepository.getUserRoleByRoomId(roomId, inviterId);
    if (role != Role.OWNER) {
      throw new AuthorizationException("ONLY_ROOM_OWNER", "Only room owner can invite users");
    }
  }

  private Room getExistingRoom(Integer roomId) {
    Room room = roomRepository.getRoomById(roomId);
    if (room == null) {
      throw new ResourceNotFoundException("ROOM_NOT_FOUND", "Room not found: " + roomId);
    }
    return room;
  }

  private User getExistingUser(Integer userId) {
    User user = userRepository.getUserById(userId);
    if (user == null) {
      throw new ResourceNotFoundException("USER_NOT_FOUND", "User not found: " + userId);
    }
    return user;
  }

  private Integer requireRoomId(Integer roomId) {
    if (roomId == null) {
      throw new ValidationException("Room id is required");
    }
    return roomId;
  }

  private Integer requireUserId(Integer userId, String message) {
    if (userId == null) {
      throw new ValidationException(message);
    }
    return userId;
  }
}
