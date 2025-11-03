package com.coactivity.dto.response;

import com.coactivity.domain.enums.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simplified room information for listing and search interfaces.
 * <p>
 * Contains essential room metadata optimized for display in lists, grids, and search results where
 * screen space is limited.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
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
   * Unique identifier of the user who created the room.
   * <p>
   * Can be used to fetch additional creator information if needed or to determine if the current
   * user has administrative privileges.
   * </p>
   */
  private Integer creatorId;
}