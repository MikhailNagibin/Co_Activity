package com.coactivity.controller.dto.response;

import com.coactivity.domain.Category;
import com.coactivity.domain.RoomStatus;
import java.time.Instant;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RoomSummaryResponse {

  private Integer id;

  private boolean isActive;

  private RoomStatus status;

  private Boolean isPublic;

  private Category category;

  private String name;

  private String description;

  private String city;

  private String country;

  private Instant dateOfStartEvent;

  private Instant dateOfEndEvent;

  private int ageRating;

  private Instant frequency;

  private Integer participantCount;

  private Integer maximumParticipants;

  private UserSummaryResponse creator;

  private Boolean isCurrentUserParticipant;

  private RoomMembershipStatusResponse membershipStatus;

  private List<Integer> imageIds;

  private List<RoomImageResponse> images;

  public RoomSummaryResponse(Integer id, boolean isActive, Boolean isPublic, Category category,
      String name, String description, Instant dateOfStartEvent, Instant dateOfEndEvent,
      int ageRating, Instant frequency, Integer participantCount, Integer maximumParticipants,
      UserSummaryResponse creator, Boolean isCurrentUserParticipant, List<Integer> imageIds) {
    this(id, isActive ? RoomStatus.ACTIVE : RoomStatus.INACTIVE, isPublic, category, name,
        description, dateOfStartEvent, dateOfEndEvent,
        ageRating, frequency, participantCount, maximumParticipants, creator,
        isCurrentUserParticipant, imageIds, List.of());
  }

  public RoomSummaryResponse(Integer id, RoomStatus status, Boolean isPublic, Category category,
      String name, String description, Instant dateOfStartEvent, Instant dateOfEndEvent,
      int ageRating, Instant frequency, Integer participantCount, Integer maximumParticipants,
      UserSummaryResponse creator, Boolean isCurrentUserParticipant, List<Integer> imageIds,
      List<RoomImageResponse> images) {
    this.id = id;
    setStatus(status);
    this.isPublic = isPublic;
    this.category = category;
    this.name = name;
    this.description = description;
    this.dateOfStartEvent = dateOfStartEvent;
    this.dateOfEndEvent = dateOfEndEvent;
    this.ageRating = ageRating;
    this.frequency = frequency;
    this.participantCount = participantCount;
    this.maximumParticipants = maximumParticipants;
    this.creator = creator;
    this.isCurrentUserParticipant = isCurrentUserParticipant;
    this.imageIds = imageIds;
    this.images = images;
  }

  public void setStatus(RoomStatus status) {
    this.status = status;
    this.isActive = status == RoomStatus.ACTIVE;
  }

  public void setActive(boolean active) {
    this.isActive = active;
    if (status == null) {
      this.status = active ? RoomStatus.ACTIVE : RoomStatus.INACTIVE;
    }
  }
}
