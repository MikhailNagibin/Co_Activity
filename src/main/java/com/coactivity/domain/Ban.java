package com.coactivity.domain;

import java.time.Duration;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Ban {
  private User user;
  private Room room;
  private Instant dateOfBan;
}
