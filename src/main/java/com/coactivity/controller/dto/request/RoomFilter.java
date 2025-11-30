package com.coactivity.controller.dto.request;

import com.coactivity.domain.Category;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents search and filtering criteria for querying rooms in the Co-Activity platform.
 * <p>
 * This value object encapsulates all available filtering options that users can apply
 * when browsing or searching for rooms. Filters are combined using AND logic, meaning
 * a room must match all specified criteria to be included in results.
 * </p>
 *
 * <p><b>Usage Pattern:</b>
 * <pre>{@code
 * RoomFilter filter = new RoomFilter();
 * filter.setCategory(Categories.SPORT);
 * filter.setQuery("yoga");
 * filter.setCity("Moscow");
 *
 * // Returns: Sport category rooms in Moscow containing "yoga" in name/description
 * List<Room> results = roomRepository.findByFilter(filter);
 * }</pre>
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomFilter {

  /**
   * Filters rooms by specific activity category.
   * <p>
   * When specified, only rooms belonging to this exact category are returned.
   * Category filtering is useful for users who know what type of activity
   * they're interested in (e.g., only Sport activities).
   * </p>
   * <p><b>Example:</b> {@code Categories.SPORT} returns only sport-related rooms</p>
   */
  private Category category;

  /**
   * Free-text search query applied to room names and descriptions.
   * <p>
   * Performs case-insensitive partial matching against room names and descriptions.
   * The search uses stemming and considers word boundaries to provide relevant results.
   * For example, searching "yoga" will match "Yoga Class", "Advanced Yoga", and
   * "yogalates".
   * </p>
   * <p><b>Search Behavior:</b>
   * <ul>
   *   <li>Searches both {@code room.name} and {@code room.description} fields</li>
   *   <li>Case-insensitive matching</li>
   *   <li>Partial word matching (contains semantics)</li>
   *   <li>Multiple words are treated as AND conditions</li>
   * </ul>
   * </p>
   * <p><b>Examples:</b>
   * <ul>
   *   <li>{@code "yoga"} → matches "Morning Yoga", "Yoga for Beginners"</li>
   *   <li>{@code "coding workshop"} → matches rooms containing both "coding" AND "workshop"</li>
   *   <li>{@code "music"} → matches "Music Production", "Classical Music Appreciation"</li>
   * </ul>
   * </p>
   */
  @Size(max = 255)
  private String query;

  /**
   * Filters rooms by their public/private visibility setting.
   * <p>
   * When {@code true}, only publicly accessible rooms are returned.
   * When {@code false}, only private rooms requiring join approval are returned.
   * When {@code null} (default), both public and private rooms are included,
   * though private room details may be limited based on user permissions.
   * </p>
   * <p><b>Note:</b> Even when including private rooms, detailed information may
   * be restricted to room participants and administrators.</p>
   */
  private Boolean isPublic;

  /**
   * Filters rooms by maximum participant capacity.
   * <p>
   * Returns only rooms where the maximum number of participants is less than
   * or equal to this value. Useful for users seeking smaller, more intimate
   * activities or specific group sizes.
   * </p>
   * <p><b>Example:</b> {@code 10} returns rooms that allow 10 or fewer participants</p>
   */
  @Positive
  @Max(100000)
  private Integer maxParticipants;

  /**
   * Filters rooms by city location.
   * <p>
   * When specified, returns only rooms associated with this city. Matching is
   * case-insensitive and supports partial city names. Particularly useful for
   * finding local activities and in-person meetings.
   * </p>
   * <p><b>Example:</b> {@code "Moscow"} matches rooms in Moscow, regardless of case</p>
   */
  @Size(max = 100)
  private String city;

  /**
   * Filters rooms by country location.
   * <p>
   * When specified, returns only rooms associated with this country. Useful
   * for finding regional activities or filtering by specific countries for
   * time zone or language considerations.
   * </p>
   * <p><b>Example:</b> {@code "Russia"} matches rooms located in Russia</p>
   */
  @Size(max = 100)
  private String country;

  // Business logic methods with clear documentation

  /**
   * Determines whether this filter contains any active filtering criteria.
   *
   * @return {@code true} if no filters are specified (all fields are {@code null}),
   *         {@code false} if at least one filter criterion is active
   */
  public boolean isEmpty() {
    return category == null && query == null && isPublic == null
        && maxParticipants == null && city == null && country == null;
  }

  /**
   * Checks if this filter includes location-based criteria.
   *
   * @return {@code true} if either city or country filtering is active,
   *         {@code false} if no location filters are specified
   */
  public boolean hasLocationFilter() {
    return city != null || country != null;
  }

  /**
   * Checks if this filter includes text search criteria.
   *
   * @return {@code true} if a search query is specified and not empty,
   *         {@code false} if no text search is active
   */
  public boolean hasTextFilter() {
    return query != null && !query.trim().isEmpty();
  }
}