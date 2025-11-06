package com.coactivity.dto.request;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for user registration operations.
 * <p>
 * Contains all necessary information to create a new user account in the system. All fields undergo
 * validation to ensure data quality and security requirements.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationRequest {

  /**
   * User's email address used as the primary login identifier.
   * <p>
   * Must be a valid email format and unique across the system. This field is immutable after
   * registration.
   * </p>
   */
  private String login;

  /**
   * Public display name visible to other users.
   * <p>
   * Must be between 2 and 50 characters and can contain letters, numbers, and common punctuation
   * marks. Usernames must be unique.
   * </p>
   */
  private String username;

  /**
   * Account password for authentication.
   * <p>
   * Must be at least 8 characters long and contain a mix of character types. Passwords are hashed
   * using secure algorithms before storage.
   * </p>
   */
  private String password;

  /**
   * User's date of birth for age verification and personalization.
   * <p>
   * Must be a valid past date. Used for age-appropriate content filtering and legal compliance.
   * </p>
   */
  private Instant dateOfBirth;

  /**
   * City where the user resides.
   * <p>
   * Used for location-based features, local activity recommendations, and community building.
   * Maximum 100 characters.
   * </p>
   */
  private String city;

  /**
   * Country where the user resides.
   * <p>
   * Used for regional content, language preferences, and compliance with local regulations. Maximum
   * 100 characters.
   * </p>
   */
  private String country;

  /**
   * Optional biographical information about the user.
   * <p>
   * Allows users to share their interests, background, or anything they'd like the community to
   * know. Maximum 500 characters.
   * </p>
   */
  private String description;

  /**
   * Identifier for the user's profile picture.
   * <p>
   * References an image previously uploaded to the system's media storage. If {@code null}, a
   * default avatar will be assigned automatically.
   * </p>
   */
  private Integer avatarId;
}