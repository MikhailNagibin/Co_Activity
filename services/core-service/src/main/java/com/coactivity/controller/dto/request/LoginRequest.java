package com.coactivity.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос логина (email + password).")
public class LoginRequest {

  @NotBlank
  @Email
  @Size(max = 255)
  @Schema(example = "user@example.com")
  private String email;

  @NotBlank
  @Size(min = 8, max = 128)
  @Schema(example = "P@ssw0rd123")
  private String password;
}
