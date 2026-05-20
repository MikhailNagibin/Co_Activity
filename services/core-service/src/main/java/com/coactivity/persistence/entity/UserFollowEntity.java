package com.coactivity.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_follows")
@Getter
@Setter
@NoArgsConstructor
public class UserFollowEntity {

  @EmbeddedId
  private UserFollowId id;

  @MapsId("followerId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "follower_id", nullable = false)
  private UserEntity follower;

  @MapsId("followedId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "followed_id", nullable = false)
  private UserEntity followed;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
