package com.coactivity.domain.entities;

import com.coactivity.domain.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RoomsRequest {

  private User user;
  private Room room;
  private RequestStatus status;
}
