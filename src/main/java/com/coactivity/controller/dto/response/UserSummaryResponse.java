package com.coactivity.controller.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Secure summary representation of user information for public API responses.
 * <p>
 * Provides essential user profile data suitable for public display while excluding sensitive
 * information such as email addresses, passwords, and internal system fields. This DTO is used
 * throughout the API wherever user information needs to be exposed to other users, ensuring
 * consistent and secure data exposure.
 * </p>
 *
 * <p><b>Security Implementation:</b> This class deliberately omits sensitive user data
 * to prevent information leakage. It is the primary mechanism for sharing user information between
 * different users of the platform.</p>
 *
 * @see BulletinBoardResponse
 * @see RoomSummaryResponse
 * @see RoomDetailedResponse
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {

  /**
   * Unique identifier for the user account.
   * <p>
   * This immutable ID is the primary key for user records and can be used for user-specific API
   * operations where the user's identity is required.
   * </p>
   */
  private Integer id;

  /**
   * Public display name chosen by the user.
   * <p>
   * This is the name that will be visible to other users throughout the platform. It is typically
   * the user's preferred name, nickname, or real name as they choose to represent themselves
   * publicly.
   * </p>
   */
  private String name;

  /**
   * The user's date of birth for age verification and personalization.
   * <p>
   * Used to enforce age-based content restrictions and to calculate user age for age-appropriate
   * activity recommendations. The time portion is typically set to midnight UTC for privacy
   * consistency.
   * </p>
   */
  private Instant dateOfBirth;

  /**
   * The city where the user is currently located.
   * <p>
   * Used for location-based activity recommendations and to help users find local events and
   * communities. Users have full control over this information and can choose not to disclose it.
   * </p>
   */
  private String city;

  /**
   * The country where the user is currently located.
   * <p>
   * Provides broader geographical context for location-based features and content localization.
   * Combined with city information to enable precise geographical filtering and recommendations.
   * </p>
   */
  private String country;

  /**
   * Personal description or bio provided by the user.
   * <p>
   * A free-form text field where users can share information about their interests, background, or
   * anything they'd like other community members to know. Supports multi-line text and basic
   * formatting.
   * </p>
   */
  private String description;

  /**
   * Identifier for the user's profile avatar image.
   * <p>
   * References the stored avatar image in the platform's media storage system. Clients should use
   * this ID to construct avatar image URLs according to the platform's media serving pattern.
   * </p>
   */
  private int avatarId;
}