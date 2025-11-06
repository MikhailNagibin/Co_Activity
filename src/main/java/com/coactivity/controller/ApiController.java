package com.coactivity.controller;

import com.coactivity.domain.entities.RoomFilter;
import com.coactivity.domain.enums.RequestStatus;
import com.coactivity.domain.enums.RoomSort;
import com.coactivity.dto.request.AnswerRequest;
import com.coactivity.dto.request.LoginRequest;
import com.coactivity.dto.request.QuestionRequest;
import com.coactivity.dto.request.RoomCreationRequest;
import com.coactivity.dto.request.UserProfileUpdateRequest;
import com.coactivity.dto.request.UserRegistrationRequest;
import com.coactivity.dto.response.AnswerResponse;
import com.coactivity.dto.response.ApiResponse;
import com.coactivity.dto.response.JoinRequestResponse;
import com.coactivity.dto.response.LoginResponse;
import com.coactivity.dto.response.QuestionResponse;
import com.coactivity.dto.response.QuestionWithAnswersResponse;
import com.coactivity.dto.response.RegistrationResponse;
import com.coactivity.dto.response.RoomCreationResponse;
import com.coactivity.dto.response.RoomSummaryResponse;
import com.coactivity.dto.response.UserProfileResponse;
import java.util.List;

/**
 * Core API controller interface for the CoActivity platform.
 * <p>
 * Defines the complete contract for user management, room operations, and activity participation.
 * All methods return standardized {@link ApiResponse} wrappers containing either the requested data
 * or detailed error information. JWT tokens are used for stateless authentication with 30-minute
 * session persistence as specified in the functional requirements.
 * </p>
 *
 * <p><b>Authentication:</b> Methods requiring authentication expect a valid JWT token in the
 * {@code Authorization} header. Tokens are obtained via the {@link #verifyLogin(String, String)}
 * method and remain valid for 30 minutes from issuance.</p>
 *
 * <p><b>Error Handling:</b> All methods return {@link ApiResponse} instances where the
 * {@code success} field indicates operation outcome. Clients should always check this field before
 * processing the data payload.</p>
 *
 * @author CoActivity 13 Development Team
 * @version 1.0
 * @see ApiResponse
 * @see RegistrationResponse
 * @see LoginResponse
 * @see UserProfileResponse
 * @see RoomCreationResponse
 * @see RoomSummaryResponse
 * @see JoinRequestResponse
 */
public interface ApiController {

  // ===== USER AUTHENTICATION & REGISTRATION =====

  /**
   * Registers a new user account in the CoActivity system.
   * <p>
   * Creates a new user with the provided personal information and prepares the account for
   * immediate use. The email address becomes the immutable login identifier, and the password is
   * securely hashed before storage. After successful registration, users must complete the login
   * process to obtain a JWT token.
   * </p>
   *
   * @param request user registration data containing all required personal information
   * @return {@link ApiResponse} containing {@link RegistrationResponse} with new user ID and
   * username confirmation, or error details if registration fails
   * @throws IllegalArgumentException if required parameters are missing or invalid
   */
  ApiResponse<RegistrationResponse> registerUser(UserRegistrationRequest request);

  /**
   * Initiates the user authentication process.
   * <p>
   * Verifies the provided credentials and, if valid, sends a 5-digit verification code to the
   * user's registered email address. This implements the two-factor authentication requirement
   * specified in the functional requirements. The verification code expires after 10 minutes for
   * security purposes.
   * </p>
   *
   * @param request login credentials containing email and password
   * @return {@link ApiResponse} with empty data but success status indicating the verification code
   * was sent, or error details for invalid credentials
   * @throws IllegalArgumentException if email format is invalid or password is empty
   */
  ApiResponse<Void> loginUser(LoginRequest request);

  /**
   * Step 2 of login: User enters the 5-digit code from email to get final access.
   * <p>
   * Flow:
   * <p>
   * 1. User calls loginUser(request) → gets "code sent to email" message
   * <p>
   * 2. User checks email, finds code like "38472"
   * <p>
   * 3. User calls verifyLogin(email, "38472") → gets JWT token for 30 minutes
   * <p>
   * 4. User uses that token for all other API calls
   *
   * @param login            same email used in step 1
   * @param verificationCode the 5-digit number from email
   * @return JWT token that works for 30 minutes
   */
  ApiResponse<LoginResponse> verifyLogin(String login, String verificationCode);

  /**
   * Validates a JWT token and checks session status.
   * <p>
   * Verifies that the provided token is properly signed, has not expired, and contains valid user
   * claims. This method enables the "no re-login within 30 minutes" functionality by allowing
   * clients to check token validity and automatically extend sessions when appropriate.
   * </p>
   *
   * @param token the JWT token to validate, typically from the {@code Authorization} header
   * @return {@link ApiResponse} with empty data but success status indicating token validity, or
   * error details for invalid/expired tokens
   */
  ApiResponse<Void> validateToken(String token);

  // ===== USER PROFILE MANAGEMENT =====

  /**
   * Retrieves complete profile information for the authenticated user.
   * <p>
   * Returns all publicly accessible user data excluding sensitive information like passwords and
   * internal system fields. This method requires a valid JWT token and returns data for the user
   * identified in the token payload.
   * </p>
   *
   * @param token valid JWT token obtained during authentication
   * @return {@link ApiResponse} containing {@link UserProfileResponse} with complete user profile
   * data, or error details for invalid tokens or missing users
   */
  ApiResponse<UserProfileResponse> getUserProfile(String token);

  /**
   * Updates the authenticated user's profile information.
   * <p>
   * Modifies the specified profile fields for the user identified by the JWT token. All fields
   * except the login email (which is immutable) can be updated. Partial updates are supported -
   * only provided fields are modified, others retain their current values. Null fields are ignored
   * during the update process.
   * </p>
   *
   * @param token   valid JWT token for the user being updated
   * @param request profile update parameters containing fields to modify
   * @return {@link ApiResponse} containing the updated {@link UserProfileResponse}, or error
   * details for invalid tokens or validation failures
   */
  ApiResponse<UserProfileResponse> updateUserProfile(String token,
      UserProfileUpdateRequest request);

  // ===== ROOM MANAGEMENT =====

  /**
   * Creates a new room or activity in the system.
   * <p>
   * Establishes a new room with the specified parameters and assigns the authenticated user as the
   * room owner. Room visibility settings determine whether users can join directly (public) or
   * require approval (private). The creator automatically becomes the first participant with full
   * administrative privileges.
   * </p>
   *
   * @param token   valid JWT token of the room creator
   * @param request room creation parameters including visibility, category, and capacity
   * @return {@link ApiResponse} containing {@link RoomCreationResponse} with the new room ID and
   * basic information, or error details for validation failures
   * @throws IllegalArgumentException if required parameters are missing or invalid
   */
  ApiResponse<RoomCreationResponse> createRoom(String token, RoomCreationRequest request);

  /**
   * Retrieves rooms based on specified filtering and sorting criteria.
   *
   * @param filter structured filter criteria for searching rooms, or {@code null} for all rooms
   * @param sortBy sorting preference, or {@code null} for default sorting
   * @param token  optional JWT token for personalized results (user's city, country, age-based
   *               filtering)
   * @return filtered and sorted list of room summaries
   * @example // Get sport rooms in Moscow, sorted by popularity RoomFilter filter = new
   * RoomFilter(); filter.setCategory(Category.Sport); filter.setCity("Moscow");
   * <p>
   * ApiResponse<List<RoomSummaryResponse>> response = getRooms(filter, RoomSort.POPULAR,
   * userToken);
   */
  ApiResponse<List<RoomSummaryResponse>> getRooms(RoomFilter filter, RoomSort sortBy, String token);

  /**
   * Retrieves detailed information for a specific room.
   * <p>
   * Returns complete room details including settings, participant count, and creator information.
   * For private rooms, additional details may be restricted based on the user's participation
   * status and the provided authentication token.
   * </p>
   *
   * @param roomId unique identifier of the room to retrieve
   * @param token  optional JWT token for permission-based access to private room details
   * @return {@link ApiResponse} containing {@link RoomSummaryResponse} with room details, or error
   * details for invalid room IDs or access restrictions
   */
  ApiResponse<RoomSummaryResponse> getRoomById(Integer roomId, String token);

  // ===== ROOM PARTICIPATION =====

  /**
   * Requests to join a room as a participant.
   * <p>
   * For public rooms, the user is added immediately as a participant. For private rooms, a join
   * request is created with status {@link RequestStatus#Consideration} awaiting administrator
   * approval. If the room has reached maximum capacity, the request is automatically rejected.
   * </p>
   *
   * @param token  valid JWT token of the user requesting to join
   * @param roomId unique identifier of the room to join
   * @return {@link ApiResponse} with empty data but success status indicating the join request was
   * processed (immediate join or request submitted), or error details for invalid rooms or capacity
   * limits
   */
  ApiResponse<Void> joinRoom(String token, Integer roomId);

  /**
   * Removes the authenticated user from a room.
   * <p>
   * Voluntarily leaves the specified room, ending participation. Room owners cannot leave their own
   * rooms unless they first transfer ownership or delete the room. This operation is immediate and
   * cannot be undone.
   * </p>
   *
   * @param token  valid JWT token of the user leaving the room
   * @param roomId unique identifier of the room to leave
   * @return {@link ApiResponse} with empty data but success status confirming the user has left the
   * room, or error details for invalid rooms or ownership restrictions
   */
  ApiResponse<Void> leaveRoom(String token, Integer roomId);

  // ===== REQUEST MANAGEMENT =====

  /**
   * Retrieves pending join requests for rooms where the user has administrative privileges.
   * <p>
   * Returns a list of all join requests requiring review for rooms where the authenticated user is
   * an owner or has been granted request approval permissions. Each request includes user
   * information, room context, and submission timestamp to facilitate informed decisions.
   * </p>
   *
   * @param token valid JWT token of a user with room administration privileges
   * @return {@link ApiResponse} containing a list of {@link JoinRequestResponse} objects
   * representing pending requests, or empty list if no pending requests exist
   */
  ApiResponse<List<JoinRequestResponse>> getPendingRequests(String token);

  /**
   * Retrieves pending join requests for a specific private room where the user has administrative
   * privileges.
   * <p>
   * This method allows room administrators to review all pending join requests for a particular
   * private room they manage. It provides focused context for making approval/rejection decisions
   * by showing only requests relevant to the specified room.
   * </p>
   *
   * @param token  valid JWT token of a user with administrative privileges for the room
   * @param roomId unique identifier of the private room to retrieve requests for
   * @return {@link ApiResponse} containing a list of {@link JoinRequestResponse} objects
   * representing pending join requests for the specified room, or empty list if no pending requests
   * exist or the room is public
   * @throws SecurityException if the authenticated user lacks administrative privileges for the
   *                           specified room
   */
  ApiResponse<List<JoinRequestResponse>> getPendingRequestsForRoom(String token, Integer roomId);

  /**
   * Processes a room join request with the specified action.
   * <p>
   * Applies the requested action to a pending join request, either accepting the user into the room
   * or rejecting the request. Only users with administrative privileges for the room can process
   * join requests. Rejected requests can include an optional ban to prevent future requests from
   * the same user.
   * </p>
   *
   * @param token     valid JWT token of a user with room administration privileges
   * @param requestId unique identifier of the join request to process
   * @param action    the action to perform: {@link RequestStatus#Accepted} to approve,
   *                  {@link RequestStatus#Refused} to reject
   * @return {@link ApiResponse} with empty data but success status confirming the request was
   * processed, or error details for invalid requests or insufficient permissions
   */
  ApiResponse<Void> processJoinRequest(String token, Integer requestId, RequestStatus action);

  /**
   * Retrieves join requests submitted by the authenticated user.
   * <p>
   * Returns a list of all rooms where the user has submitted join requests, including the current
   * status of each request. This allows users to track their pending applications and see which
   * rooms they've been accepted into or rejected from.
   * </p>
   *
   * @param token valid JWT token of the user whose requests are being retrieved
   * @return {@link ApiResponse} containing a list of {@link JoinRequestResponse} objects
   * representing the user's sent requests, or empty list if no requests exist
   */
  ApiResponse<List<JoinRequestResponse>> getSentRequests(String token);

  /**
   * Cancels a pending join request submitted by the authenticated user.
   * <p>
   * Withdraws a join request that is still in {@link RequestStatus#Consideration} status. This
   * operation is only available for requests that haven't yet been processed by room
   * administrators. Cancelled requests are permanently removed and cannot be restored.
   * </p>
   *
   * @param token     valid JWT token of the user who submitted the request
   * @param requestId unique identifier of the join request to cancel
   * @return {@link ApiResponse} with empty data but success status confirming the request was
   * cancelled, or error details for invalid requests or already-processed requests
   */
  ApiResponse<Void> cancelRequest(String token, Integer requestId);

// ===== Q&A SYSTEM =====

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
   * @return {@link ApiResponse} containing the created {@link QuestionResponse} with assigned
   * question ID and metadata
   * @example Ask about music equipment in the Music category QuestionRequest request = new
   * QuestionRequest(); request.setCategoryId(2); // Music category request.setQuestion("What's the
   * best beginner guitar for under $300?");
   * <p>
   * ApiResponse<QuestionResponse> response = askQuestion(userToken, request);
   */
  ApiResponse<QuestionResponse> askQuestion(String token, QuestionRequest request);

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
   * @return {@link ApiResponse} containing the created {@link AnswerResponse} with threading
   * information and author details
   * @example // Answer a question about beginner guitars AnswerRequest request = new
   * AnswerRequest(); request.setQuestionId(123); request.setAnswer("I recommend the Yamaha FG800
   * for beginners - great value at $250!");
   * <p>
   * ApiResponse<AnswerResponse> response = answerQuestion(userToken, request);
   * @example // Reply to a specific previous answer AnswerRequest replyRequest = new
   * AnswerRequest(); replyRequest.setQuestionId(123); replyRequest.setPreviousAnswerId(456); //
   * Replying to answer #456 replyRequest.setAnswer("I agree, the Yamaha FG800 is excellent!");
   * <p>
   * ApiResponse<AnswerResponse> replyResponse = answerQuestion(userToken, replyRequest);
   */
  ApiResponse<AnswerResponse> answerQuestion(String token, AnswerRequest request);

  /**
   * Retrieves all questions in the Q&A system with optional category filtering.
   * <p>
   * Returns a list of questions sorted by most recent activity (most recently answered questions
   * first).
   * </p>
   *
   * @param token      optional JWT token for personalized sorting
   * @param categoryId optional category filter to show only questions from a specific category, or
   *                   {@code null} for all categories
   * @return {@link ApiResponse} containing a list of {@link QuestionResponse} objects matching the
   * criteria
   */
  ApiResponse<List<QuestionResponse>> getQuestions(String token, Integer categoryId);

  /**
   * Retrieves a specific question and its complete answer hierarchy.
   * <p>
   * Returns the full question details along with all answers organized in threaded conversation
   * format. Top-level answers are direct responses to the question, with nested replies forming
   * discussion trees under each parent answer.
   * </p>
   *
   * @param token      optional JWT token for tracking user engagement
   * @param questionId unique identifier of the question to retrieve
   * @return {@link ApiResponse} containing {@link QuestionWithAnswersResponse} with the question
   * and hierarchical answer structure
   * @example // Get question #123 with all its answers and replies
   * ApiResponse<QuestionWithAnswersResponse> response = getQuestionWithAnswers(userToken, 123);
   * <p>
   * // Response structure: // { //   "question": { ... }, //   "answers": [ //     { //       "id":
   * 456, //       "answer": "Main answer text", //       "replies": [ //         { // "id": 789, //
   * "answer": "Reply to main answer", //           "replies": [] // } //       ] //     } //   ] //
   * }
   */
  ApiResponse<QuestionWithAnswersResponse> getQuestionWithAnswers(String token, Integer questionId);
}