package com.coactivity.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Comprehensive response containing a question and its complete answer thread.
 * <p>
 * Used when displaying an individual question page with full discussion context.
 * The answer hierarchy is reconstructed to show threaded conversations properly.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionWithAnswersResponse {

  /**
   * The complete question details.
   */
  private QuestionResponse question;

  /**
   * Hierarchical list of answers and their replies.
   * <p>
   * Top-level answers (those with {@code previousAnswerId = null}) are listed
   * directly, with nested replies organized under their parent answers.
   * </p>
   */
  private List<AnswerResponse> answers;
}