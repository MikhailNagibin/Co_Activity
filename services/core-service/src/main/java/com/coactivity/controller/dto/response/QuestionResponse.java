package com.coactivity.controller.dto.response;

import com.coactivity.domain.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response payload representing a question in the Q&A system.
 * <p>
 * Contains complete question information including author details and metadata for display in
 * question lists and individual question pages.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {

  /**
   * Unique identifier for the question.
   */
  private Integer id;

  /**
   * The category this question belongs to.
   */
  private Category category;

  /**
   * The actual question text.
   */
  private String question;

  /**
   * Author information of the user who asked the question.
   */
  private UserSummaryResponse author;
}
