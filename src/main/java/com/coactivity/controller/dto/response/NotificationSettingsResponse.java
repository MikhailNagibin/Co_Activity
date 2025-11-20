package com.coactivity.controller.dto.response;

import com.coactivity.controller.dto.request.NotificationSettingsRequest;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a user's current notification preferences and settings.
 * <p>
 * This response DTO provides a complete view of the user's chosen notification preferences across
 * all notification types supported by the platform. It is used both to display current settings and
 * to confirm updates after preference changes.
 * </p>
 *
 * <p><b>Notification Types:</b>
 * <ul>
 *   <li>{@code membershipAccepted} - Notifications when join requests are approved</li>
 *   <li>{@code membershipRejected} - Notifications when join requests are denied</li>
 *   <li>{@code activityClosed} - Notifications when participated activities end</li>
 *   <li>{@code newJoinRequest} - Notifications for new membership requests (admins only)</li>
 * </ul>
 * </p>
 *
 * @see com.coactivity.controller.UserController#configureNotificationSettings(String,
 * NotificationSettingsRequest)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingsResponse {

  /**
   * Whether to receive notifications when membership requests are accepted.
   * <p>
   * When {@code true}, users receive notifications when their requests to join rooms are approved
   * by room administrators. This helps users stay informed about their membership status changes.
   * </p>
   */
  private Boolean membershipAccepted;

  /**
   * Whether to receive notifications when membership requests are rejected.
   * <p>
   * When {@code true}, users receive notifications when their requests to join rooms are denied by
   * room administrators. This provides closure and allows users to explore alternative activities.
   * </p>
   */
  private Boolean membershipRejected;

  /**
   * Whether to receive notifications when participated activities are closed.
   * <p>
   * When {@code true}, users receive notifications when rooms they participate in are permanently
   * closed or ended. This helps users stay informed about the status of their current activities.
   * </p>
   */
  private Boolean activityClosed;

  /**
   * Whether to receive notifications for new membership requests.
   * <p>
   * When {@code true}, room administrators receive notifications when new users request to join
   * their rooms. This setting is typically only relevant for users with administrative privileges
   * in one or more rooms.
   * </p>
   */
  private Boolean newJoinRequest;

  /**
   * The timestamp when these notification settings were last updated.
   * <p>
   * Tracks when the user last modified their notification preferences, providing context for
   * preference changes and supporting synchronization across devices.
   * </p>
   */
  private Instant updatedAt;
}