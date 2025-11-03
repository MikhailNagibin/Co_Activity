package com.coactivity.domain;

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
