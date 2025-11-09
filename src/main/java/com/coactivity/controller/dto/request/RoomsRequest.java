package com.coactivity.controller.dto.request;

import com.coactivity.domain.entities.Room;
import com.coactivity.domain.entities.User;
import com.coactivity.domain.enums.RequestStatus;
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
