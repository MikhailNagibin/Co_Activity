package com.coactivity.domain;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Answer {

  private Integer id;
  private Integer questionId;
  private Integer previousAnswerId;
  private String answer;
  private User ownerId;
  private Instant createdAt;
}
