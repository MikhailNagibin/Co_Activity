package com.coactivity.controller.dto.response;

import com.coactivity.domain.Category;
import java.time.Instant;
import java.util.List;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * Simplified room information for listing and search interfaces.
 * <p>
 * Contains essential room metadata optimized for display in lists, grids, and search results where
 * screen space is limited. Designed for public access without authentication requirements. All
 * fields are safe for consumption by both authorized and unauthorized users.
 * </p>
 *
 * @see RoomDetailedResponse
 */
@Data
@SuperBuilder
public class RoomSummaryResponse {

  /**
   * Unique room identifier.
   */
  private Integer id;

  /**
   * Indicates whether the room is currently active and accepting participants.
   * <p>
   * Inactive rooms are typically completed, canceled, or archived activities that are no longer
   * available for participation.
   * </p>
   */
  private boolean isActive;

  /**
   * Indicates whether the room is publicly accessible.
   * <p>
   * {@code true} means any user can view and join the room directly without approval. {@code false}
   * means users must request and be approved by room administrators to join.
   * </p>
   */
  private Boolean isPublic;

  /**
   * Classification category for the room's primary activity.
   * <p>
   * Helps users quickly identify rooms relevant to their interests and enables effective filtering
   * and discovery of related activities.
   * </p>
   */
  private Category category;

  /**
   * Public display name of the room or activity.
   * <p>
   * Should be clear, descriptive, and engaging to help users understand the room's purpose at a
   * glance in list views.
   * </p>
   */
  private String name;

  /**
   * Brief description of the room's purpose or activities.
   * <p>
   * Provides additional context about the room's goals, activities, and expectations. Typically
   * truncated or summarized for optimal display in list and grid interfaces.
   * </p>
   */
  private String description;

  /**
   * Scheduled start time of the room's primary activity or event.
   * <p>
   * Uses {@link Instant} for timezone-agnostic timestamp representation, ensuring consistent
   * display across all user locations and devices.
   * </p>
   */
  private Instant dateOfStartEvent;

  /**
   * Scheduled end time of the room's primary activity or event.
   * <p>
   * Helps users understand the duration and time commitment required for participation.
   * </p>
   */
  private Instant dateOfEndEvent;

  /**
   * Minimum age requirement for room participation.
   * <p>
   * Ensures age-appropriate activities and compliance with platform guidelines. Users must meet or
   * exceed this age rating to join the room.
   * </p>
   */
  private int ageRating;

  /**
   * How frequently the room's activities occur, measured in days.
   * <p>
   * Indicates the recurrence pattern for ongoing activities. Common values include 1 (daily), 7
   * (weekly), 14 (bi-weekly), or 30 (monthly).
   * </p>
   */
  private Instant frequency;

  /**
   * Current number of active participants in the room.
   * <p>
   * Provides social proof and helps users gauge activity popularity and engagement levels before
   * deciding to join. Updated in real-time as users join and leave.
   * </p>
   */
  private Integer participantCount;

  /**
   * Maximum number of participants allowed in the room.
   * <p>
   * When {@code participantCount} equals this value, the room is at capacity and cannot accept
   * additional participants until existing members leave.
   * </p>
   */
  private Integer maximumParticipants;


  /**
   * Public profile information of the user who created the room.
   * <p>
   * Provides attribution and helps users identify rooms created by people they know or trust. Only
   * includes publicly accessible user information.
   * </p>
   */
  private UserSummaryResponse creator;

  /**
   * Indicates whether the current user is already a participant.
   * <p>
   * Only populated when an authenticated user accesses the API. Used to show "Joined" status
   * instead of "Join" buttons.
   * </p>
   */
  private Boolean isCurrentUserParticipant;

  /**
   * List of image identifiers associated with the room.
   * <p>
   * Contains unique identifiers for room-related images that can be used to construct image URLs
   * via the platform's media serving endpoints. The first image in the list is typically considered
   * the primary or featured image.
   * </p>
   */
  private List<Integer> imageIds;
}