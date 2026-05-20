package com.coactivity.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomImageResponse {

  private Integer id;

  private String url;

  private Integer order;
}
