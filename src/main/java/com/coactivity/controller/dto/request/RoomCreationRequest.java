package com.coactivity.controller.dto.request;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating a new room or activity.
 * <p>
 * Contains all necessary information to establish a new room with specified visibility, category,
 * capacity, and descriptive details. Room creators automatically become owners with full
 * administrative privileges.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomCreationRequest {

  /**
   * Determines the room's visibility and join requirements.
   * <p>
   * {@code true} for public rooms where any user can join directly, {@code false} for private rooms
   * requiring administrator approval for joining.
   * </p>
   */
  private Boolean isPublic;

  /**
   * Category identifier for organizing and filtering rooms.
   * <p>
   * Determines how the room is grouped in category-based navigation and search results. Must
   * reference an existing system category.
   * </p>
   */
  private Integer categoryId;

  /**
   * Public name or title of the room.
   * <p>
   * Displayed in room lists, search results, and throughout the application. Must be between 3 and
   * 100 characters and unique within the system.
   * </p>
   */
  private String name;

  /**
   * Detailed description of the room's purpose, activities, and expectations.
   * <p>
   * Provides context for potential participants about what the room offers and what kind of
   * engagement is expected. Maximum 2000 characters.
   * </p>
   */
  private String description;

  /**
   * Maximum number of participants allowed in the room.
   * <p>
   * Enforces capacity limits to maintain room quality and engagement. Must be at least 2 (room
   * creator + one participant) and maximum 100,000 for system performance considerations.
   * </p>
   */
  private Integer maximumNumberOfPeople;

  /**
   * External communication link for room participants.
   * <p>
   * Optional URL or connection string for external chat services (Discord, Telegram, etc.)
   * that room participants can use for real-time communication. Visible only to room members
   * after joining.
   * </p>
   */
  private String chatLink;

  /**
   * Scheduled start time for the room's primary activity or event.
   * <p>
   * Defines when the main activity begins. Used for scheduling and reminder notifications.
   * Can be {@code null} for ongoing rooms without specific start times.
   * </p>
   */
  private Instant dateOfStartEvent;

  /**
   * Scheduled end time for the room's primary activity or event.
   * <p>
   * Defines when the main activity concludes. Used for scheduling and automatic room closure.
   * Can be {@code null} for ongoing rooms without specific end times.
   * </p>
   */
  private Instant dateOfEndEvent;

  /**
   * Recurrence pattern for repeating activities.
   * <p>
   * Defines how often the room's activities repeat (daily, weekly, etc.). Used for scheduling
   * recurring events and generating calendar invites. Format and interpretation depends on
   * room category and activity type.
   * </p>
   */
  private Instant frequency;

  /**
   * Age restriction level for room participation.
   * <p>
   * Minimum age requirement for joining the room. Must be between 0 (no restriction) and 21.
   * Used to enforce age-appropriate content and comply with platform safety policies.
   * </p>
   */
  private int ageRating;
}