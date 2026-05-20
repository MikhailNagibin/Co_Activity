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
@Table(name = "room_invitations")
@Getter
@Setter
@NoArgsConstructor
public class RoomInvitationEntity {

  @EmbeddedId
  private RoomInvitationId id;

  @MapsId("roomId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "room_id", nullable = false)
  private RoomEntity room;

  @MapsId("invitedUserId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "invited_user_id", nullable = false)
  private UserEntity invitedUser;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "invited_by_user_id", nullable = false)
  private UserEntity invitedByUser;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;
}
