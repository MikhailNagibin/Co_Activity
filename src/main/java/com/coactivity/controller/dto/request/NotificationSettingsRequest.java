package com.coactivity.controller.dto.request;

import lombok.Data;

/**
 * Request DTO for updating user notification preferences and settings.
 * <p>
 * Allows users to customize their notification experience by enabling or disabling
 * specific types of platform notifications. This request is used to update
 * user preferences and is processed by the notification settings configuration endpoint.
 * </p>
 *
 * <p><b>Usage Notes:</b>
 * <ul>
 *   <li>All fields are optional - only provided fields will be updated</li>
 *   <li>Null values are ignored during the update process</li>
 *   <li>Existing preferences are preserved for fields not included in the request</li>
 *   <li>Settings apply globally across all rooms and activities</li>
 * </ul>
 * </p>
 *
 * @author CoActivity Development Team
 * @version 1.0
 * @see com.coactivity.controller.UserController#configureNotificationSettings(String, NotificationSettingsRequest)
 * @see com.coactivity.controller.dto.response.NotificationSettingsResponse
 * @since 2024
 */
@Data
public class NotificationSettingsRequest {

  /**
   * Preference for receiving notifications when membership requests are accepted.
   * <p>
   * When set to {@code true}, the user will receive notifications when their
   * requests to join rooms are approved by room administrators. When {@code false},
   * these notifications are disabled. If {@code null}, the current setting is preserved.
   * </p>
   *
   * @example true
   */
  private Boolean membershipAccepted;

  /**
   * Preference for receiving notifications when membership requests are rejected.
   * <p>
   * When set to {@code true}, the user will receive notifications when their
   * requests to join rooms are denied by room administrators. When {@code false},
   * these notifications are disabled. If {@code null}, the current setting is preserved.
   * </p>
   *
   * @example false
   */
  private Boolean membershipRejected;

  /**
   * Preference for receiving notifications when participated activities are closed.
   * <p>
   * When set to {@code true}, the user will receive notifications when rooms
   * they participate in are permanently closed or ended. When {@code false},
   * these notifications are disabled. If {@code null}, the current setting is preserved.
   * </p>
   *
   * @example true
   */
  private Boolean activityClosed;

  /**
   * Preference for receiving notifications for new membership requests.
   * <p>
   * When set to {@code true}, room administrators will receive notifications
   * when new users request to join their rooms. When {@code false}, these
   * notifications are disabled. If {@code null}, the current setting is preserved.
   * This setting primarily affects users with administrative privileges.
   * </p>
   *
   * @example true
   */
  private Boolean newJoinRequest;
}