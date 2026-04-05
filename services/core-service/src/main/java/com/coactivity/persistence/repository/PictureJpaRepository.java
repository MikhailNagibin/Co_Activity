package com.coactivity.persistence.repository;

import com.coactivity.persistence.entity.PictureEntity;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PictureJpaRepository extends JpaRepository<PictureEntity, Integer> {

  @EntityGraph(attributePaths = {"room", "room.category"})
  List<PictureEntity> findAllByRoom_Id(Integer roomId);

  void deleteAllByRoom_Id(Integer roomId);
}
