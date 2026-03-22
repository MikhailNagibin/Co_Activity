package com.coactivity.controller.dto.request;

import com.coactivity.domain.Category;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomFilter {

  private Category category;

  @Size(max = 255)
  private String query;

  private Boolean isPublic;

  @Positive
  @Max(100000)
  private Integer maxParticipants;

  @Size(max = 100)
  private String city;

  @Size(max = 100)
  private String country;

  public boolean isEmpty() {
    return category == null && query == null && isPublic == null
        && maxParticipants == null && city == null && country == null;
  }

  public boolean hasLocationFilter() {
    return city != null || country != null;
  }

  public boolean hasTextFilter() {
    return query != null && !query.trim().isEmpty();
  }
}
