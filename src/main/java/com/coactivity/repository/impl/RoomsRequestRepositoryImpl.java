package com.coactivity.repository.impl;

import com.coactivity.domain.RequestStatus;
import com.coactivity.domain.Room;
import com.coactivity.domain.RoomsRequest;
import com.coactivity.domain.User;
import com.coactivity.repository.impl.RequestStatusRepository;
import com.coactivity.repository.RoomsRequestRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
@Transactional
public class RoomsRequestRepositoryImpl implements RoomsRequestRepository {

  @PersistenceContext
  private EntityManager entityManager;

  private final UserRepositoryImpl userRepository;
  private final RoomRepositoryImpl roomRepository;
  private final RequestStatusRepository requestStatusRepository;

  public RoomsRequestRepositoryImpl(RoomRepositoryImpl roomRepository,
                                    UserRepositoryImpl userRepository,
                                    RequestStatusRepository requestStatusRepository) {
    this.roomRepository = roomRepository;
    this.userRepository = userRepository;
    this.requestStatusRepository = requestStatusRepository;
  }

  @Override
  public RoomsRequest createRequest(int userId, int roomId, RequestStatus status) {
    User user = userRepository.getUserById(userId);
    Room room = roomRepository.getRoomById(roomId);

    RoomsRequest request = new RoomsRequest();
    request.setUser(user);
    request.setRoom(room);
    request.setCreatedAt(Instant.now());
    request.setStatus(status);

    entityManager.persist(request);
    return request;
  }

  public RoomsRequest createRequest(int userId, int roomId, String statusName) {
    RequestStatus status = requestStatusRepository.findByName(statusName)
        .orElseThrow(() -> new IllegalArgumentException("Unknown status: " + statusName));
    return createRequest(userId, roomId, status);
  }

  @Override
  public RoomsRequest updateRequest(int requestId, RequestStatus status) {
    RoomsRequest request = entityManager.find(RoomsRequest.class, requestId);
    if (request == null) {
      throw new RuntimeException("Request not found with id: " + requestId);
    }

    request.setStatus(status);
    return entityManager.merge(request);
  }

  public RoomsRequest updateRequest(int requestId, String statusName) {
    RequestStatus status = requestStatusRepository.findByName(statusName)
        .orElseThrow(() -> new IllegalArgumentException("Unknown status: " + statusName));
    return updateRequest(requestId, status);
  }

  @Override
  public void deleteRequest(int requestId) {
    RoomsRequest request = entityManager.find(RoomsRequest.class, requestId);
    if (request != null) {
      entityManager.remove(request);
    } else {
      throw new RuntimeException("Request not found with id: " + requestId);
    }
  }

  @Override
  public List<RoomsRequest> getRoomRequests(int roomId) {
    return entityManager.createQuery(
            "SELECT rr FROM RoomsRequest rr WHERE rr.room.id = :roomId ORDER BY rr.createdAt DESC",
            RoomsRequest.class)
        .setParameter("roomId", roomId)
        .getResultList();
  }

  @Override
  public List<RoomsRequest> getRequestsByUser(int userId) {
    return entityManager.createQuery(
            "SELECT rr FROM RoomsRequest rr WHERE rr.user.id = :userId ORDER BY rr.createdAt DESC",
            RoomsRequest.class)
        .setParameter("userId", userId)
        .getResultList();
  }

  @Override
  public RoomsRequest getRequestById(int requestId) {
    return entityManager.find(RoomsRequest.class, requestId);
  }

  public RoomsRequest getRequestByUserAndRoom(int userId, int roomId) {
    try {
      return entityManager.createQuery(
              "SELECT rr FROM RoomsRequest rr WHERE rr.user.id = :userId AND rr.room.id = :roomId",
              RoomsRequest.class)
          .setParameter("userId", userId)
          .setParameter("roomId", roomId)
          .getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public List<RoomsRequest> getRoomRequestsByStatus(int roomId, String statusName) {
    RequestStatus status = requestStatusRepository.findByName(statusName)
        .orElseThrow(() -> new IllegalArgumentException("Unknown status: " + statusName));

    return entityManager.createQuery(
            "SELECT rr FROM RoomsRequest rr WHERE rr.room.id = :roomId AND rr.status = :status ORDER BY rr.createdAt DESC",
            RoomsRequest.class)
        .setParameter("roomId", roomId)
        .setParameter("status", status)
        .getResultList();
  }

  public long countRequestsByStatus(String statusName) {
    RequestStatus status = requestStatusRepository.findByName(statusName)
        .orElseThrow(() -> new IllegalArgumentException("Unknown status: " + statusName));

    return entityManager.createQuery(
            "SELECT COUNT(rr) FROM RoomsRequest rr WHERE rr.status = :status",
            Long.class)
        .setParameter("status", status)
        .getSingleResult();
  }
}
