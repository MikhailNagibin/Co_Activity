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
    return category == null
        && !hasTextFilter()
        && isPublic == null
        && maxParticipants == null
        && !hasLocationFilter();
  }

  public boolean hasLocationFilter() {
    return hasValue(city) || hasValue(country);
  }

  public boolean hasTextFilter() {
    return hasValue(query);
  }

  private boolean hasValue(String value) {
    return value != null && !value.trim().isEmpty();
  }
}
