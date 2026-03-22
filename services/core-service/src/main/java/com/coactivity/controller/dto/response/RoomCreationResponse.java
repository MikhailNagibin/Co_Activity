package com.coactivity.controller.dto.response;

import com.coactivity.domain.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomCreationResponse {

  private Integer roomId;

  private String name;

  private Category category;
}
