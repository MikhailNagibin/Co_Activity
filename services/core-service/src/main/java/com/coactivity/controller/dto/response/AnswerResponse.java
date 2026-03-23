package com.coactivity.controller.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerResponse {

  private Integer id;

  private Integer questionId;

  private Integer previousAnswerId;

  private String answer;

  private UserSummaryResponse author;

  private Instant createdAt;

  private List<AnswerResponse> replies;
}