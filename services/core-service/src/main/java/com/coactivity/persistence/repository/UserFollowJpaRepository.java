package com.coactivity.persistence.repository;

import com.coactivity.persistence.entity.UserFollowEntity;
import com.coactivity.persistence.entity.UserFollowId;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserFollowJpaRepository extends JpaRepository<UserFollowEntity, UserFollowId> {

  boolean existsByFollower_IdAndFollowed_Id(Integer followerId, Integer followedId);

  long deleteByFollower_IdAndFollowed_Id(Integer followerId, Integer followedId);

  long countByFollowed_Id(Integer followedId);

  @EntityGraph(attributePaths = {"followed", "followed.avatarFile"})
  List<UserFollowEntity> findAllByFollower_IdOrderByCreatedAtDescFollowed_IdDesc(Integer followerId);

  @EntityGraph(attributePaths = {"follower", "follower.avatarFile"})
  List<UserFollowEntity> findAllByFollowed_IdOrderByCreatedAtDescFollower_IdDesc(Integer followedId);

  @Query("""
      SELECT uf.follower.email
      FROM UserFollowEntity uf
      WHERE uf.followed.id = :followedId
        AND uf.follower.email IS NOT NULL
        AND trim(uf.follower.email) <> ''
      ORDER BY uf.createdAt DESC, uf.follower.id DESC
      """)
  List<String> findFollowerEmailsByFollowedId(@Param("followedId") Integer followedId);
}
