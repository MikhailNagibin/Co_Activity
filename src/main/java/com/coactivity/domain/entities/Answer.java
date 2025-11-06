package com.coactivity.domain.entities;

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
  private int ownerId;
  private Instant createdAt;
}
