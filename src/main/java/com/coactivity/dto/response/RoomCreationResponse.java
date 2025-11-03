package com.coactivity.dto.response;

import com.coactivity.domain.enums.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response payload returned after creating a new room or activity.
 * <p>
 * Provides confirmation of successful room creation and the essential information needed to
 * redirect the user to the new room or display creation confirmation.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomCreationResponse {

  /**
   * Unique identifier assigned to the newly created room.
   * <p>
   * This ID must be used by the client for all future operations related to this specific room
   * (joining, viewing, updating, etc.).
   * </p>
   */
  private Integer roomId;

  /**
   * Name of the newly created room.
   * <p>
   * Provided as confirmation that the room was created with the intended name. Can be displayed in
   * success messages or UI feedback.
   * </p>
   */
  private String name;

  /**
   * Categorization of the room's activity type.
   * <p>
   * Determines how the room is grouped and filtered in search results and category-based
   * navigation.
   * </p>
   */
  private Category category;
}
