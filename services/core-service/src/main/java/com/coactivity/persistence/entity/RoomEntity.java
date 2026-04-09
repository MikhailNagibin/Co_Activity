package com.coactivity.persistence.entity;

import com.coactivity.domain.RoomStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
public class RoomEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private RoomStatus status;

  @Column(name = "is_public", nullable = false)
  private boolean publicRoom;

  @Column(name = "chat_link")
  private String chatLink;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  private CategoryEntity category;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "start_date")
  private Instant dateOfStartEvent;

  @Column(name = "end_date")
  private Instant dateOfEndEvent;

  @Column(name = "age_rating", nullable = false)
  private int ageRating;

  @Column(name = "frequency")
  private Instant frequency;

  @Column(name = "maximum_number_of_people", nullable = false)
  private int maximumNumberOfPeople;

  public boolean isActive() {
    return status == RoomStatus.ACTIVE;
  }
}
