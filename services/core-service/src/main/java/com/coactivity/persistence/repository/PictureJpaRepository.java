package com.coactivity.persistence.repository;

import com.coactivity.persistence.entity.PictureEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PictureJpaRepository extends JpaRepository<PictureEntity, Integer> {

  @EntityGraph(attributePaths = {"room", "room.category"})
  List<PictureEntity> findAllByRoom_IdAndStorageKeyIsNotNullOrderBySortOrderAscIdAsc(Integer roomId);

  @EntityGraph(attributePaths = {"room", "room.category"})
  Optional<PictureEntity> findByIdAndRoom_IdAndStorageKeyIsNotNull(Integer id, Integer roomId);

  long countByRoom_IdAndStorageKeyIsNotNull(Integer roomId);

  void deleteAllByRoom_Id(Integer roomId);
}
