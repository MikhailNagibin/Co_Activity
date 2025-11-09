package com.coactivity.controller.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * Comprehensive room information with conditional data exposure.
 * <p>
 * Formally, it is an extension of class {@link RoomSummaryResponse}
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class RoomDetailedResponse extends RoomSummaryResponse {

  /**
   * Indicates whether the current user can see protected content.
   */
  private Boolean hasProtectedAccess;

  /**
   * Direct link to the room's chat/communication platform. Null for unauthorized users.
   */
  private String chatLink;

  /**
   * The current bulletin board content. Null for unauthorized users.
   */
  private BulletinBoardResponse bulletinBoard;
}