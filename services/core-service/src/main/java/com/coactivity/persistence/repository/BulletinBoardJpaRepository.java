package com.coactivity.persistence.repository;

import com.coactivity.persistence.entity.BulletinBoardEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BulletinBoardJpaRepository extends JpaRepository<BulletinBoardEntity, Integer> {

  @EntityGraph(attributePaths = {"author", "room", "room.category"})
  Optional<BulletinBoardEntity> findByRoom_Id(Integer roomId);

  boolean existsByRoom_Id(Integer roomId);

  void deleteByRoom_Id(Integer roomId);
}
