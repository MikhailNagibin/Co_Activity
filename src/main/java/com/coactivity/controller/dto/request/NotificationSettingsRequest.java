package com.coactivity.controller.dto.request;

import lombok.Data;

@Data
public class NotificationSettingsRequest {

  private Boolean membershipAccepted;
  private Boolean membershipRejected;
  private Boolean activityClosed;
  private Boolean newJoinRequest;
}