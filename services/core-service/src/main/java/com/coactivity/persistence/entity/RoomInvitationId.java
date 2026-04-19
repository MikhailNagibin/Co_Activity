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
public class RoomInvitationId implements Serializable {

  @Column(name = "room_id")
  private Integer roomId;

  @Column(name = "invited_user_id")
  private Integer invitedUserId;

  public RoomInvitationId(Integer roomId, Integer invitedUserId) {
    this.roomId = roomId;
    this.invitedUserId = invitedUserId;
  }
}
