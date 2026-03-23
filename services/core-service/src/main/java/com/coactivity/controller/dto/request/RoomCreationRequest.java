package com.coactivity.controller.dto.request;

import java.time.Instant;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomCreationRequest {

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

  @NotNull
  @Min(2)
  @Max(100000)
  private Integer maximumNumberOfPeople;

  @Size(max = 255)
  private String chatLink;

  @FutureOrPresent
  private Instant dateOfStartEvent;

  @Future
  private Instant dateOfEndEvent;

  private Instant frequency;

  @Min(0)
  @Max(21)
  private int ageRating;
}
