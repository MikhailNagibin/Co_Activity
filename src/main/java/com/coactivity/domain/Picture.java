package com.coactivity.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;

@Entity
@Table(name = "pictures")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Picture {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "room_id")
  private Room room;

  @Column(name = "photo_id")
  private Integer photoId;
}
