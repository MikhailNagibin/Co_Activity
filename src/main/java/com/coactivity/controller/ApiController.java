package com.coactivity.controller;

import com.coactivity.domain.entities.RoomFilter;
import com.coactivity.domain.enums.Category;
import com.coactivity.domain.enums.RequestStatus;
import com.coactivity.domain.enums.RoomSort;
import com.coactivity.dto.response.*;
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
 * {@code success} field indicates operation outcome. Clients should always check this field
 * before processing the data payload.</p>
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
   * Creates a new user with the provided personal information and prepares the account
   * for immediate use. The email address becomes the immutable login identifier, and
   * the password is securely hashed before storage. After successful registration,
   * users must complete the login process to obtain a JWT token.
   * </p>
   *
   * @param login the user's email address, used as the primary login identifier (immutable)
   * @param username public display name visible to other users (2-50 characters)
   * @param password account password for authentication (minimum 8 characters)
   * @param dateOfBirth the user's date of birth for age verification and personalization
   * @param city the user's city of residence for location-based features
   * @param country the user's country of residence for regional content
   * @param description optional biographical information about the user (max 500 characters)
   * @param avatarId identifier for the user's profile picture, or {@code null} for default
   * @return {@link ApiResponse} containing {@link RegistrationResponse} with new user ID
   *         and username confirmation, or error details if registration fails
   * @throws IllegalArgumentException if required parameters are missing or invalid
   */
  ApiResponse<RegistrationResponse> registerUser(String login, String username, String password,
      java.time.Instant dateOfBirth, String city,
      String country, String description, Integer avatarId);

  /**
   * Initiates the user authentication process.
   * <p>
   * Verifies the provided credentials and, if valid, sends a 5-digit verification code
   * to the user's registered email address. This implements the two-factor authentication
   * requirement specified in the functional requirements. The verification code expires
   * after 10 minutes for security purposes.
   * </p>
   *
   * @param login the user's registered email address
   * @param password the user's account password
   * @return {@link ApiResponse} with empty data but success status indicating the
   *         verification code was sent, or error details for invalid credentials
   * @throws IllegalArgumentException if email format is invalid or password is empty
   */
  ApiResponse<Void> loginUser(String login, String password);

  /**
   * Step 2 of login: User enters the 5-digit code from email to get final access.
   * <p>
   * Flow:
   * <p>
   * 1. User calls loginUser(email, password) → gets "code sent to email" message
   * <p>
   * 2. User checks email, finds code like "38472"
   * <p>
   * 3. User calls verifyLogin(email, "38472") → gets JWT token for 30 minutes
   * <p>
   * 4. User uses that token for all other API calls
   *
   * @param login same email used in step 1
   * @param verificationCode the 5-digit number from email
   * @return JWT token that works for 30 minutes
   */
  ApiResponse<LoginResponse> verifyLogin(String login, String verificationCode);

  /**
   * Validates a JWT token and checks session status.
   * <p>
   * Verifies that the provided token is properly signed, has not expired, and contains
   * valid user claims. This method enables the "no re-login within 30 minutes" functionality
   * by allowing clients to check token validity and automatically extend sessions when
   * appropriate.
   * </p>
   *
   * @param token the JWT token to validate, typically from the {@code Authorization} header
   * @return {@link ApiResponse} with empty data but success status indicating token validity,
   *         or error details for invalid/expired tokens
   */
  ApiResponse<Void> validateToken(String token);

  // ===== USER PROFILE MANAGEMENT =====

  /**
   * Retrieves complete profile information for the authenticated user.
   * <p>
   * Returns all publicly accessible user data excluding sensitive information like
   * passwords and internal system fields. This method requires a valid JWT token
   * and returns data for the user identified in the token payload.
   * </p>
   *
   * @param token valid JWT token obtained during authentication
   * @return {@link ApiResponse} containing {@link UserProfileResponse} with complete
   *         user profile data, or error details for invalid tokens or missing users
   */
  ApiResponse<UserProfileResponse> getUserProfile(String token);

  /**
   * Updates the authenticated user's profile information.
   * <p>
   * Modifies the specified profile fields for the user identified by the JWT token.
   * All fields except the login email (which is immutable) can be updated. Partial
   * updates are supported - only provided fields are modified, others retain their
   * current values.
   * </p>
   *
   * @param token valid JWT token for the user being updated
   * @param username new public display name (optional, null to keep current)
   * @param dateOfBirth updated date of birth (optional, null to keep current)
   * @param city updated city of residence (optional, null to keep current)
   * @param country updated country of residence (optional, null to keep current)
   * @param description updated biographical information (optional, null to keep current)
   * @param avatarId updated profile picture identifier (optional, null to keep current)
   * @return {@link ApiResponse} containing the updated {@link UserProfileResponse},
   *         or error details for invalid tokens or validation failures
   */
  ApiResponse<UserProfileResponse> updateUserProfile(String token, String username,
      java.time.Instant dateOfBirth, String city,
      String country, String description, Integer avatarId);

  // ===== ROOM MANAGEMENT =====

  /**
   * Creates a new room or activity in the system.
   * <p>
   * Establishes a new room with the specified parameters and assigns the authenticated
   * user as the room owner. Room visibility settings determine whether users can join
   * directly (public) or require approval (private). The creator automatically becomes
   * the first participant with full administrative privileges.
   * </p>
   *
   * @param token valid JWT token of the room creator
   * @param isPublic {@code true} for publicly accessible rooms, {@code false} for private
   *                 rooms requiring join approval
   * @param category the primary activity category for organizational purposes
   * @param name the room title or name (required, 3-100 characters)
   * @param description detailed description of the room's purpose and activities
   *                    (optional, max 1000 characters)
   * @param maximumNumberOfPeople maximum participant capacity (minimum 2, maximum 500)
   * @return {@link ApiResponse} containing {@link RoomCreationResponse} with the new
   *         room ID and basic information, or error details for validation failures
   * @throws IllegalArgumentException if required parameters are missing or invalid
   */
  ApiResponse<RoomCreationResponse> createRoom(String token, Boolean isPublic,
      Category category, String name,
      String description, Integer maximumNumberOfPeople);

  /**
   * Retrieves rooms based on specified filtering and sorting criteria.
   *
   * @param filter structured filter criteria for searching rooms, or {@code null} for all rooms
   * @param sortBy sorting preference, or {@code null} for default sorting
   * @param token optional JWT token for personalized results (user's city, country, age-based filtering)
   * @return filtered and sorted list of room summaries
   *
   * @example
   * // Get sport rooms in Moscow, sorted by popularity
   * RoomFilter filter = new RoomFilter();
   * filter.setCategory(Categories.Sport);
   * filter.setCity("Moscow");
   *
   * ApiResponse<List<RoomSummaryResponse>> response =
   *     getRooms(filter, RoomSort.POPULAR, userToken);
   */
  ApiResponse<List<RoomSummaryResponse>> getRooms(RoomFilter filter, RoomSort sortBy, String token);

  /**
   * Retrieves detailed information for a specific room.
   * <p>
   * Returns complete room details including settings, participant count, and creator
   * information. For private rooms, additional details may be restricted based on the
   * user's participation status and the provided authentication token.
   * </p>
   *
   * @param roomId unique identifier of the room to retrieve
   * @param token optional JWT token for permission-based access to private room details
   * @return {@link ApiResponse} containing {@link RoomSummaryResponse} with room details,
   *         or error details for invalid room IDs or access restrictions
   */
  ApiResponse<RoomSummaryResponse> getRoomById(Integer roomId, String token);

  // ===== ROOM PARTICIPATION =====

  /**
   * Requests to join a room as a participant.
   * <p>
   * For public rooms, the user is added immediately as a participant. For private rooms,
   * a join request is created with status {@link RequestStatus#Consideration} awaiting
   * administrator approval. If the room has reached maximum capacity, the request is
   * automatically rejected.
   * </p>
   *
   * @param token valid JWT token of the user requesting to join
   * @param roomId unique identifier of the room to join
   * @return {@link ApiResponse} with empty data but success status indicating the
   *         join request was processed (immediate join or request submitted),
   *         or error details for invalid rooms or capacity limits
   */
  ApiResponse<Void> joinRoom(String token, Integer roomId);

  /**
   * Removes the authenticated user from a room.
   * <p>
   * Voluntarily leaves the specified room, ending participation. Room owners cannot
   * leave their own rooms unless they first transfer ownership or delete the room.
   * This operation is immediate and cannot be undone.
   * </p>
   *
   * @param token valid JWT token of the user leaving the room
   * @param roomId unique identifier of the room to leave
   * @return {@link ApiResponse} with empty data but success status confirming the user
   *         has left the room, or error details for invalid rooms or ownership restrictions
   */
  ApiResponse<Void> leaveRoom(String token, Integer roomId);

  // ===== REQUEST MANAGEMENT =====

  /**
   * Retrieves pending join requests for rooms where the user has administrative privileges.
   * <p>
   * Returns a list of all join requests requiring review for rooms where the authenticated
   * user is an owner or has been granted request approval permissions. Each request includes
   * user information, room context, and submission timestamp to facilitate informed decisions.
   * </p>
   *
   * @param token valid JWT token of a user with room administration privileges
   * @return {@link ApiResponse} containing a list of {@link JoinRequestResponse} objects
   *         representing pending requests, or empty list if no pending requests exist
   */
  ApiResponse<List<JoinRequestResponse>> getPendingRequests(String token);

  /**
   * Processes a room join request with the specified action.
   * <p>
   * Applies the requested action to a pending join request, either accepting the user
   * into the room or rejecting the request. Only users with administrative privileges
   * for the room can process join requests. Rejected requests can include an optional
   * ban to prevent future requests from the same user.
   * </p>
   *
   * @param token valid JWT token of a user with room administration privileges
   * @param requestId unique identifier of the join request to process
   * @param action the action to perform: {@link RequestStatus#Accepted} to approve,
   *               {@link RequestStatus#Refused} to reject
   * @return {@link ApiResponse} with empty data but success status confirming the
   *         request was processed, or error details for invalid requests or
   *         insufficient permissions
   */
  ApiResponse<Void> processJoinRequest(String token, Integer requestId, RequestStatus action);

  /**
   * Retrieves join requests submitted by the authenticated user.
   * <p>
   * Returns a list of all rooms where the user has submitted join requests, including
   * the current status of each request. This allows users to track their pending
   * applications and see which rooms they've been accepted into or rejected from.
   * </p>
   *
   * @param token valid JWT token of the user whose requests are being retrieved
   * @return {@link ApiResponse} containing a list of {@link JoinRequestResponse} objects
   *         representing the user's sent requests, or empty list if no requests exist
   */
  ApiResponse<List<JoinRequestResponse>> getSentRequests(String token);

  /**
   * Cancels a pending join request submitted by the authenticated user.
   * <p>
   * Withdraws a join request that is still in {@link RequestStatus#Consideration} status.
   * This operation is only available for requests that haven't yet been processed by
   * room administrators. Cancelled requests are permanently removed and cannot be restored.
   * </p>
   *
   * @param token valid JWT token of the user who submitted the request
   * @param requestId unique identifier of the join request to cancel
   * @return {@link ApiResponse} with empty data but success status confirming the
   *         request was cancelled, or error details for invalid requests or
   *         already-processed requests
   */
  ApiResponse<Void> cancelRequest(String token, Integer requestId);
}