package com.coactivity.controller.dto.response;

import com.coactivity.domain.RequestStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinRequestResponse {

  private Integer requestId;

  private Integer userId;

  private String username;

  private Integer roomId;

  private String roomName;

  private RequestStatus status;

  private Instant createdAt;
}