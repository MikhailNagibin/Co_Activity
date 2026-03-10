package com.coactivity.domain;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "bulletin_boards")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BulletinBoard {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne
  @JoinColumn(name = "room_id")
  private Room room;

  @Column(name = "content", length = 2000)
  private String content;

  @ManyToOne
  @JoinColumn(name = "author_id")
  private User author;

  @Column(name = "updated_at")
  private Instant updatedAt;
}
