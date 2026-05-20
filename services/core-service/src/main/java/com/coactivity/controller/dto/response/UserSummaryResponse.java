package com.coactivity.controller.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {

  private Integer id;

  private String userName;

  private Instant dateOfBirth;

  private String city;

  private String country;

  private String description;

  private Integer avatarId;

  private String avatarUrl;

  private Long followersCount;
}
