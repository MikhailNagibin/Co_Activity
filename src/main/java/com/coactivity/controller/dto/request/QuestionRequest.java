package com.coactivity.controller.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for submitting a new question about a specific category.
 * <p>
 * Questions are publicly visible and can be answered by any authorized user.
 * Each question is associated with a category to provide context and organization.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionRequest {

  /**
   * The category identifier this question relates to.
   * <p>
   * Determines which category forum the question appears in and helps
   * route the question to users interested in that category.
   * </p>
   */
  private Integer categoryId;

  /**
   * The question text being asked.
   * <p>
   * Should be clear, specific, and relevant to the chosen category.
   * Maximum length of 2000 characters to ensure readability.
   * </p>
   */
  private String question;
}