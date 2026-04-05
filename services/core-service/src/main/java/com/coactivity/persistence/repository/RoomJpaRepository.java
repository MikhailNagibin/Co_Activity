package com.coactivity.persistence.repository;

import com.coactivity.persistence.entity.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomJpaRepository extends JpaRepository<RoomEntity, Integer> {
}
