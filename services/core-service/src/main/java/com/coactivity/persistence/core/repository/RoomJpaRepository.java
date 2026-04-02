package com.coactivity.persistence.core.repository;

import com.coactivity.persistence.core.entity.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomJpaRepository extends JpaRepository<RoomEntity, Integer> {
}
