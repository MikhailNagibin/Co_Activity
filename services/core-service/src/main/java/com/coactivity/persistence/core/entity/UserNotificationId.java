package com.coactivity.persistence.core.entity;

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
public class UserNotificationId implements Serializable {

  @Column(name = "user_id")
  private Integer userId;

  @Column(name = "notification_id")
  private Integer notificationId;

  public UserNotificationId(Integer userId, Integer notificationId) {
    this.userId = userId;
    this.notificationId = notificationId;
  }
}
