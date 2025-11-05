package com.coactivity.domain;

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
}
