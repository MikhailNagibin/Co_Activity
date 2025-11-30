package com.coactivity.controller;

import com.coactivity.controller.dto.request.AnswerRequest;
import com.coactivity.controller.dto.request.QuestionRequest;
import com.coactivity.controller.dto.response.AnswerResponse;
import com.coactivity.controller.dto.response.QuestionResponse;
import com.coactivity.controller.dto.response.QuestionWithAnswersResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;

/**
 * Core API controller interface for the CoActivity platform.
 * <p>
 * Defines the complete contract for user management, room operations, and activity participation.
 * All methods now return {@link ResponseEntity} values so controllers can express HTTP status,
 * headers, and DTO payloads explicitly. JWT-like bearer tokens are still used for stateless
 * authentication with 30-minute session persistence.
 *
 * @author CoActivity 13 Development Team
 * </p>
 */
public interface QAController {

  /**
   * Submits a new question about a specific category.
   * <p>
   * Creates a publicly visible question that any authorized user can answer. Questions are
   * organized by category to help users find relevant discussions. The question author's identity
   * is visible to all users.
   * </p>
   *
   * @param token   valid JWT token of the user asking the question
   * @param request question details including category and question text
   * @return {@link ResponseEntity} containing the created {@link QuestionResponse} with assigned
   * question ID, metadata, and {@code 201 Created} status
   * @example Ask about music equipment in the Music category QuestionRequest request = new
   * QuestionRequest(); request.setCategoryId(2); // Music category request.setQuestion("What's the
   * best beginner guitar for under $300?");
   * <p>
   * ResponseEntity<QuestionResponse> response = askQuestion(userToken, request);
   */
  ResponseEntity<QuestionResponse> askQuestion(String token, QuestionRequest request);

  /**
   * Submits an answer to an existing question.
   * <p>
   * Creates a new answer in the question's discussion thread. Answers can be direct responses to
   * the main question or nested replies to previous answers. The original question author receives
   * an email notification with the answer.
   * </p>
   *
   * @param token   valid JWT token of the user providing the answer
   * @param request answer details including the target question and answer text
   * @return {@link ResponseEntity} containing the created {@link AnswerResponse} with threading
   * information and author details
   * @example // Answer a question about beginner guitars AnswerRequest request = new
   * AnswerRequest(); request.setQuestionId(123); request.setAnswer("I recommend the Yamaha FG800
   * for beginners - great value at $250!");
   * <p>
   * ResponseEntity<AnswerResponse> response = answerQuestion(userToken, request);
   * @example // Reply to a specific previous answer AnswerRequest replyRequest = new
   * AnswerRequest(); replyRequest.setQuestionId(123); replyRequest.setPreviousAnswerId(456); //
   * Replying to answer #456 replyRequest.setAnswer("I agree, the Yamaha FG800 is excellent!");
   * <p>
   * ResponseEntity<AnswerResponse> replyResponse = answerQuestion(userToken, replyRequest);
   */
  ResponseEntity<AnswerResponse> answerQuestion(String token, AnswerRequest request);

  /**
   * Retrieves all questions in the Q&A system with optional category filtering.
   * <p>
   * <b>Access Control:</b> Public endpoint - no authentication required.
   * Implements read-only Q&A access for all users as specified in requirements. Questions are
   * sorted by most recent activity (most recently answered questions first).
   * </p>
   *
   * @param categoryId optional category filter to show only questions from a specific category, or
   *                   {@code null} to return questions from all categories
   * @return {@link ResponseEntity} containing list of {@link QuestionResponse} objects with basic
   * question information and answer counts
   */
  ResponseEntity<List<QuestionResponse>> getQuestions(Integer categoryId);

  /**
   * Retrieves a specific question and its complete answer hierarchy.
   * <p>
   * <b>Access Control:</b> Public endpoint - no authentication required.
   * Returns the full question details along with all answers organized in threaded conversation
   * format. Top-level answers are direct responses to the question, with nested replies forming
   * discussion trees under each parent answer.
   * </p>
   *
   * @param questionId unique identifier of the question to retrieve
   * @return {@link ResponseEntity} containing {@link QuestionWithAnswersResponse} with the question
   * and hierarchical answer structure
   */
  ResponseEntity<QuestionWithAnswersResponse> getQuestionWithAnswers(Integer questionId);
}
