package com.coactivity.repository;

import com.coactivity.domain.UserAvatar;

public interface UserAvatarRepository {

  UserAvatar createAvatar(String storageKey, String originalFilename, String contentType,
      long sizeBytes);

  UserAvatar getAvatarById(Integer avatarId);

  void deleteAvatar(Integer avatarId);
}
