package com.coactivity.repository.impl;

import com.coactivity.domain.RequestStatus;
import com.coactivity.domain.RoomsRequest;
import com.coactivity.persistence.core.CoreDomainMapper;
import com.coactivity.persistence.core.CoreLookupMapper;
import com.coactivity.persistence.core.entity.RequestStatusEntity;
import com.coactivity.persistence.core.entity.RoomEntity;
import com.coactivity.persistence.core.entity.RoomsRequestEntity;
import com.coactivity.persistence.core.entity.UserEntity;
import com.coactivity.persistence.core.repository.RequestStatusLookupRepository;
import com.coactivity.persistence.core.repository.RoomJpaRepository;
import com.coactivity.persistence.core.repository.RoomsRequestJpaRepository;
import com.coactivity.persistence.core.repository.UserJpaRepository;
import com.coactivity.repository.RoomsRequestRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class RoomsRequestRepositoryImpl implements RoomsRequestRepository {

  private final RoomsRequestJpaRepository roomsRequestJpaRepository;
  private final UserJpaRepository userJpaRepository;
  private final RoomJpaRepository roomJpaRepository;
  private final RequestStatusLookupRepository requestStatusLookupRepository;

  public RoomsRequestRepositoryImpl(RoomsRequestJpaRepository roomsRequestJpaRepository,
      UserJpaRepository userJpaRepository,
      RoomJpaRepository roomJpaRepository,
      RequestStatusLookupRepository requestStatusLookupRepository) {
    this.roomsRequestJpaRepository = roomsRequestJpaRepository;
    this.userJpaRepository = userJpaRepository;
    this.roomJpaRepository = roomJpaRepository;
    this.requestStatusLookupRepository = requestStatusLookupRepository;
  }

  @Override
  public RoomsRequest createRequest(int userId, int roomId, RequestStatus status) {
    UserEntity userEntity = userJpaRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    RoomEntity roomEntity = roomJpaRepository.findById(roomId)
        .orElseThrow(() -> new RuntimeException("Room not found: " + roomId));

    RoomsRequestEntity entity = new RoomsRequestEntity();
    entity.setUser(userEntity);
    entity.setRoom(roomEntity);
    entity.setStatus(findStatusEntity(status));
    entity.setCreatedAt(Instant.now());

    return toDomain(roomsRequestJpaRepository.saveAndFlush(entity));
  }

  @Override
  public RoomsRequest updateRequest(int requestId, RequestStatus status) {
    RoomsRequestEntity entity = roomsRequestJpaRepository.findById(requestId)
        .orElseThrow(() -> new RuntimeException("Request not found with id: " + requestId));
    entity.setStatus(findStatusEntity(status));
    return toDomain(entity);
  }

  @Override
  public void deleteRequest(int requestId) {
    if (!roomsRequestJpaRepository.existsById(requestId)) {
      throw new RuntimeException("Request not found with id: " + requestId);
    }
    roomsRequestJpaRepository.deleteById(requestId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<RoomsRequest> getRoomRequests(int roomId) {
    return roomsRequestJpaRepository.findAllByRoom_IdOrderByCreatedAtDesc(roomId).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<RoomsRequest> getRequestsByUser(int userId) {
    return roomsRequestJpaRepository.findAllByUser_IdOrderByCreatedAtDesc(userId).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public RoomsRequest getRequestById(int requestId) {
    return roomsRequestJpaRepository.findById(requestId)
        .map(this::toDomain)
        .orElse(null);
  }

  @Override
  @Transactional(readOnly = true)
  public RoomsRequest getRequestByUserAndRoom(int userId, int roomId) {
    return roomsRequestJpaRepository.findByUser_IdAndRoom_Id(userId, roomId)
        .map(this::toDomain)
        .orElse(null);
  }

  private RequestStatusEntity findStatusEntity(RequestStatus status) {
    return requestStatusLookupRepository.findByStatusInfoIgnoreCase(
            CoreLookupMapper.toDbRequestStatus(status))
        .orElseThrow(() -> new RuntimeException("Status not found: " + status));
  }

  private RoomsRequest toDomain(RoomsRequestEntity entity) {
    return CoreDomainMapper.toRoomsRequest(
        entity,
        CoreDomainMapper.toUserSummary(entity.getUser()),
        CoreDomainMapper.toRoom(entity.getRoom()));
  }
}
