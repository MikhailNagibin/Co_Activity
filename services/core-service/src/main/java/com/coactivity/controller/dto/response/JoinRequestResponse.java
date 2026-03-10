package com.coactivity.controller.dto.response;

import com.coactivity.domain.RequestStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Information about a room join request for administrative review.
 * <p>
 * Used by room administrators to review and process pending join requests from users wanting to
 * participate in private rooms.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinRequestResponse {

  /**
   * Unique identifier for this specific join request.
   * <p>
   * Used when processing (accepting/rejecting) the request to identify which specific request is
   * being acted upon.
   * </p>
   */
  private Integer requestId;

  /**
   * Unique identifier of the user requesting to join.
   */
  private Integer userId;

  /**
   * Public display name of the requesting user.
   * <p>
   * Allows administrators to identify the user without needing to fetch complete profile
   * information.
   * </p>
   */
  private String username;

  /**
   * Unique identifier of the room being requested to join.
   */
  private Integer roomId;

  /**
   * Name of the room being requested to join.
   * <p>
   * Provides context to administrators who may manage multiple rooms.
   * </p>
   */
  private String roomName;

  /**
   * Current status of the join request.
   * <p>
   * Indicates whether the request is pending review, has been accepted, or has been rejected by
   * room administrators.
   * </p>
   */
  private RequestStatus status;

  /**
   * Timestamp when the join request was originally submitted.
   * <p>
   * Helps administrators prioritize review of older requests and understand how long users have
   * been waiting for access.
   * </p>
   */
  private Instant createdAt;
}