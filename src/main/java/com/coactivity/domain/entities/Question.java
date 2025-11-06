package com.coactivity.domain.entities;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Question {

  private int id;
  private int ownerId;
  private String question;
  private int categoryId;
  private Instant createdAt;
}
