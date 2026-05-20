package com.coactivity.persistence.repository;

import com.coactivity.persistence.entity.RoomMemberEntity;
import com.coactivity.persistence.entity.RoomMemberId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomMemberJpaRepository extends JpaRepository<RoomMemberEntity, RoomMemberId> {

  @EntityGraph(attributePaths = {"user", "role"})
  List<RoomMemberEntity> findAllByRoom_Id(Integer roomId);

  @EntityGraph(attributePaths = {"room", "room.category", "role"})
  List<RoomMemberEntity> findAllByUser_Id(Integer userId);

  @EntityGraph(attributePaths = {"role"})
  Optional<RoomMemberEntity> findByRoom_IdAndUser_Id(Integer roomId, Integer userId);

  boolean existsByRoom_IdAndUser_Id(Integer roomId, Integer userId);

  long countByRoom_Id(Integer roomId);

  void deleteByRoom_IdAndUser_Id(Integer roomId, Integer userId);

  void deleteAllByRoom_Id(Integer roomId);

  void deleteAllByUser_Id(Integer userId);
}
