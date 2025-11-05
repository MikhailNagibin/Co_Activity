package com.coactivity.domain;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BulletinBoard {

  private int id;
  private int roomId;
  private String content;
  private int authorId;
  private Instant updatedAt;
}

