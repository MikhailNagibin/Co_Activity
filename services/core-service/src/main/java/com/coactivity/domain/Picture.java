package com.coactivity.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
public class Picture {
  private Room room;
  private Integer photoId;
  private String storageKey;
  private String originalFilename;
  private String contentType;
  private Long sizeBytes;
  private Integer sortOrder;
  private Instant createdAt;
}
