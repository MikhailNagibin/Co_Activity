// src/main/java/com/coactivity/dto/response/RoomDetailedResponse.java
package com.coactivity.dto.response;

import com.coactivity.domain.enums.Category;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;

/**
 * Comprehensive room information with conditional data exposure.
 */
@Data
@Builder
public class RoomDetailedResponse {

  private Integer id;
  private String name;
  private Category category;
  private String description;
  private Boolean isPublic;
  private Integer participantCount;
  private Integer maximumParticipants;
  private String creatorName;

  /**
   * Direct link to the room's chat/communication platform. Null for unauthorized users.
   */
  private String chatLink;

  /**
   * The current bulletin board text content. Null for unauthorized users.
   */
  private String bulletinBoard;

  /**
   * When it was last updated
   */
  private Instant bulletinBoardUpdatedAt;

  /**
   * Indicates whether the current user can see protected content.
   */
  private Boolean hasProtectedAccess;
}