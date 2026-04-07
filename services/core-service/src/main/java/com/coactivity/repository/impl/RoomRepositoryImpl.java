package com.coactivity.repository.impl;

import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.domain.Role;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;
import com.coactivity.persistence.CoreDomainMapper;
import com.coactivity.persistence.CoreLookupMapper;
import com.coactivity.persistence.entity.BanEntity;
import com.coactivity.persistence.entity.BanId;
import com.coactivity.persistence.entity.CategoryEntity;
import com.coactivity.persistence.entity.RoleEntity;
import com.coactivity.persistence.entity.RoomEntity;
import com.coactivity.persistence.entity.RoomMemberEntity;
import com.coactivity.persistence.entity.RoomMemberId;
import com.coactivity.persistence.entity.UserEntity;
import com.coactivity.persistence.repository.BanJpaRepository;
import com.coactivity.persistence.repository.BulletinBoardJpaRepository;
import com.coactivity.persistence.repository.CategoryLookupRepository;
import com.coactivity.persistence.repository.RoleLookupRepository;
import com.coactivity.persistence.repository.RoomJpaRepository;
import com.coactivity.persistence.repository.RoomMemberJpaRepository;
import com.coactivity.persistence.repository.RoomsRequestJpaRepository;
import com.coactivity.persistence.repository.UserJpaRepository;
import com.coactivity.repository.RoomRepository;
import com.coactivity.service.exception.ValidationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class RoomRepositoryImpl implements RoomRepository {

  private final RoomJpaRepository roomJpaRepository;
  private final RoomMemberJpaRepository roomMemberJpaRepository;
  private final BanJpaRepository banJpaRepository;
  private final CategoryLookupRepository categoryLookupRepository;
  private final RoleLookupRepository roleLookupRepository;
  private final UserJpaRepository userJpaRepository;
  private final RoomsRequestJpaRepository roomsRequestJpaRepository;
  private final BulletinBoardJpaRepository bulletinBoardJpaRepository;

  public RoomRepositoryImpl(RoomJpaRepository roomJpaRepository,
      RoomMemberJpaRepository roomMemberJpaRepository,
      BanJpaRepository banJpaRepository,
      CategoryLookupRepository categoryLookupRepository,
      RoleLookupRepository roleLookupRepository,
      UserJpaRepository userJpaRepository,
      RoomsRequestJpaRepository roomsRequestJpaRepository,
      BulletinBoardJpaRepository bulletinBoardJpaRepository) {
    this.roomJpaRepository = roomJpaRepository;
    this.roomMemberJpaRepository = roomMemberJpaRepository;
    this.banJpaRepository = banJpaRepository;
    this.categoryLookupRepository = categoryLookupRepository;
    this.roleLookupRepository = roleLookupRepository;
    this.userJpaRepository = userJpaRepository;
    this.roomsRequestJpaRepository = roomsRequestJpaRepository;
    this.bulletinBoardJpaRepository = bulletinBoardJpaRepository;
  }

  @Override
  public Room createRoom(Integer ownerId, RoomCreationRequest request) {
    CategoryEntity categoryEntity = findCategoryEntity(request.getCategory());

    RoomEntity roomEntity = new RoomEntity();
    roomEntity.setActive(true);
    roomEntity.setPublicRoom(Boolean.TRUE.equals(request.getIsPublic()));
    roomEntity.setChatLink(request.getChatLink());
    roomEntity.setCategory(categoryEntity);
    roomEntity.setName(request.getName());
    roomEntity.setDescription(request.getDescription());
    roomEntity.setDateOfStartEvent(request.getDateOfStartEvent());
    roomEntity.setDateOfEndEvent(request.getDateOfEndEvent());
    roomEntity.setAgeRating(request.getAgeRating());
    roomEntity.setFrequency(request.getFrequency());
    roomEntity.setMaximumNumberOfPeople(
        request.getMaximumNumberOfPeople() != null ? request.getMaximumNumberOfPeople() : 2);

    RoomEntity saved = roomJpaRepository.saveAndFlush(roomEntity);
    addUserToRoom(saved.getId(), ownerId, Role.OWNER);
    return CoreDomainMapper.toRoom(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public Room getRoomById(Integer roomId) {
    return roomJpaRepository.findById(roomId)
        .map(CoreDomainMapper::toRoom)
        .orElse(null);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Room> getAllRooms() {
    return roomJpaRepository.findAll().stream()
        .map(CoreDomainMapper::toRoom)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Room> getRoomsOwnedByUser(Integer userId) {
    return roomMemberJpaRepository.findAllByUser_Id(userId).stream()
        .filter(membership -> CoreLookupMapper.toRole(membership.getRole().getRoleName()) == Role.OWNER)
        .map(RoomMemberEntity::getRoom)
        .map(CoreDomainMapper::toRoom)
        .toList();
  }

  @Override
  public void addUserToRoom(Integer roomId, Integer userId, Role role) {
    if (isUserBannedInRoom(roomId, userId)) {
      throw new RuntimeException("User is banned from room");
    }
    if (isUserInMembers(roomId, userId)) {
      throw new RuntimeException("User is already a member of room");
    }

    RoomEntity roomEntity = getExistingRoomEntity(roomId);
    UserEntity userEntity = userJpaRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    RoleEntity roleEntity = findRoleEntity(role);

    RoomMemberEntity membership = new RoomMemberEntity();
    membership.setId(new RoomMemberId(roomId, userId));
    membership.setRoom(roomEntity);
    membership.setUser(userEntity);
    membership.setRole(roleEntity);
    roomMemberJpaRepository.save(membership);
  }

  @Override
  public void deleteRoom(Integer roomId) {
    RoomEntity roomEntity = getExistingRoomEntity(roomId);
    bulletinBoardJpaRepository.deleteByRoom_Id(roomId);
    banJpaRepository.deleteAllByRoom_Id(roomId);
    roomsRequestJpaRepository.deleteAllByRoom_Id(roomId);
    roomMemberJpaRepository.deleteAllByRoom_Id(roomId);
    roomJpaRepository.delete(roomEntity);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isUserInMembers(Integer roomId, Integer userId) {
    return roomMemberJpaRepository.existsByRoom_IdAndUser_Id(roomId, userId);
  }

  @Override
  public void removeUserFromRoom(Integer roomId, Integer userId) {
    if (!roomMemberJpaRepository.existsByRoom_IdAndUser_Id(roomId, userId)) {
      throw new RuntimeException("User " + userId + " is not a member of room " + roomId);
    }
    roomMemberJpaRepository.deleteByRoom_IdAndUser_Id(roomId, userId);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isUserBannedInRoom(Integer roomId, Integer userId) {
    return banJpaRepository.existsByRoom_IdAndUser_Id(roomId, userId);
  }

  @Override
  @Transactional(readOnly = true)
  public int getRoomParticipantCount(Integer roomId) {
    return (int) roomMemberJpaRepository.countByRoom_Id(roomId);
  }

  @Override
  @Transactional(readOnly = true)
  public Map<User, Role> getUsersInRoom(Integer roomId) {
    Map<User, Role> usersInRoom = new LinkedHashMap<>();
    for (RoomMemberEntity membership : roomMemberJpaRepository.findAllByRoom_Id(roomId)) {
      usersInRoom.put(
          CoreDomainMapper.toUserSummary(membership.getUser()),
          CoreLookupMapper.toRole(membership.getRole().getRoleName()));
    }
    return usersInRoom;
  }

  @Override
  public void setRoleByUserIdAndRoomId(Integer userId, Integer roomId, Role role) {
    RoomMemberEntity membership = roomMemberJpaRepository.findByRoom_IdAndUser_Id(roomId, userId)
        .orElseThrow(() -> new RuntimeException(
            "Failed to update role. User " + userId + " not found in room " + roomId));
    membership.setRole(findRoleEntity(role));
  }

  @Override
  @Transactional(readOnly = true)
  public Role getUserRoleByRoomId(Integer roomId, Integer userId) {
    RoomMemberEntity membership = roomMemberJpaRepository.findByRoom_IdAndUser_Id(roomId, userId)
        .orElseThrow(() -> new RuntimeException(
            "User " + userId + " not found in room " + roomId));
    return CoreLookupMapper.toRole(membership.getRole().getRoleName());
  }

  @Override
  public void addUserBan(Integer roomId, Integer userId) {
    if (banJpaRepository.existsByRoom_IdAndUser_Id(roomId, userId)) {
      return;
    }
    RoomEntity roomEntity = getExistingRoomEntity(roomId);
    UserEntity userEntity = userJpaRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found: " + userId));

    BanEntity banEntity = new BanEntity();
    banEntity.setId(new BanId(userId, roomId));
    banEntity.setUser(userEntity);
    banEntity.setRoom(roomEntity);
    banJpaRepository.save(banEntity);
  }

  @Transactional(readOnly = true)
  public int getCategoryIdByName(String categoryName) {
    return findCategoryEntity(categoryName).getId();
  }

  private RoomEntity getExistingRoomEntity(Integer roomId) {
    return roomJpaRepository.findById(roomId)
        .orElseThrow(() -> new RuntimeException("Room not found: " + roomId));
  }

  private CategoryEntity findCategoryEntity(String categoryName) {
    String dbName = CoreLookupMapper.toDbCategoryName(categoryName);
    return categoryLookupRepository.findByNameIgnoreCase(dbName)
        .orElseThrow(() -> new ValidationException("Category not found: " + categoryName));
  }

  private RoleEntity findRoleEntity(Role role) {
    return roleLookupRepository.findByRoleNameIgnoreCase(CoreLookupMapper.toDbRoleName(role))
        .orElseThrow(() -> new RuntimeException("Role not found: " + role));
  }
}
