package com.coactivity.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for submitting an answer to an existing question.
 * <p>
 * Answers create threaded discussions where users can reply to previous answers,
 * forming conversation trees. The original question author receives email notification
 * when their question is answered.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerRequest {

  /**
   * The question identifier being answered.
   * <p>
   * Must reference an existing, active question in the system.
   * </p>
   */
  @NotNull
  @Positive
  private Integer questionId;

  /**
   * Optional reference to a previous answer in the conversation thread.
   * <p>
   * When provided, this answer becomes a reply to the specified previous answer,
   * creating nested discussion threads. When {@code null}, this is a direct
   * answer to the main question.
   * </p>
   */
  @Positive
  private Integer previousAnswerId;

  /**
   * The answer text providing response to the question or previous answer.
   * <p>
   * Should be helpful, relevant, and contribute meaningfully to the discussion.
   * Maximum length of 2000 characters to maintain discussion quality.
   * </p>
   */
  @NotBlank
  @Size(max = 2000)
  private String answer;
}