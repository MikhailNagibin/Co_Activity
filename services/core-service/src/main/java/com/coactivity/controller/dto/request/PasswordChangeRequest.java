package com.coactivity.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordChangeRequest {

  @NotBlank
  @Size(min = 8, max = 128)
  private String currentPassword;

  @NotBlank
  @Size(min = 8, max = 128)
  private String newPassword;
}
