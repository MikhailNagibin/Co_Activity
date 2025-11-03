package com.coactivity.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Complete user profile information for viewing and editing.
 * <p>
 * Contains all publicly accessible user data without exposing sensitive information like passwords
 * or internal system fields.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

  /**
   * Unique identifier for the user.
   */
  private Integer id;

  /**
   * User's email address used for authentication.
   * <p>
   * This field is immutable and cannot be changed after registration. It serves as the user's
   * unique login identifier.
   * </p>
   */
  private String email;

  /**
   * Public display name visible to other users.
   * <p>
   * This name appears in room participant lists, question author fields, and throughout the
   * application where user identity is shown.
   * </p>
   */
  private String username;

  /**
   * User's date of birth.
   * <p>
   * Used for age verification and personalized user experience. May affect access to age-restricted
   * rooms and activities.
   * </p>
   */
  private Instant dateOfBirth;

  /**
   * City where the user resides.
   */
  private String city;

  /**
   * Country where the user resides.
   */
  private String country;

  /**
   * User-provided biographical information.
   * <p>
   * Optional field where users can describe their interests, background, or anything they'd like to
   * share with other community members.
   * </p>
   */
  private String description;

  /**
   * Identifier for the user's profile picture.
   * <p>
   * References an image stored in the system's media storage. A value of {@code null} indicates no
   * profile picture is set.
   * </p>
   */
  private Integer avatarId;
}