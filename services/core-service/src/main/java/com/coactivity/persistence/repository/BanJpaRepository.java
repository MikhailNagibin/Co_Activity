package com.coactivity.persistence.repository;

import com.coactivity.persistence.entity.BanEntity;
import com.coactivity.persistence.entity.BanId;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BanJpaRepository extends JpaRepository<BanEntity, BanId> {

  boolean existsByRoom_IdAndUser_Id(Integer roomId, Integer userId);

  @EntityGraph(attributePaths = {"user"})
  List<BanEntity> findAllByRoom_Id(Integer roomId);

  void deleteByRoom_IdAndUser_Id(Integer roomId, Integer userId);

  void deleteAllByRoom_Id(Integer roomId);

  void deleteAllByUser_Id(Integer userId);
}
