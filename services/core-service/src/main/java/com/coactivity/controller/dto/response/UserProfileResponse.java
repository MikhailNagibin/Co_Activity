package com.coactivity.controller.dto.response;

import com.coactivity.domain.Notification;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

  private Integer id;

  private String email;

  private String username;

  private Instant dateOfBirth;

  private String city;

  private String country;

  private String description;

  private Integer avatarId;

  private List<Notification> notifications;
}
