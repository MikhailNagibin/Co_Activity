package com.coactivity.controller.dto.response;

import com.coactivity.domain.Category;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;

/**
 * Simplified room information for listing and search interfaces.
 * <p>
 * Contains essential room metadata optimized for display in lists, grids, and search results where
 * screen space is limited. Safe for both authorized and unauthorized users.
 * </p>
 *
 * @see RoomDetailedResponse
 */
@Data
@Builder
public class RoomSummaryResponse {

  /**
   * Unique room identifier.
   */
  private Integer id;

  /**
   * Shows whether room is active
   */
  private boolean isActive;

  /**
   * Indicates whether the room is publicly accessible.
   * <p>
   * {@code true} means any user can view and join the room directly. {@code false} means users must
   * request and be approved to join.
   * </p>
   */
  private Boolean isPublic;

  /**
   * Classification category for the room's primary activity.
   */
  private Category category;

  /**
   * Public name of the room or activity.
   */
  private String name;

  /**
   * Brief description of the room's purpose or activities.
   * <p>
   * Truncated or summarized for display in list views.
   * </p>
   */
  private String description;

  /**
   * The date when event begins
   */
  private Instant dateOfStartEvent;

  /**
   * The date when event ends
   */
  private Instant dateOfEndEvent;

  /**
   * Age rating of event.
   */
  private int ageRating;

  /**
   * Frequency of the event.
   */
  private int frequency;

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
   * User who created the room.
   * <p>
   * Provides attribution and helps users identify rooms created by people they know or trust.
   * </p>
   */
  private UserProfileResponse creator;

  /**
   * Indicates whether the current user is already a participant.
   * <p>
   * Only populated when an authenticated user accesses the API. Used to show "Joined" status
   * instead of "Join" buttons.
   * </p>
   */
  private Boolean isCurrentUserParticipant;
}