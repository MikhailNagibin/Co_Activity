package com.coactivity.controller.dto.response;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationSettingsResponse {

  private Boolean membershipAccepted;
  private Boolean membershipRejected;
  private Boolean activityClosed;
  private Boolean newJoinRequest;
  private Instant updatedAt;
}