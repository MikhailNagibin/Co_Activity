package com.coactivity.dto.response;

import com.coactivity.domain.enums.Category;
import lombok.Builder;
import lombok.Data;

/**
 * Simplified room information for listing and search interfaces.
 * <p>
 * Contains essential room metadata optimized for display in lists, grids, and search results where
 * screen space is limited. Safe for both authorized and unauthorized users.
 * </p>
 */
@Data
@Builder
public class RoomSummaryResponse {

  /**
   * Unique room identifier.
   */
  private Integer id;

  /**
   * Public name of the room or activity.
   */
  private String name;

  /**
   * Classification category for the room's primary activity.
   */
  private Category category;

  /**
   * Brief description of the room's purpose or activities.
   * <p>
   * Truncated or summarized for display in list views.
   * </p>
   */
  private String description;

  /**
   * Indicates whether the room is publicly accessible.
   * <p>
   * {@code true} means any user can view and join the room directly. {@code false} means users must
   * request and be approved to join.
   * </p>
   */
  private Boolean isPublic;

  /**
   * Current number of active participants in the room.
   * <p>
   * This count helps users gauge the activity level and popularity of the room before deciding to
   * join.
   * </p>
   */
  private Integer participantCount;

  /**
   * Maximum number of participants allowed in the room.
   * <p>
   * When {@code participantCount} equals this value, no additional users can join until existing
   * participants leave.
   * </p>
   */
  private Integer maximumParticipants;

  /**
   * Display name of the user who created the room.
   * <p>
   * Provides attribution and helps users identify rooms created by people they know or trust.
   * </p>
   */
  private String creatorName;

  /**
   * Indicates whether the current user is already a participant.
   * <p>
   * Only populated when an authenticated user accesses the API. Used to show "Joined" status
   * instead of "Join" buttons.
   * </p>
   */
  private Boolean isCurrentUserParticipant;
}