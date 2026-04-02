package com.coactivity.persistence.core.repository;

import com.coactivity.persistence.core.entity.PictureEntity;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PictureJpaRepository extends JpaRepository<PictureEntity, Integer> {

  @EntityGraph(attributePaths = {"room", "room.category"})
  List<PictureEntity> findAllByRoom_Id(Integer roomId);

  void deleteAllByRoom_Id(Integer roomId);
}
