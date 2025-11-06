package com.coactivity.dto.request;

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
  private Integer questionId;

  /**
   * Optional reference to a previous answer in the conversation thread.
   * <p>
   * When provided, this answer becomes a reply to the specified previous answer,
   * creating nested discussion threads. When {@code null}, this is a direct
   * answer to the main question.
   * </p>
   */
  private Integer previousAnswerId;

  /**
   * The answer text providing response to the question or previous answer.
   * <p>
   * Should be helpful, relevant, and contribute meaningfully to the discussion.
   * Maximum length of 2000 characters to maintain discussion quality.
   * </p>
   */
  private String answer;
}