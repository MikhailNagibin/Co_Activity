package com.coactivity.controller;

import com.coactivity.controller.dto.request.RoomCreationRequest;
import com.coactivity.controller.dto.request.RoomFilter;
import com.coactivity.controller.dto.request.RoomSort;
import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.controller.dto.response.BulletinBoardResponse;
import com.coactivity.controller.dto.response.RoomCreationResponse;
import com.coactivity.controller.dto.response.RoomDetailedResponse;
import com.coactivity.controller.dto.response.RoomSummaryResponse;
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
   * request is created with status {@link RequestStatus#CONSIDERATION} awaiting administrator
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
   * @return {@link ApiResponse} with success status confirming room deletion
   * @throws SecurityException        if user is not the room owner
   * @throws IllegalArgumentException if room doesn't exist or is already deleted
   * @example
   * @see #createRoom(String, RoomCreationRequest)
   */
  ApiResponse<Void> deleteRoom(String token, Integer roomId);
}
