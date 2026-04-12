package com.coactivity.controller.dto.request;

import com.coactivity.domain.RoomStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomUpdateRequest {

  @NotNull
  private Boolean isPublic;

  @NotNull
  private String category;

  @NotBlank
  @Size(min = 3, max = 100)
  private String name;

  @NotBlank
  @Size(max = 2000)
  private String description;

  @Size(max = 100)
  private String city;

  @Size(max = 100)
  private String country;

  @NotNull
  @Min(2)
  @Max(100000)
  private Integer maximumNumberOfPeople;

  @Size(max = 255)
  private String chatLink;

  private Instant dateOfStartEvent;

  private Instant dateOfEndEvent;

  private Instant frequency;

  @NotNull
  private RoomStatus status;

  @NotNull
  @Min(0)
  @Max(21)
  private Integer ageRating;
}
