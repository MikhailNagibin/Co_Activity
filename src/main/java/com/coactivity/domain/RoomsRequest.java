package com.coactivity.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
public class RoomsRequest {

  private User user;
  private Room room;
  private Instant createdAt;
  private RequestStatus status;
}
