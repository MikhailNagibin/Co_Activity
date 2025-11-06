package com.coactivity.dto.request;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for updating user profile information.
 * <p>
 * Supports partial updates - only provided fields are modified, while unspecified fields retain
 * their current values. The login email is immutable and cannot be changed after registration.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {

  /**
   * New public display name for the user.
   * <p>
   * If provided, must be between 2 and 50 characters and unique across the system. If {@code null},
   * the current username is preserved.
   * </p>
   */
  private String username;

  /**
   * Updated date of birth for age verification and personalization.
   * <p>
   * If provided, must be a valid past date and the user must be at least 13 years old. If
   * {@code null}, the current date of birth is preserved.
   * </p>
   */
  private Instant dateOfBirth;

  /**
   * Updated city of residence for location-based features.
   * <p>
   * If provided, maximum 100 characters. If {@code null}, the current city is preserved.
   * </p>
   */
  private String city;

  /**
   * Updated country of residence for regional content.
   * <p>
   * If provided, maximum 100 characters. If {@code null}, the current country is preserved.
   * </p>
   */
  private String country;

  /**
   * Updated biographical information about the user.
   * <p>
   * If provided, maximum 500 characters. If {@code null}, the current description is preserved.
   * Empty string can be used to clear the description.
   * </p>
   */
  private String description;

  /**
   * Updated profile picture identifier.
   * <p>
   * If provided, must reference an existing image in the system's media storage. If {@code null},
   * the current avatar is preserved.
   * </p>
   */
  private Integer avatarId;
}