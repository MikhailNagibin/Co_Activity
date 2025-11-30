package com.coactivity.controller;

import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.controller.dto.request.RoomFilter;
import com.coactivity.controller.dto.request.RoomSort;
import com.coactivity.controller.dto.response.BulletinBoardResponse;
import com.coactivity.controller.dto.response.MembershipVerificationResponse;
import com.coactivity.controller.dto.response.RoomCreationResponse;
import com.coactivity.controller.dto.response.RoomDetailedResponse;
import com.coactivity.controller.dto.response.RoomParticipantResponse;
import com.coactivity.controller.dto.response.RoomSummaryResponse;
import com.coactivity.domain.RequestStatus;
import com.coactivity.domain.Role;
import java.util.List;
import org.springframework.http.ResponseEntity;

/**
 * Core API controller interface for the CoActivity platform.
 * <p>
 * Defines the complete contract for user management, room operations, and activity participation.
 * All methods now return {@link ResponseEntity} values so controllers can express HTTP status,
 * headers, and DTO payloads explicitly. Business logic belongs in services; controllers simply map
 * to HTTP semantics and rely on DTO payloads instead of {@code ApiResponse} wrappers.
 *
 * @author CoActivity 13 Development Team
 * </p>
 */
public interface RoomController {

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
   * @return {@link ResponseEntity} containing {@link RoomCreationResponse} with the new room ID and
   * basic information, or an appropriate HTTP error status for validation failures
   * @throws IllegalArgumentException if required parameters are missing or invalid
   */
  ResponseEntity<RoomCreationResponse> createRoom(String token, RoomCreationRequest request);

  /**
   * Updates or creates if board does not exist the bulletin board content for a specific room.
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
   * @return {@link ResponseEntity} containing the updated {@link BulletinBoardResponse} with new
   * content and updated timestamp
   * @throws SecurityException        if user lacks administrative privileges for the room
   * @throws IllegalArgumentException if room doesn't exist or content is invalid
   * @see BulletinBoardResponse
   */
  ResponseEntity<BulletinBoardResponse> updateBulletinBoard(String token, Integer roomId,
      String newContent);

  /**
   * Deletes the bulletin board for a specific room.
   * <p>
   * Allows room administrators to delete the bulletin board if current information is deprecated
   * </p>
   *
   * <p><b>Access Control:</b> Requires valid authentication and administrative privileges
   * in the specified room. Only users with owner or administrator roles can modify the bulletin
   * board content.</p>
   *
   * @param token  valid JWT token of a user with room administration privileges
   * @param roomId unique identifier of the room to update
   * @return {@link ResponseEntity} with {@code 204 No Content} when the board is deleted
   * @throws SecurityException        if user lacks administrative privileges for the room
   * @throws IllegalArgumentException if room doesn't exist or content is invalid
   */
  ResponseEntity<Void> deleteBulletinBoard(String token, Integer roomId);

  /**
   * Retrieves rooms based on specified filtering and sorting criteria.
   * <p>
   * <b>Access Control:</b> Token optional. When provided, the response indicates whether
   * the requesting user already participates in each room.
   * </p>
   *
   * @param token  optional JWT token of the requesting user
   * @param filter structured filter criteria for searching rooms, or {@code null} to return all
   *               active rooms
   * @param sortBy sorting preference, or {@code null} for default relevance-based sorting
   * @return {@link ResponseEntity} containing list of {@link RoomSummaryResponse}
   * @see RoomSummaryResponse
   * @see RoomFilter
   * @see RoomSort
   */
  ResponseEntity<List<RoomSummaryResponse>> getRooms(String token, RoomFilter filter,
      RoomSort sortBy);

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
   * @return {@link ResponseEntity} containing {@link RoomDetailedResponse} with access-appropriate
   * data exposure based on whether a valid token was provided
   */
  ResponseEntity<RoomDetailedResponse> getRoomById(Integer roomId, String token);

  /**
   * Retrieves all rooms where the authenticated user is an active participant.
   * <p>
   * Provides users with a comprehensive view of their current activity engagements, including both
   * rooms they've joined and rooms they administer. This method serves as the user's personal
   * activity dashboard, showing all rooms where they have an active membership regardless of their
   * role (participant, administrator, or owner).
   * </p>
   *
   * <p><b>Access Control:</b> Requires valid authentication. Users can only view
   * their own room participations. The response includes both public and private rooms where the
   * user has membership.</p>
   *
   * <p><b>Response Details:</b> Returns room summaries with full details including
   * protected content like chat links and bulletin boards, since the user is a verified participant
   * in these rooms.</p>
   *
   * @param token valid JWT token of the authenticated user
   * @return {@link ResponseEntity} containing list of {@link RoomDetailedResponse} with complete
   * room information including protected content for all rooms where the user has active
   * participation
   * @throws SecurityException if authentication token is invalid or expired
   * @see RoomDetailedResponse
   * @see #getRooms(String, RoomFilter, RoomSort)
   * @see #getRoomById(Integer, String)
   */
  ResponseEntity<List<RoomDetailedResponse>> getUserRooms(String token);

  // ===== ROOM PARTICIPATION =====

  /**
   * Requests to join a room as a participant.
   * <p>
   * For public rooms, the user is added immediately as a participant. For private rooms, a join
   * request is created with status {@link RequestStatus#CONSIDERATION} awaiting administrator
   * approval. If the room has reached maximum capacity, the request is automatically rejected.
   * </p>
   *
   * @param token  valid JWT token of the user requesting to join
   * @param roomId unique identifier of the room to join
   * @return {@link ResponseEntity} with {@code 204 No Content} once the join request is processed
   * (immediate join or request submitted), or an error status for invalid rooms or capacity limits
   */
  ResponseEntity<Void> joinRoom(String token, Integer roomId);

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
   * @return {@link ResponseEntity} with {@code 204 No Content} confirming the user has left the
   * room, or an error status for invalid rooms or ownership restrictions
   */
  ResponseEntity<Void> leaveRoom(String token, Integer roomId);

  /**
   * Permanently deletes a room and all associated data.
   * <p>
   * Completely removes a room from the system, including all participant relationships, bulletin
   * board content, and room settings. This operation is irreversible and should only be available
   * to the room owner with appropriate confirmation steps.
   * </p>
   *
   * <p><b>Access Control:</b> Requires valid authentication and owner privileges
   * for the specified room. Only the room creator can delete the room.</p>
   *
   * <p><b>Impact:</b> All room participants will lose access and any ongoing
   * activities will be terminated. Participants should be notified of the room deletion.</p>
   *
   * @param token  valid JWT token of the room owner
   * @param roomId unique identifier of the room to delete
   * @return {@link ResponseEntity} with {@code 204 No Content} confirming room deletion
   * @throws SecurityException        if user is not the room owner
   * @throws IllegalArgumentException if room doesn't exist or is already deleted
   * @example
   * @see #createRoom(String, RoomCreationRequest)
   */
  ResponseEntity<Void> deleteRoom(String token, Integer roomId);

  /**
   * Retrieves the list of participants for a specific room with optional role filtering.
   * <p>
   * Provides room administrators with comprehensive participant information including user details,
   * roles, join dates, and activity timestamps. Enables effective room management and moderation
   * with flexible filtering capabilities.
   * </p>
   *
   * <p><b>Access Control:</b> Requires valid authentication and administrative privileges
   * (OWNER or ADMIN) in the specified room.</p>
   *
   * @param token      valid JWT token of a user with room administration privileges
   * @param roomId     unique identifier of the room to retrieve participants for
   * @param roleFilter optional role to filter participants by (e.g., only ADMINs). If null, all
   *                   participants are returned.
   * @return {@link ResponseEntity} containing list of participant information with enhanced user
   * details
   * @throws SecurityException        if user lacks administrative privileges for the room
   * @throws IllegalArgumentException if the room doesn't exist
   */
  ResponseEntity<List<RoomParticipantResponse>> getRoomParticipants(String token, Integer roomId,
      Role roleFilter);

  /**
   * Verifies whether a specific user is a participant in a given room.
   * <p>
   * This method is exclusively for room administrators (OWNER or ADMIN) to verify the membership
   * status of any user within their managed rooms. Regular participants cannot use this method to
   * check other users' membership status.
   * </p>
   *
   * <p><b>Access Control:</b> Requires valid authentication and administrative privileges
   * (OWNER or ADMIN) in the specified room. The requesting user must be an administrator of the
   * room they are checking.</p>
   *
   * @param token  valid JWT token of a user with room administration privileges
   * @param roomId unique identifier of the room to check
   * @param userId unique identifier of the user to verify membership for
   * @return {@link ResponseEntity} containing {@link MembershipVerificationResponse} with
   * verification results and role information, or indicates the user is not a participant
   * @throws SecurityException        if user lacks administrative privileges for the room
   * @throws IllegalArgumentException if the room or target user doesn't exist
   */
  ResponseEntity<MembershipVerificationResponse> isUserInRoom(String token, Integer roomId,
      Integer userId);
}
