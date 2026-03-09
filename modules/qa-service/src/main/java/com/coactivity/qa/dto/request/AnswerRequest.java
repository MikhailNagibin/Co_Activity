package com.coactivity.qa.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AnswerRequest(
    @NotNull @Positive Integer questionId,
    @Positive Integer previousAnswerId,
    @NotBlank @Size(max = 2000) String answer) {
}
