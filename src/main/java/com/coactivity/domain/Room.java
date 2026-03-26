package com.coactivity.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Room {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "is_active")
  private boolean isActive;

  @Column(name = "is_public")
  private boolean isPublic;

  @Column(name = "chat_link")
  private String chatLink;

  @ManyToOne
  @JoinColumn(name = "category_id")
  private Category category;

  @Column(name = "name")
  private String name;

  @Column(name = "description", length = 1000)
  private String description;

  @Column(name = "date_of_start_event")
  private Instant dateOfStartEvent;

  @Column(name = "date_of_end_event")
  private Instant dateOfEndEvent;

  @Column(name = "age_rating")
  private int ageRating;

  @Column(name = "frequency")
  private Instant frequency;

  @Column(name = "maximum_number_of_people")
  private int maximumNumberOfPeople;

  @OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
  private List<RoomMember> members;

  @OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
  private List<Ban> bans;
}
