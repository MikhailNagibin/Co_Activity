package com.coactivity.qa.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuestionRequest(
    @NotBlank String category,
    @NotBlank @Size(max = 2000) String question) {
}
