package com.coactivity.controller.dto.response;

import com.coactivity.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OwnershipTransferResponse {

  private Integer roomId;

  private Integer previousOwnerId;

  private Integer newOwnerId;

  private Role previousOwnerNewRole;

  private Role newOwnerRole;
}
