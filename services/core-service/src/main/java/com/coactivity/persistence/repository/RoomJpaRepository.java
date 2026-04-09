package com.coactivity.persistence.repository;

import com.coactivity.persistence.entity.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;

import jakarta.persistence.LockModeType;

public interface RoomJpaRepository extends JpaRepository<RoomEntity, Integer> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<RoomEntity> findWithLockById(Integer id);
}
