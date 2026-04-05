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
public class RoomMemberId implements Serializable {

  @Column(name = "room_id")
  private Integer roomId;

  @Column(name = "user_id")
  private Integer userId;

  public RoomMemberId(Integer roomId, Integer userId) {
    this.roomId = roomId;
    this.userId = userId;
  }
}
