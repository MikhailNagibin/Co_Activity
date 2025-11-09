package com.coactivity.controller.dto.response;

import com.coactivity.domain.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MembershipVerificationResponse {

  private Boolean isMember;
  private Role role;
  private String userName;
  private String roomName;
}
