package com.coactivity.controller;

import com.coactivity.controller.dto.request.AnswerRequest;
import com.coactivity.controller.dto.request.GenerateQrCodeRequest;
import com.coactivity.controller.dto.request.LoginRequest;
import com.coactivity.controller.dto.request.QuestionRequest;
import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.controller.dto.request.RoomFilter;
import com.coactivity.controller.dto.request.RoomSort;
import com.coactivity.controller.dto.request.UserProfileUpdateRequest;
import com.coactivity.controller.dto.request.UserRegistrationRequest;
import com.coactivity.controller.dto.request.VerifyQrCodeRequest;
import com.coactivity.controller.dto.response.AnswerResponse;
import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.controller.dto.response.BulletinBoardResponse;
import com.coactivity.controller.dto.response.JoinRequestResponse;
import com.coactivity.controller.dto.response.LoginResponse;
import com.coactivity.controller.dto.response.QrCodeResponse;
import com.coactivity.controller.dto.response.QuestionResponse;
import com.coactivity.controller.dto.response.QuestionWithAnswersResponse;
import com.coactivity.controller.dto.response.RegistrationResponse;
import com.coactivity.controller.dto.response.RoomCreationResponse;
import com.coactivity.controller.dto.response.RoomDetailedResponse;
import com.coactivity.controller.dto.response.RoomSummaryResponse;
import com.coactivity.controller.dto.response.UserProfileResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.controller.dto.response.VerificationResponse;
import com.coactivity.domain.RequestStatus;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;

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
@Validated
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
   * Retrieves public profile information for any user in the system.
   * <p>
   * Provides a safe, public view of user profiles that can be accessed by any authenticated user.
   * This method returns the same information that users make publicly available on their profiles,
   * excluding sensitive personal data.
   * </p>
   *
   * <p><b>Access Control:</b> Requires valid authentication. Any authenticated user
   * can view the public profile of any other user in the system.</p>
   *
   * @param token  valid JWT token of the requesting user
   * @param userId unique identifier of the user whose profile is being requested
   * @return {@link ApiResponse} containing {@link UserSummaryResponse} with public profile
   * information of the requested user
   * @throws SecurityException        if the requesting user is not authenticated
   * @throws IllegalArgumentException if the target user doesn't exist
   * @see UserSummaryResponse
   * @see #getUserProfile(String)
   */
  ApiResponse<UserSummaryResponse> getUserProfileById(String token, Integer userId);

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
   * Updates the bulletin board content for a specific room.
   * <p>
   * Allows room administrators to post announcements, updates, and important information that will
   * be visible to all room participants. The bulletin board serves as the primary communication
   * channel for room-related announcements and updates.
   * </p>
   *
   * <p><b>Access Control:</b> Requires valid authentication and administrative privileges
   * in the specified room. Only users with owner or administrator roles can modify the bulletin
   * board content.</p>
   *
   * @param token      valid JWT token of a user with room administration privileges
   * @param roomId     unique identifier of the room to update
   * @param newContent contains the new bulletin board content to display
   * @return {@link ApiResponse} containing the updated {@link BulletinBoardResponse} with new
   * content and updated timestamp
   * @throws SecurityException        if user lacks administrative privileges for the room
   * @throws IllegalArgumentException if room doesn't exist or content is invalid
   * @see BulletinBoardResponse
   */
  ApiResponse<BulletinBoardResponse> updateBulletinBoard(String token, Integer roomId,
      String newContent);

  /**
   * Retrieves rooms based on specified filtering and sorting criteria.
   * <p>
   * <b>Access Control:</b> Public endpoint - no authentication required.
   * Unauthorized users can call this method by passing {@code null} as the token parameter. All
   * users receive basic room information. Authenticated users receive additional context about
   * their participation status in each room.
   * </p>
   *
   * @param filter structured filter criteria for searching rooms, or {@code null} for all rooms
   * @param sortBy sorting preference, or {@code null} for default sorting
   * @param token  optional JWT token for personalized participation status. Pass {@code null} for
   *               unauthorized access.
   * @return {@link ApiResponse} containing list of {@link RoomSummaryResponse} with basic room
   * information for all users, plus participation context for authenticated users
   */
  ApiResponse<List<RoomSummaryResponse>> getRooms(RoomFilter filter, RoomSort sortBy, String token);

  /**
   * Retrieves detailed information for a specific room with conditional data exposure.
   * <p>
   * <b>Access Control:</b> Public endpoint - no authentication required for basic information.
   * Unauthorized users can access room details by passing {@code null} as the token parameter, but
   * will not receive protected data like chat links and bulletin board content. Room participants
   * receive full room details including protected content.
   * </p>
   *
   * @param roomId unique identifier of the room to retrieve
   * @param token  optional JWT token for access to protected room data (chat links, bulletin
   *               board). Pass {@code null} for unauthorized access to public room information
   *               only.
   * @return {@link ApiResponse} containing {@link RoomDetailedResponse} with access-appropriate
   * data exposure based on whether a valid token was provided
   */
  ApiResponse<RoomDetailedResponse> getRoomById(Integer roomId, String token);

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
   * <b>Access Control:</b> Public endpoint - no authentication required.
   * Implements read-only Q&A access for all users as specified in requirements. Questions are
   * sorted by most recent activity (most recently answered questions first).
   * </p>
   *
   * @param categoryId optional category filter to show only questions from a specific category, or
   *                   {@code null} to return questions from all categories
   * @return {@link ApiResponse} containing list of {@link QuestionResponse} objects with basic
   * question information and answer counts
   */
  ApiResponse<List<QuestionResponse>> getQuestions(Integer categoryId);

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
   * @return {@link ApiResponse} containing {@link QuestionWithAnswersResponse} with the question
   * and hierarchical answer structure
   */
  ApiResponse<QuestionWithAnswersResponse> getQuestionWithAnswers(Integer questionId);

  // ===== QR CODE VERIFICATION SYSTEM =====

  /**
   * Generates a time-sensitive QR code for physical participant verification at offline meetings.
   * <p>
   * Creates a cryptographically secure, one-time-use QR code that expires 60 seconds after
   * generation. This enables secure access control for in-person events by verifying that
   * individuals presenting at physical locations are legitimate participants of the specified
   * activity.
   * </p>
   *
   * <b>Usage Flow:</b>
   * <ol>
   *   <li>Room participant requests QR code generation for their current activity</li>
   *   <li>System validates user is active participant of specified room</li>
   *   <li>Generates unique, time-limited verification code</li>
   *   <li>Returns code data for display as QR image at meeting location</li>
   *   <li>Other participants scan QR code with mobile devices to verify attendance</li>
   * </ol>
   *
   * <b>Security Considerations:</b>
   * <ul>
   *   <li>QR codes are valid for exactly 60 seconds to minimize attack window</li>
   *   <li>Each code can only be used once, preventing replay attacks</li>
   *   <li>Codes are cryptographically random (32-byte secure random)</li>
   *   <li>Requires valid JWT token proving room participation</li>
   * </ul>
   *
   * @param token   Valid JWT authentication token identifying the requesting user. Must have active
   *                participant status in the specified room.
   * @param request Contains the room identifier for which to generate verification QR code. Room
   *                must exist and user must be participant.
   * @return {@link ApiResponse} containing {@link QrCodeResponse} with generated code data, or
   * error details if generation fails due to invalid permissions or room status.
   * @throws SecurityException        if user lacks participant status in specified room
   * @throws IllegalArgumentException if room ID is invalid or room doesn't exist
   * @see QrCodeResponse
   * @see GenerateQrCodeRequest
   * @see #verifyQrCode(String, VerifyQrCodeRequest)
   */
  @Valid
  ApiResponse<QrCodeResponse> generateQrCode(String token, GenerateQrCodeRequest request);

  /**
   * Verifies a scanned QR code to confirm participant eligibility for physical meeting access.
   * <p>
   * Validates that a QR code scanned at a physical meeting location is active, unexpired, and
   * belongs to the correct room context, while also confirming the scanning user's participant
   * status. This method implements the core access control logic for in-person events and generates
   * audit trails for attendance tracking.
   * </p>
   *
   * <b>Verification Checks:</b>
   * <ol>
   *   <li>QR code exists in system and matches provided room context</li>
   *   <li>Code has not expired (within 1-minute validity window)</li>
   *   <li>Code has not been previously used (one-time use enforcement)</li>
   *   <li>Scanning user is verified participant of the specified room</li>
   *   <li>All verifications pass → access granted and code marked as used</li>
   * </ol>
   *
   * <b>Typical Integration:</b>
   * <ul>
   *   <li>Mobile app scans QR code displayed at meeting entrance</li>
   *   <li>App extracts code and room ID, calls this verification endpoint</li>
   *   <li>System returns verification result with user/room details</li>
   *   <li>Meeting organizers see green checkmark for verified participants</li>
   * </ul>
   *
   * @param token   Valid JWT authentication token of the user scanning the QR code. Identifies the
   *                individual seeking physical access verification.
   * @param request Contains the scanned QR code data and room context for verification. Both code
   *                and room ID are required for context-aware validation.
   * @return {@link ApiResponse} containing {@link VerificationResponse} with detailed validation
   * lidation results, participant identity, and room information.
   * @throws SecurityException if verification process detects tampering or security violations
   * @example // Verify scanned QR code for room 12345 VerifyQrCodeRequest request = new
   * VerifyQrCodeRequest(); request.setQrCode("AbCdEfG123XYZ-7890_kJhGfDdSaQ");
   * request.setRoomId(12345L); ApiResponse<VerificationResponse> response =
   * apiController.verifyQrCode(userToken, request);
   * <p>
   * // Successful verification: // { //   "success": true, //   "message": "QR code verified
   * successfully", //   "data": { //     "valid": true, //     "message": "Verification successful
   * - John Doe confirmed for Basketball Club", //     "userName": "John Doe", //     "roomName":
   * "Weekly Basketball Practice", //     "userId": 67890, //     "roomId": 12345 //   } // }
   * <p>
   * // Failed verification (expired code): // { //   "success": true, // Note: API call succeeded,
   * verification failed //   "message": "QR code verification completed", //   "data": { //
   * "valid": false, //     "message": "QR code has expired - please generate a new code", //
   * "userName": null, //     "roomName": null, //     "userId": null, //     "roomId": null //   }
   * // }
   * @see VerificationResponse
   * @see VerifyQrCodeRequest
   * @see #generateQrCode(String, GenerateQrCodeRequest)
   */
  ApiResponse<VerificationResponse> verifyQrCode(String token, VerifyQrCodeRequest request);


}