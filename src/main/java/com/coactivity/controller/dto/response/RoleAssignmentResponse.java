package com.coactivity.controller.dto.response;

import com.coactivity.domain.Role;
import lombok.Builder;
import lombok.Data;

/**
 * Represents the outcome of a role change operation within a room.
 * <p>
 * This response DTO provides complete audit information for role transitions, including both
 * promotions and demotions. It tracks the user, room, roles before and after the change, and who
 * authorized the operation for full accountability and audit trail purposes.
 * </p>
 *
 * <p><b>Usage Context:</b> Returned by both {@code assignAdminRole} and {@code demoteAdminRole}
 * operations to provide consistent feedback about role changes within room management.</p>
 *
 * @see com.coactivity.controller.UserController#assignAdminRole(String, Integer, Integer)
 * @see com.coactivity.controller.UserController#demoteAdminRole(String, Integer, Integer)
 * @see Role
 */
@Data
@Builder
public class RoleAssignmentResponse {

  /**
   * Unique identifier of the user whose role was changed.
   * <p>
   * Identifies the participant who was promoted or demoted in the room hierarchy. This ID can be
   * used to fetch additional user details if needed.
   * </p>
   */
  private Integer userId;

  /**
   * Unique identifier of the room where the role change occurred.
   * <p>
   * Provides context for the role assignment operation and allows clients to correlate the change
   * with specific room management activities.
   * </p>
   */
  private Integer roomId;

  /**
   * The role assigned to the user after the operation.
   * <p>
   * Represents the user's new position in the room hierarchy. For promotions, this will be
   * {@link Role#ADMIN}; for demotions, this will be {@link Role#PARTICIPANT}.
   * </p>
   */
  private Role newRole;

  /**
   * The role held by the user before the operation.
   * <p>
   * Provides historical context for the role transition and helps track the progression of user
   * responsibilities within the room.
   * </p>
   */
  private Role previousRole;

  /**
   * Unique identifier of the user who performed the role assignment.
   * <p>
   * Tracks accountability for role changes within the system. Typically, this will be the room
   * owner who authorized the promotion or demotion.
   * </p>
   */
  private Integer assignedBy;
}