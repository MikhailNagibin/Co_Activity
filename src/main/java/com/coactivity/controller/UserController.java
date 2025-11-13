package com.coactivity.controller;

import com.coactivity.controller.dto.request.LoginRequest;
import com.coactivity.controller.dto.request.NotificationSettingsRequest;
import com.coactivity.controller.dto.request.UserProfileUpdateRequest;
import com.coactivity.controller.dto.request.UserRegistrationRequest;
import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.controller.dto.response.JoinRequestResponse;
import com.coactivity.controller.dto.response.LoginResponse;
import com.coactivity.controller.dto.response.MembershipVerificationResponse;
import com.coactivity.controller.dto.response.NotificationSettingsResponse;
import com.coactivity.controller.dto.response.RegistrationResponse;
import com.coactivity.controller.dto.response.RoleAssignmentResponse;
import com.coactivity.controller.dto.response.UserProfileResponse;
import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.domain.RequestStatus;
import java.util.List;

/**
 * Core API controller interface for the CoActivity platform.
 * <p>
 * Defines the complete contract for user management, room operations, and activity participation.
 * All methods return standardized {@link ApiResponse} wrappers containing either the requested data
 * or detailed error information. JWT tokens are used for stateless authentication with 30-minute
 * session persistence.
 * <p><b>Error Handling:</b> All methods return {@link ApiResponse} instances where the
 * {@code success} field indicates operation outcome. Clients should always check this field before
 * processing the data payload.</p>
 *
 * @author CoActivity 13 Development Team
 * </p>
 */
public interface UserController {

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

  /**
   * Invalidates the user's current authentication token and ends the session.
   * <p>
   * Securely logs the user out by invalidating the provided JWT token on the server side. After
   * calling this method, the token can no longer be used for authenticated requests, providing an
   * additional security layer for session management.
   * </p>
   *
   * <p><b>Access Control:</b> Requires valid authentication. The token provided will be
   * invalidated immediately and cannot be reused.</p>
   *
   * @param token valid JWT token to invalidate (this will be the last successful use)
   * @return {@link ApiResponse} with success status confirming the logout
   * @throws SecurityException if the token is already invalid or expired
   * @example // Log out the current user ApiResponse<Void> response = logout(userToken); //
   * Response: { "success": true, "message": "Logged out successfully", "data": null }
   * @see #loginUser(LoginRequest)
   * @see #verifyLogin(String, String)
   */
  ApiResponse<Void> logoutUser(String token);

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
  ApiResponse<UserProfileResponse> getUserProfile(int token);

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
   * @see #getUserProfile(int)
   */
  ApiResponse<UserSummaryResponse> getPublicUserProfileById(String token, Integer userId);

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

  /**
   * Updates the user's notification preferences and delivery settings.
   * <p>
   * Allows users to customize how and when they receive notifications from the platform. Users can
   * enable or disable specific notification types and choose their preferred delivery channels
   * (email, push notifications, etc.) based on their preferences.
   * </p>
   *
   * <p><b>Access Control:</b> Requires valid authentication. Users can only modify
   * their own notification settings.</p>
   *
   * @param token   valid JWT token of the user updating their preferences
   * @param request contains the notification settings to apply
   * @return {@link ApiResponse} containing the updated {@link NotificationSettingsResponse} with
   * applied preferences
   * @throws SecurityException if the token is invalid or expired
   * @example // Configure notification settings NotificationSettingsRequest request = new
   * NotificationSettingsRequest(); request.setEmailNotifications(true);
   * request.setPushNotifications(false); request.setMembershipAlerts(true);
   * request.setActivityUpdates(false);
   * <p>
   * ApiResponse<NotificationSettingsResponse> response = configureNotificationSettings(userToken,
   * request);
   * @see NotificationSettingsRequest
   * @see NotificationSettingsResponse
   */
  ApiResponse<NotificationSettingsResponse> configureNotificationSettings(String token,
      NotificationSettingsRequest request);

  /**
   * Permanently deletes the user's account and all associated data.
   * <p>
   * Completely removes the user account from the system, including profile information, room
   * participations, and generated content. This operation is irreversible and should be accompanied
   * by appropriate confirmation steps in the client interface.
   * </p>
   *
   * <p><b>Access Control:</b> Requires valid authentication. Users can only delete
   * their own accounts.</p>
   *
   * <p><b>Data Handling:</b> Depending on data retention policies, some information
   * may be anonymized rather than completely deleted for legal or analytical purposes.</p>
   *
   * @param token valid JWT token of the user requesting account deletion
   * @return {@link ApiResponse} with success status confirming account deletion
   * @throws SecurityException if authentication fails or user doesn't exist
   * @see #registerUser(UserRegistrationRequest)
   */
  ApiResponse<Void> deleteAccount(String token);

  /**
   * Grants administrator privileges to a room participant.
   * <p>
   * Allows room owners to delegate administrative responsibilities by promoting regular
   * participants to administrator roles. Administrators can manage room settings, process join
   * requests, and moderate content within their assigned rooms.
   * </p>
   *
   * <p><b>Access Control:</b> Requires valid authentication and owner privileges
   * in the specified room. Only room owners can assign administrator roles.</p>
   *
   * @param token  valid JWT token of the room owner
   * @param roomId unique identifier of the room
   * @param userId unique identifier of the participant to promote
   * @return {@link ApiResponse} with success status and updated role information
   * @throws SecurityException        if user is not the room owner
   * @throws IllegalArgumentException if target user is not a room participant
   * @see RoleAssignmentResponse
   */
  ApiResponse<RoleAssignmentResponse> assignAdminRole(String token, Integer roomId,
      Integer userId);

  /**
   * Revokes administrator privileges from a room participant, returning them to regular participant
   * status.
   * <p>
   * Allows room owners to remove administrative privileges from users who previously held
   * administrator roles in the room. This operation demotes the user to a regular participant while
   * maintaining their membership in the room. Room owners cannot demote themselves.
   * </p>
   *
   * <p><b>Access Control:</b> Requires valid authentication and owner privileges
   * in the specified room. Only room owners can revoke administrator roles.</p>
   *
   * <p><b>Restrictions:</b>
   * <ul>
   *   <li>Room owners cannot demote themselves</li>
   *   <li>Target user must currently have administrator role</li>
   *   <li>Target user must remain a room participant after demotion</li>
   *   <li>At least one administrator (the owner) must always remain in the room</li>
   * </ul>
   * </p>
   *
   * @param token  valid JWT token of the room owner
   * @param roomId unique identifier of the room
   * @param userId unique identifier of the administrator to demote
   * @return {@link ApiResponse} with success status and updated role information
   * @throws SecurityException        if user is not the room owner or attempts self-demotion
   * @throws IllegalArgumentException if target user is not an administrator or doesn't exist
   * @throws IllegalStateException    if demotion would leave the room with no administrators
   * @see #assignAdminRole(String, Integer, Integer)
   * @see RoleAssignmentResponse
   */
  ApiResponse<RoleAssignmentResponse> demoteAdminRole(String token, Integer roomId, Integer userId);

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
   * @param action    the action to perform: {@link RequestStatus#ACCEPTED} to approve,
   *                  {@link RequestStatus#REFUSED} to reject,
   *                  {@link RequestStatus#REFUSED_WITH_BAN} to reject and ban the user
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
   * Withdraws a join request that is still in {@link RequestStatus#CONSIDERATION} status. This
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


  /**
   * Verifies whether a specific user is a participant in a given room.
   * <p>
   * Provides a programmatic way to check room membership without requiring QR code scanning. This
   * method serves as a backend alternative to the QR verification system and can be used for
   * administrative purposes or integration with other systems.
   * </p>
   *
   * <p><b>Access Control:</b> Requires valid authentication. Users can check their own
   * membership or, if they have administrative privileges in the room, they can check other users'
   * membership status.</p>
   *
   * @param token  valid JWT token of the requesting user
   * @param roomId unique identifier of the room to check
   * @param userId unique identifier of the user to verify
   * @return {@link ApiResponse} containing {@link MembershipVerificationResponse} with verification
   * results and role information
   * @throws SecurityException if user lacks permission to check membership
   * @see MembershipVerificationResponse
   */
  ApiResponse<MembershipVerificationResponse> isUserInRoom(String token, Integer roomId,
      Integer userId);
}
