package com.coactivity.controller.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulletinBoardResponse {

  private int id;

  private String content;

  private UserSummaryResponse author;

  private Instant updatedAt;
}