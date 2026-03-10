package com.coactivity.controller.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the bulletin board content for a room with author and metadata information.
 * <p>
 * This response DTO provides a secure representation of bulletin board data that can be safely
 * exposed to authorized room participants. It includes the bulletin content along with authorship
 * and timing information for proper attribution and context.
 * </p>
 *
 * <p><b>Security Note:</b> This information is only available to room participants.
 * Unauthorized users receive {@code null} for the entire bulletin board field in
 * {@link RoomDetailedResponse} to prevent unauthorized access to internal room communications.</p>
 *
 * @see RoomDetailedResponse
 * @see UserSummaryResponse
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulletinBoardResponse {

  /**
   * Unique identifier for the bulletin board entry.
   * <p>
   * This ID corresponds to the primary key in the bulletin_boards database table and can be used
   * for update operations or audit trail purposes.
   * </p>
   */
  private int id;

  /**
   * The textual content of the bulletin board announcement.
   * <p>
   * Contains the actual message, announcement, or information that room administrators wish to
   * communicate to all room participants. This field supports multi-line text and standard markdown
   * formatting for rich content presentation.
   * </p>
   */
  private String content;

  /**
   * Summary information about the user who last updated the bulletin board.
   * <p>
   * Provides attribution for the bulletin board content while maintaining privacy by exposing only
   * non-sensitive user information. This field helps participants identify the source and authority
   * of the announcement.
   * </p>
   *
   * @see UserSummaryResponse
   */
  private UserSummaryResponse author;

  /**
   * The timestamp when this bulletin board entry was last updated.
   * <p>
   * Uses {@link Instant} for timezone-agnostic timestamp storage, ensuring consistent display
   * across all user locations. This field helps participants gauge the recency and relevance of the
   * announcement.
   * </p>
   */
  private Instant updatedAt;
}