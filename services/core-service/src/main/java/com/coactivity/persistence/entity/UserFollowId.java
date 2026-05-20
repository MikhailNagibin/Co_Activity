package com.coactivity.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class UserFollowId implements Serializable {

  @Column(name = "follower_id")
  private Integer followerId;

  @Column(name = "followed_id")
  private Integer followedId;

  public UserFollowId(Integer followerId, Integer followedId) {
    this.followerId = followerId;
    this.followedId = followedId;
  }
}
