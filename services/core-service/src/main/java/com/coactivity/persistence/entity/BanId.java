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
public class BanId implements Serializable {

  @Column(name = "user_id")
  private Integer userId;

  @Column(name = "room_id")
  private Integer roomId;

  public BanId(Integer userId, Integer roomId) {
    this.userId = userId;
    this.roomId = roomId;
  }
}
