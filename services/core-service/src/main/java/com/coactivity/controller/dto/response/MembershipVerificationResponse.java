package com.coactivity.controller.dto.response;

import com.coactivity.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the result of verifying a user's membership status in a specific room.
 * <p>
 * This response DTO provides comprehensive membership verification information, including whether
 * the user is a member, their role if they are a member, and contextual information about the user
 * and room for display purposes.
 * </p>
 *
 * <p><b>Primary Use Cases:</b>
 * <ul>
 *   <li>Administrative membership checks for room management</li>
 *   <li>Integration with external systems requiring membership validation</li>
 * </ul>
 * </p>
 *
 * @see com.coactivity.controller.RoomController#isUserInRoom(String, Integer, Integer)
 * @see Role
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MembershipVerificationResponse {

  /**
   * Indicates whether the verified user is a current participant in the specified room.
   * <p>
   * {@code true} if the user has an active membership in the room, {@code false} if they are not a
   * participant or if their membership has been revoked, suspended, or has otherwise ended.
   * </p>
   */
  private Boolean isMember;

  /**
   * The user's current role within the room, if they are a member.
   * <p>
   * Only populated when {@code isMember} is {@code true}. Indicates the user's level of authority
   * and responsibilities within the room hierarchy.
   * </p>
   *
   * @see Role
   */
  private Role role;

  /**
   * Basic user information for identification purposes.
   */
  private UserSummaryResponse userInfo;

  /**
   * Official name of the room being verified against.
   * <p>
   * Provides context for the membership verification and helps users and administrators understand
   * which specific activity or room is being referenced.
   * </p>
   */
  private String roomName;
}