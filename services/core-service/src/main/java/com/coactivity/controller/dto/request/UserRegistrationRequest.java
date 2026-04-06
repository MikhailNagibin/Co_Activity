package com.coactivity.controller.dto.request;

import java.time.Instant;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationRequest {

  @NotBlank
  @Email
  @Size(max = 255)
  private String email;

  @NotBlank
  @Size(min = 2, max = 20)
  private String userName;

  @NotBlank
  @Size(min = 8, max = 128)
  private String password;

  @NotNull
  @Past
  private Instant dateOfBirth;

  @Size(max = 100)
  private String city;

  @Size(max = 100)
  private String country;

  @Size(max = 500)
  private String description;

  private Integer avatarId;
}
