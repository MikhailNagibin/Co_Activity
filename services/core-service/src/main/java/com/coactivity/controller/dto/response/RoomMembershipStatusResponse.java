package com.coactivity.controller.dto.response;

import com.coactivity.domain.Role;
import com.coactivity.domain.RoomMembershipStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomMembershipStatusResponse {

  private Integer roomId;

  private Integer userId;

  private RoomMembershipStatus status;

  private Role role;

  private Integer pendingRequestId;

  private Boolean canJoin;
}
