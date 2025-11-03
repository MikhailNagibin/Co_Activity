package com.coactivity.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Answer {

  private int id;
  private Question question;
  private Answer previousAnswer;
  private String answer;
  private User owner;
}
