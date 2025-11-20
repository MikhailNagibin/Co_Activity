package com.coactivity.controller.dto.response;

import com.coactivity.domain.Role;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a participant in a room with their role and basic user information.
 * <p>
 * Used for room administration views where both user details and participation context are required
 * for effective management.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomParticipantResponse {

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
   * User's profile picture identifier.
   */
  private Integer avatarId;

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
   * User's role within this specific room.
   */
  private Role role;
}