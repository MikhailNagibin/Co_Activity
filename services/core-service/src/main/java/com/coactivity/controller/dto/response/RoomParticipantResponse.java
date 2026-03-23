package com.coactivity.controller.dto.response;

import com.coactivity.domain.Role;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomParticipantResponse {

  private Integer id;

  private String name;

  private Instant dateOfBirth;

  private String city;

  private String country;

  private Integer avatarId;

  private String description;

  private Role role;
}