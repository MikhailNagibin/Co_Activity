package com.coactivity.repository.impl;

import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.domain.*;
import com.coactivity.repository.RoomRepository;
import com.coactivity.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@Transactional
public class RoomRepositoryImpl implements RoomRepository {

  @PersistenceContext
  private EntityManager entityManager;

  private final CategoryRepository categoryRepository;
  private final RoleRepository roleRepository;
  private final UserRepository userRepository;

  public RoomRepositoryImpl(CategoryRepository categoryRepository,
                            RoleRepository roleRepository,
                            UserRepository userRepository) {
    this.categoryRepository = categoryRepository;
    this.roleRepository = roleRepository;
    this.userRepository = userRepository;
  }

  @Override
  public Room createRoom(Integer ownerId, RoomCreationRequest request) {
    Category category = categoryRepository.findByName(request.getCategory().toUpperCase())
        .orElseThrow(() -> new IllegalArgumentException("Category not found: " + request.getCategory()));

    Room room = new Room();
    room.setActive(true);
    room.setPublic(request.getIsPublic());
    room.setChatLink(request.getChatLink());
    room.setCategory(category);
    room.setName(request.getName());
    room.setDescription(request.getDescription());
    room.setDateOfStartEvent(request.getDateOfStartEvent());
    room.setDateOfEndEvent(request.getDateOfEndEvent());
    room.setAgeRating(request.getAgeRating());
    room.setFrequency(request.getFrequency());
    room.setMaximumNumberOfPeople(request.getMaximumNumberOfPeople());

    entityManager.persist(room);

    addUserToRoom(room.getId(), ownerId, Role.OWNER);

    return getRoomById(room.getId());
  }

  @Override
  public Room getRoomById(Integer roomId) {
    Room room = entityManager.find(Room.class, roomId);
    if (room != null) {
      room.setMembers(getRoomMembers(roomId));
      room.setBans(getBannedUsers(roomId));
    }
    return room;
  }

  @Override
  public void addUserToRoom(Integer roomId, Integer userId, String roleName) {
    Role role = roleRepository.findByName(roleName)
        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
    addUserToRoom(roomId, userId, role);
  }

  public void addUserToRoom(Integer roomId, Integer userId, Role role) {
    if (isUserBannedInRoom(roomId, userId)) {
      throw new IllegalStateException("User is banned from this room");
    }

    if (isUserInMembers(roomId, userId)) {
      throw new IllegalStateException("User is already a member of this room");
    }

    Room room = getRoomById(roomId);
    if (room == null) {
      throw new IllegalArgumentException("Room with id " + roomId + " does not exist");
    }

    User user = userRepository.getUserById(userId);
    if (user == null) {
      throw new IllegalArgumentException("User with id " + userId + " does not exist");
    }

    RoomMember member = new RoomMember();
    member.setRoom(room);
    member.setUser(user);
    member.setRole(role);

    entityManager.persist(member);
  }

  @Override
  public void removeUserFromRoom(Integer roomId, Integer userId) {
    entityManager.createQuery(
            "DELETE FROM RoomMember rm WHERE rm.room.id = :roomId AND rm.user.id = :userId")
        .setParameter("roomId", roomId)
        .setParameter("userId", userId)
        .executeUpdate();
  }

  public void addUserBan(Integer roomId, Integer userId) {
    Room room = getRoomById(roomId);
    User user = userRepository.getUserById(userId);

    if (room == null || user == null) {
      throw new IllegalArgumentException("Room or user not found");
    }

    Ban ban = new Ban();
    ban.setRoom(room);
    ban.setUser(user);

    entityManager.persist(ban);
  }

  @Override
  public void deleteRoom(Integer roomId) {
    deleteAllWithRooms(roomId);

    Room room = entityManager.find(Room.class, roomId);
    if (room != null) {
      entityManager.remove(room);
    }
  }

  private void deleteAllWithRooms(Integer roomId) {
    entityManager.createQuery("DELETE FROM BulletinBoard b WHERE b.room.id = :roomId")
        .setParameter("roomId", roomId)
        .executeUpdate();
    entityManager.createQuery("DELETE FROM Ban b WHERE b.room.id = :roomId")
        .setParameter("roomId", roomId)
        .executeUpdate();
    entityManager.createQuery("DELETE FROM RoomsRequest rr WHERE rr.room.id = :roomId")
        .setParameter("roomId", roomId)
        .executeUpdate();
    entityManager.createQuery("DELETE FROM RoomMember rm WHERE rm.room.id = :roomId")
        .setParameter("roomId", roomId)
        .executeUpdate();
    entityManager.createQuery("DELETE FROM Picture p WHERE p.room.id = :roomId")
        .setParameter("roomId", roomId)
        .executeUpdate();
  }

  public List<RoomMember> getRoomMembers(Integer roomId) {
    return entityManager.createQuery(
            "SELECT rm FROM RoomMember rm WHERE rm.room.id = :roomId",
            RoomMember.class)
        .setParameter("roomId", roomId)
        .getResultList();
  }

  public Map<User, Role> getUsersInRoom(Integer roomId) {
    List<RoomMember> members = getRoomMembers(roomId);
    Map<User, Role> result = new HashMap<>();
    for (RoomMember member : members) {
      result.put(member.getUser(), member.getRole());
    }
    return result;
  }

  private List<Ban> getBannedUsers(Integer roomId) {
    return entityManager.createQuery(
            "SELECT b FROM Ban b WHERE b.room.id = :roomId",
            Ban.class)
        .setParameter("roomId", roomId)
        .getResultList();
  }

  private boolean isUserBannedInRoom(Integer roomId, Integer userId) {
    Long count = entityManager.createQuery(
            "SELECT COUNT(b) FROM Ban b WHERE b.room.id = :roomId AND b.user.id = :userId",
            Long.class)
        .setParameter("roomId", roomId)
        .setParameter("userId", userId)
        .getSingleResult();
    return count > 0;
  }

  @Override
  public boolean isUserInMembers(Integer roomId, Integer userId) {
    Long count = entityManager.createQuery(
            "SELECT COUNT(rm) FROM RoomMember rm WHERE rm.room.id = :roomId AND rm.user.id = :userId",
            Long.class)
        .setParameter("roomId", roomId)
        .setParameter("userId", userId)
        .getSingleResult();
    return count > 0;
  }

  public boolean isUserOwnerOfRoom(Integer userId, Integer roomId) {
    Role ownerRole = roleRepository.findByName(Role.OWNER)
        .orElseThrow(() -> new RuntimeException("Owner role not found"));

    Long count = entityManager.createQuery(
            "SELECT COUNT(rm) FROM RoomMember rm " +
                "WHERE rm.room.id = :roomId AND rm.user.id = :userId AND rm.role = :ownerRole",
            Long.class)
        .setParameter("roomId", roomId)
        .setParameter("userId", userId)
        .setParameter("ownerRole", ownerRole)
        .getSingleResult();
    return count > 0;
  }

  public void setRoleByUserIdAndRoomId(Integer userId, Integer roomId, String roleName) {
    Role role = roleRepository.findByName(roleName)
        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
    setRoleByUserIdAndRoomId(userId, roomId, role);
  }

  public void setRoleByUserIdAndRoomId(Integer userId, Integer roomId, Role role) {
    entityManager.createQuery(
            "UPDATE RoomMember rm SET rm.role = :role " +
                "WHERE rm.room.id = :roomId AND rm.user.id = :userId")
        .setParameter("role", role)
        .setParameter("roomId", roomId)
        .setParameter("userId", userId)
        .executeUpdate();
  }

  public Role getUserRoleByRoomId(Integer roomId, Integer userId) {
    try {
      return entityManager.createQuery(
              "SELECT rm.role FROM RoomMember rm WHERE rm.room.id = :roomId AND rm.user.id = :userId",
              Role.class)
          .setParameter("roomId", roomId)
          .setParameter("userId", userId)
          .getSingleResult();
    } catch (NoResultException e) {
      throw new RuntimeException("User not found in room members", e);
    }
  }

  @Override
  public List<Room> getAllRooms() {
    List<Room> rooms = entityManager.createQuery("SELECT r FROM Room r", Room.class)
        .getResultList();

    rooms.forEach(room -> {
      room.setMembers(getRoomMembers(room.getId()));
      room.setBans(getBannedUsers(room.getId()));
    });

    return rooms;
  }

  public int getCategoryIdByName(String categoryName) {
    return categoryRepository.findByName(categoryName.toUpperCase())
        .map(Category::getId)
        .orElse(-1);
  }
}