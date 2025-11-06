package com.coactivity.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * Response payload representing an answer in a question discussion thread.
 * <p>
 * Contains answer information along with author details and threading context
 * to display hierarchical conversation structures.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerResponse {

  /**
   * Unique identifier for the answer.
   */
  private Integer id;

  /**
   * The question this answer responds to.
   */
  private Integer questionId;

  /**
   * Reference to the previous answer in the thread, if this is a nested reply.
   * <p>
   * When {@code null}, this is a direct answer to the main question.
   * When populated, this answer is a reply to the specified previous answer.
   * </p>
   */
  private Integer previousAnswerId;

  /**
   * The answer text content.
   */
  private String answer;

  /**
   * Author information of the user who provided this answer.
   */
  private UserProfileResponse author;

  /**
   * Timestamp when this answer was submitted.
   */
  private Instant createdAt;

  /**
   * List of nested replies to this answer, forming conversation threads.
   * <p>
   * Empty list indicates no replies, creating a flat structure when all
   * answers are direct responses to the main question.
   * </p>
   */
  private List<AnswerResponse> replies;
}