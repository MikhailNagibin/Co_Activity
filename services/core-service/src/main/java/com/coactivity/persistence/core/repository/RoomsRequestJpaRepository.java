package com.coactivity.persistence.core.repository;

import com.coactivity.persistence.core.entity.RoomsRequestEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomsRequestJpaRepository extends JpaRepository<RoomsRequestEntity, Integer> {

  @EntityGraph(attributePaths = {"user", "room", "room.category", "status"})
  List<RoomsRequestEntity> findAllByRoom_IdOrderByCreatedAtDesc(Integer roomId);

  @EntityGraph(attributePaths = {"user", "room", "room.category", "status"})
  List<RoomsRequestEntity> findAllByUser_IdOrderByCreatedAtDesc(Integer userId);

  @EntityGraph(attributePaths = {"user", "room", "room.category", "status"})
  Optional<RoomsRequestEntity> findById(Integer id);

  @EntityGraph(attributePaths = {"user", "room", "room.category", "status"})
  Optional<RoomsRequestEntity> findByUser_IdAndRoom_Id(Integer userId, Integer roomId);

  void deleteAllByRoom_Id(Integer roomId);

  void deleteAllByUser_Id(Integer userId);
}
