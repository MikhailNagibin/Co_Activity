package com.coactivity.controller.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingsResponse {

  private Boolean membershipAccepted;

  private Boolean membershipRejected;

  private Boolean activityClosed;

  private Boolean newJoinRequest;

  private Instant updatedAt;
}
