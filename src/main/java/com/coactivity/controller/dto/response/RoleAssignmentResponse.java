package com.coactivity.controller.dto.response;

import com.coactivity.domain.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleAssignmentResponse {

  private Integer userId;
  private Integer roomId;
  private Role newRole;
  private Role previousRole;
  private Integer assignedBy;
}