package com.coactivity.controller.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingsRequest {

  private Boolean membershipAccepted;

  private Boolean membershipRejected;

  private Boolean activityClosed;

  private Boolean newJoinRequest;

  private Boolean importantRoomUpdates;
}
