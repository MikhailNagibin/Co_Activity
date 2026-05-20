package com.coactivity.controller.dto.response;

import com.coactivity.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MembershipVerificationResponse {

  private Boolean isMember;

  private Role role;

  private UserSummaryResponse userInfo;

  private String roomName;
}
