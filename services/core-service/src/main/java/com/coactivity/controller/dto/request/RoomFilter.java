package com.coactivity.controller.dto.request;

import com.coactivity.domain.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Фильтры поиска комнат (query параметры).")
public class RoomFilter {

  @Schema(description = "Категория комнаты.", example = "SPORT")
  private Category category;

  @Size(max = 255)
  @Schema(description = "Текстовый поиск по названию/описанию.", example = "теннис")
  private String query;

  @Schema(description = "Фильтр по публичности комнаты.", example = "true")
  private Boolean isPublic;

  @Positive
  @Max(100000)
  @Schema(description = "Максимальное число участников.", example = "50")
  private Integer maxParticipants;

  @Size(max = 100)
  @Schema(description = "Город.", example = "Москва")
  private String city;

  @Size(max = 100)
  @Schema(description = "Страна.", example = "Россия")
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
