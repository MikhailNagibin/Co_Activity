package com.coactivity.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "rooms_requests")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RoomsRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;

  @ManyToOne
  @JoinColumn(name = "room_id")
  private Room room;

  @Column(name = "created_at")
  private Instant createdAt;

  @ManyToOne
  @JoinColumn(name = "status_id")
  private RequestStatus status;
}
