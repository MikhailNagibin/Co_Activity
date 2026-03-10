package com.coactivity.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "name", nullable = false, unique = true)
  private String name;

  @Column(name = "description")
  private String description;

  // Для обратной совместимости
  public static Notification getByIndex(int index) {
    throw new UnsupportedOperationException("Use NotificationRepository instead");
  }

  @Override
  public String toString() {
    return name;
  }

  // Предопределенные константы
  public static final String MEMBERSHIP_ACCEPTED = "membershipAccepted";
  public static final String MEMBERSHIP_REJECTED = "membershipRejected";
  public static final String ACTIVITY_CLOSED = "activityClosed";
  public static final String NEW_JOIN_REQUEST = "newJoinRequest";
}
