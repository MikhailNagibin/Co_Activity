package com.coactivity.domain;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserAvatar {

  private Integer id;
  private String storageKey;
  private String originalFilename;
  private String contentType;
  private long sizeBytes;
  private Instant createdAt;
}
