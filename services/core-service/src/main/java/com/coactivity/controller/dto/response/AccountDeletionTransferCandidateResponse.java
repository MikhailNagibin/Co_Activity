package com.coactivity.controller.dto.response;

import com.coactivity.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountDeletionTransferCandidateResponse {

  private Integer userId;

  private String userName;

  private Role role;
}
