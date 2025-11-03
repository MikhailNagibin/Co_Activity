package com.coactivity.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Question {

  private int id;
  private User owner;
  private String question;
}
