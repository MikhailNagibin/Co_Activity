package com.coactivity.domain;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Answer {

  private int id;
  private int questionId;
  private int previousAnswerId;
  private String answer;
  private User ownerId;
  private Instant createdAt;
}
