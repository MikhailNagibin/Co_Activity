package com.coactivity.domain.enums;

/**
 * Defines the available sorting strategies for room listings in the Co-Activity platform.
 * <p>
 * Sorting preferences affect how rooms are ordered when displayed to users on the main page,
 * search results, and category listings. Each strategy serves specific user interaction patterns
 * and discovery scenarios.
 * </p>
 *
 * <p><b>Usage Examples:</b>
 * <ul>
 *   <li>{@code NEWEST} - For users wanting to discover recently created activities</li>
 *   <li>{@code POPULAR} - For users seeking active, well-attended rooms</li>
 *   <li>{@code NAME} - For users browsing alphabetically or searching specific rooms</li>
 *   <li>{@code UPCOMING} - For time-sensitive activities with scheduled start times</li>
 * </ul>
 * </p>
 */
public enum RoomSort {

  /**
   * Rooms ordered by creation timestamp, most recently created first.
   * <p>
   * This sorting strategy prioritizes new content, helping users discover recently
   * launched activities. It's particularly useful for returning users who want to
   * see what's new since their last visit.
   * </p>
   * <p><b>Database Field:</b> {@code rooms.created_at DESC}</p>
   */
  NEWEST,

  /**
   * Rooms ordered by participant count, highest participation first.
   * <p>
   * This strategy surfaces popular and active rooms, providing social proof and
   * helping users find communities with established engagement. Rooms nearing
   * maximum capacity may be deprioritized to avoid disappointment.
   * </p>
   * <p><b>Database Field:</b> {@code rooms.participant_count DESC}</p>
   */
  POPULAR,

  /**
   * Rooms ordered alphabetically by name, A to Z.
   * <p>
   * Useful for users who know what they're looking for or prefer browsing
   * through an organized, predictable list. Case-insensitive sorting is
   * applied to ensure consistent user experience.
   * </p>
   * <p><b>Database Field:</b> {@code LOWER(rooms.name) ASC}</p>
   */
  NAME,

  /**
   * Rooms ordered by scheduled start time, soonest events first.
   * <p>
   * Specifically designed for time-bound activities with scheduled start dates.
   * Rooms without scheduled start times are listed after time-bound activities.
   * This helps users find imminent events and plan their participation.
   * </p>
   * <p><b>Database Field:</b> {@code rooms.date_of_start_event ASC NULLS LAST}</p>
   */
  UPCOMING
}