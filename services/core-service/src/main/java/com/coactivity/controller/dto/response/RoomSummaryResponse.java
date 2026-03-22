package com.coactivity.controller.dto.response;

import com.coactivity.domain.Category;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomSummaryResponse {

  private Integer id;

  private boolean isActive;

  private Boolean isPublic;

  private Category category;

  private String name;

  private String description;

  private Instant dateOfStartEvent;

  private Instant dateOfEndEvent;

  private int ageRating;

  private Instant frequency;

  private Integer participantCount;

  private Integer maximumParticipants;

  private UserSummaryResponse creator;

  private Boolean isCurrentUserParticipant;

  private List<Integer> imageIds;
}
