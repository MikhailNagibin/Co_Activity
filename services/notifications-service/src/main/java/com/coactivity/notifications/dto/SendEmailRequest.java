package com.coactivity.notifications.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendEmailRequest(
    @NotBlank @Email String to,
    @NotBlank @Size(max = 200) String subject,
    @NotBlank @Size(max = 10000) String body) {
}
