package com.coactivity.controller.dto.response;

import com.coactivity.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleAssignmentResponse {

  private Integer userId;

  private Integer roomId;

  private Role newRole;

  private Role previousRole;

  private Integer assignedBy;
}
