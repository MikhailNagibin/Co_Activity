package com.coactivity.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerRequest {

  @NotNull
  @Positive
  private Integer questionId;

  private Integer previousAnswerId;

  @NotBlank
  @Size(max = 2000)
  private String answer;
}