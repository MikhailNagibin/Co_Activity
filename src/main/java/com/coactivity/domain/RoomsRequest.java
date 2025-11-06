package com.coactivity.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
public class RoomsRequest {

  private int userId;
  private int roomId;
  private Instant createdAt;
  private RequestStatus status;
}
