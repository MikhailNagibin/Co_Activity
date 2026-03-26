package com.coactivity.service;

import com.coactivity.controller.dto.response.BulletinBoardResponse;
import com.coactivity.controller.dto.response.MembershipVerificationResponse;
import com.coactivity.controller.dto.response.RoleAssignmentResponse;
import com.coactivity.controller.dto.response.RoomDetailedResponse;
import com.coactivity.controller.dto.response.RoomParticipantResponse;
import com.coactivity.controller.dto.response.RoomSummaryResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.domain.*;
import com.coactivity.repository.impl.BulletinBoardRepositoryImpl;
import com.coactivity.repository.impl.PictureRepositoryImpl;
import com.coactivity.repository.impl.RoomRepositoryImpl;
import com.coactivity.repository.impl.RoomsRequestRepositoryImpl;
import com.coactivity.repository.impl.UserRepositoryImpl;
import com.coactivity.service.exception.AuthorizationException;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.ValidationException;

import java.lang.reflect.Member;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class UserWithRoomService {

    private final UserRepositoryImpl userRepository;
    private final RoomRepositoryImpl roomRepository;
    private final RoomsRequestRepositoryImpl roomsRequestRepository;
    private final PictureRepositoryImpl pictureRepository;
    private final BulletinBoardRepositoryImpl bulletinBoardRepository;
    private final NotificationService notificationService;

    public UserWithRoomService(UserRepositoryImpl userRepository,
                               RoomRepositoryImpl roomRepository,
                               RoomsRequestRepositoryImpl roomsRequestRepository,
                               PictureRepositoryImpl pictureRepository,
                               BulletinBoardRepositoryImpl bulletinBoardRepository,
                               NotificationService notificationService) {
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.roomsRequestRepository = roomsRequestRepository;
        this.pictureRepository = pictureRepository;
        this.bulletinBoardRepository = bulletinBoardRepository;
        this.notificationService = notificationService;
    }

    public RoleAssignmentResponse assignAdminRole(Integer requesterId, Integer roomId,
                                                  Integer targetUserId) {
        Integer ownerId = requireOwner(requesterId, roomId);
        getExistingUser(targetUserId);

        if (!roomRepository.isUserInMembers(roomId, targetUserId)) {
            throw new ValidationException("Target user is not a member of the room and cannot be assigned admin role.");
        }
        Role previousRole = roomRepository.getUserRoleByRoomId(roomId, targetUserId);
        roomRepository.setRoleByUserIdAndRoomId(targetUserId, roomId, Role.ADMIN);
        return new RoleAssignmentResponse(targetUserId, roomId, Role.ADMIN, Role.PARTICIPANT, ownerId);
    }

    public RoleAssignmentResponse demoteAdminRole(Integer requesterId, Integer roomId,
                                                  Integer targetUserId) {
        Integer ownerId = requireOwner(requesterId, roomId);
        getExistingUser(targetUserId);

        Role previousRole = roomRepository.getUserRoleByRoomId(roomId, targetUserId);
        roomRepository.setRoleByUserIdAndRoomId(targetUserId, roomId, Role.PARTICIPANT);
        return new RoleAssignmentResponse(targetUserId, roomId, Role.PARTICIPANT, previousRole.getPermissions(), ownerId);
    }

    public boolean isUserInRoom(Integer requesterId, Integer roomId) {
        validateRoomId(roomId);
        getExistingRoom(roomId);
        return roomRepository.isUserInMembers(roomId, requesterId);
    }

    public MembershipVerificationResponse verifyUserMembership(Integer requesterId,
                                                               Integer roomId, Integer targetUserId) {
        validateRoomId(roomId);
        Integer adminId = requireRoomParticipant(requesterId, roomId);
        enforceAdminPrivileges(roomId, adminId);

        User targetUser = getExistingUser(targetUserId);
        boolean isMember = roomRepository.isUserInMembers(roomId, targetUserId);
        Role memberRole = isMember ? roomRepository.getUserRoleByRoomId(roomId, targetUserId) : null;

        Room room = getExistingRoom(roomId);
        return new MembershipVerificationResponse(isMember, memberRole,
            mapUserToSummaryResponse(targetUser), room.getName());
    }

    public void joinRoom(Integer userId, Integer roomId) {
        validateRoomId(roomId);
        User user = getExistingUser(userId);
        Room room = getExistingRoom(roomId);

        enforceJoinEligibility(user, room);

        if (roomRepository.isUserInMembers(roomId, userId)) {
            return;
        }

        if (room.isPublic()) {
            roomRepository.addUserToRoom(roomId, userId, Role.PARTICIPANT);
        } else {
            roomsRequestRepository.createRequest(userId, roomId, RequestStatus.CONSIDERATION);

            if (room.getMembers() != null) {
                for (RoomMember member : room.getMembers()) {
                    User roomUser = member.getUser();
                    Role role = member.getRole();
                    if (roomUser != null && roomUser.getId() != null) {
                        if (role.getPermissions().equals(Role.OWNER) || role.getPermissions().equals(Role.ADMIN)) {
                            notificationService.sendNewJoinRequest(roomUser.getId(), room.getName(), user.getUserName());
                        }
                    }
                }
            }
        }
    }

    public List<RoomSummaryResponse> getBanRooms(Integer userId) {
        Integer requesterId = requireUserId(userId);
        List<Room> rooms = roomRepository.getAllRooms();
        if (rooms == null || rooms.isEmpty()) {
            return Collections.emptyList();
        }

        List<RoomSummaryResponse> bannedRooms = new ArrayList<>();

        for (Room room : rooms) {
            if (room.getBans() != null) {
                for (Ban ban : room.getBans()) {
                    if (ban.getUser().getId().equals(userId)) {
                        RoomSummaryResponse response = mapRoomToSummaryResponse(room, requesterId);
                        bannedRooms.add(response);
                        break;
                    }
                }
            }
        }

        return bannedRooms;
    }

    public List<RoomDetailedResponse> getUserRooms(Integer userId) {
        User user = getExistingUser(userId);
        List<RoomMember> rooms = user.getRooms();
        if (rooms == null || rooms.isEmpty()) {
            return Collections.emptyList();
        }
        List<RoomDetailedResponse> response = new ArrayList<>();
        for (RoomMember member : rooms) {
            RoomDetailedResponse curResponse = mapRoomToDetailedResponse(member.getRoom(), userId, true);
            response.add(curResponse);
        }
        return response;
    }

    public void leaveRoom(Integer userId, Integer roomId) {
        validateRoomId(roomId);
        Room room = getExistingRoom(roomId);
        ensureParticipant(room, userId);

        Role role = roomRepository.getUserRoleByRoomId(roomId, userId);
        if (role.getPermissions().equals(Role.OWNER)) {
            throw new AuthorizationException("Room owner cannot leave the room");
        }

        roomRepository.removeUserFromRoom(roomId, userId);
    }

    public List<RoomParticipantResponse> getRoomParticipants(Integer requesterId, Integer roomId,
                                                             Role roleFilter) {
        validateRoomId(roomId);
        Integer adminId = requireRoomParticipant(requesterId, roomId);
        enforceAdminPrivileges(roomId, adminId);

        Room room = getExistingRoom(roomId);
        System.out.println(room);
        Map<User, Role> users = roomRepository.getUsersInRoom(roomId);
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }

        return users.entrySet().stream()
            .filter(entry -> roleFilter == null || entry.getValue() == roleFilter)
            .map(entry -> mapParticipant(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }

    private Integer requireOwner(Integer requesterId, Integer roomId) {
        Integer ownerId = requireRoomParticipant(requesterId, roomId);
        Role requesterRole = roomRepository.getUserRoleByRoomId(roomId, ownerId);
        if (!requesterRole.getPermissions().equals(Role.OWNER)) {
            throw new AuthorizationException("Only room owner can perform this action");
        }
        return ownerId;
    }

    private Integer requireRoomParticipant(Integer requesterId, Integer roomId) {
        validateRoomId(roomId);
        Integer userId = requireUserId(requesterId);
        if (!roomRepository.isUserInMembers(roomId, userId)) {
            throw new AuthorizationException("User is not a participant of room " + roomId);
        }
        return userId;
    }

    private void enforceAdminPrivileges(Integer roomId, Integer participantId) {
        Role requesterRole = roomRepository.getUserRoleByRoomId(roomId, participantId);
        if (!requesterRole.getPermissions().equals(Role.OWNER) && !requesterRole.getPermissions().equals(Role.ADMIN)) {
            throw new AuthorizationException("Insufficient privileges for room " + roomId);
        }
    }

    private void enforceJoinEligibility(User user, Room room) {
        if (room.getBans() != null && room.getBans().contains(user)) {
            throw new AuthorizationException("User is banned from this room");
        }

        int currentParticipants = room.getMembers() != null ? room.getMembers().size() : 0;
        if (currentParticipants >= room.getMaximumNumberOfPeople()) {
            throw new ValidationException("Room capacity exceeded");
        }
    }

    private void ensureParticipant(Room room, Integer userId) {
        if (!isUserParticipant(room, userId)) {
            throw new ResourceNotFoundException("User " + userId + " is not in room " + room.getId());
        }
    }

    private void validateRoomId(Integer roomId) {
        if (roomId == null) {
            throw new ValidationException("Room id is required");
        }
    }

    private Integer requireUserId(Integer userId) {
        if (userId == null) {
            throw new ValidationException("User id is required");
        }
        return userId;
    }

    private Room getExistingRoom(Integer roomId) {
        Room room = roomRepository.getRoomById(roomId);
        if (room == null) {
            throw new ResourceNotFoundException("Room not found: " + roomId);
        }
        return room;
    }

    private User getExistingUser(Integer userId) {
        User user = userRepository.getUserById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        return user;
    }

    private RoomParticipantResponse mapParticipant(User user, Role role) {
        return new RoomParticipantResponse(
            user.getId(),
            user.getUserName(),
            user.getDataOfBirth(),
            user.getCity(),
            user.getCountry(),
            user.getAvatarId(),
            user.getDescription(),
            role
        );
    }

    private RoomDetailedResponse mapRoomToDetailedResponse(Room room, Integer currentUserId,
                                                           boolean hasProtectedAccess) {
        RoomSummaryResponse summary = mapRoomToSummaryResponse(room, currentUserId);
        RoomDetailedResponse detailed = new RoomDetailedResponse();
        copySummary(summary, detailed);
        detailed.setHasProtectedAccess(hasProtectedAccess);
        if (hasProtectedAccess) {
            detailed.setChatLink(room.getChatLink());
            BulletinBoard board = bulletinBoardRepository.getBulletinBoard(room.getId());
            detailed.setBulletinBoard(board != null ? mapBulletinBoardToResponse(board) : null);
        }
        return detailed;
    }

    private void copySummary(RoomSummaryResponse source, RoomDetailedResponse target) {
        target.setId(source.getId());
        target.setActive(source.isActive());
        target.setIsPublic(source.getIsPublic());
        target.setCategory(source.getCategory());
        target.setName(source.getName());
        target.setDescription(source.getDescription());
        target.setDateOfStartEvent(source.getDateOfStartEvent());
        target.setDateOfEndEvent(source.getDateOfEndEvent());
        target.setAgeRating(source.getAgeRating());
        target.setFrequency(source.getFrequency());
        target.setParticipantCount(source.getParticipantCount());
        target.setMaximumParticipants(source.getMaximumParticipants());
        target.setCreator(source.getCreator());
        target.setIsCurrentUserParticipant(source.getIsCurrentUserParticipant());
        target.setImageIds(source.getImageIds());
    }

    private RoomSummaryResponse mapRoomToSummaryResponse(Room room, Integer currentUserId) {
        RoomSummaryResponse response = new RoomSummaryResponse();
        response.setId(room.getId());
        response.setActive(room.isActive());
        response.setIsPublic(room.isPublic());
        response.setCategory(room.getCategory());
        response.setName(room.getName());
        response.setDescription(room.getDescription());
        response.setDateOfStartEvent(room.getDateOfStartEvent());
        response.setDateOfEndEvent(room.getDateOfEndEvent());
        response.setAgeRating(room.getAgeRating());
        response.setFrequency(room.getFrequency());

        List<RoomMember> users = room.getMembers();
        int participantCount = users != null ? users.size() : 0;
        response.setParticipantCount(participantCount);
        response.setMaximumParticipants(room.getMaximumNumberOfPeople());

        User owner = findOwner(users);
        if (owner != null) {
            response.setCreator(mapUserToSummaryResponse(owner));
        }

        response.setIsCurrentUserParticipant(
            currentUserId != null && isUserParticipant(room, currentUserId));
        response.setImageIds(loadImageIds(room.getId()));
        return response;
    }

    private User findOwner(List<RoomMember> users) {
        if (users == null) {
            return null;
        }
        for (RoomMember member : users) {
            if (member.getRole().getPermissions().equals(Role.OWNER)) {
                return member.getUser();
            }
        }
        return null;
    }

    private List<Integer> loadImageIds(Integer roomId) {
        try {
            List<Picture> pictures = pictureRepository.getRoomPictures(roomId);
            if (pictures == null) {
                return Collections.emptyList();
            }
            return pictures.stream()
                .map(Picture::getPhotoId)
                .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private BulletinBoardResponse mapBulletinBoardToResponse(BulletinBoard board) {
        return new BulletinBoardResponse(
            board.getId(),
            board.getContent(),
            mapUserToSummaryResponse(board.getAuthor()),
            board.getUpdatedAt()
        );
    }

    private UserSummaryResponse mapUserToSummaryResponse(User user) {
        if (user == null) {
            return null;
        }
        UserSummaryResponse response = new UserSummaryResponse();
        response.setId(user.getId());
        response.setUserName(user.getUserName());
        response.setDateOfBirth(user.getDataOfBirth());
        response.setCity(user.getCity());
        response.setCountry(user.getCountry());
        response.setDescription(user.getDescription());
        response.setAvatarId(user.getAvatarId());
        return response;
    }

    private boolean isUserParticipant(Room room, Integer userId) {
        List<RoomMember> members = room.getMembers();
        for (RoomMember member : members) {
            if (member.getUser().getId().equals(userId)) {
                return true;
            }
        }
        return false;
    }
}
