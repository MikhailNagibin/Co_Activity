package com.coactivity.repository.impl;

import com.coactivity.domain.UserAvatar;
import com.coactivity.persistence.entity.UserAvatarEntity;
import com.coactivity.persistence.repository.UserAvatarJpaRepository;
import com.coactivity.repository.UserAvatarRepository;
import java.time.Instant;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class UserAvatarRepositoryImpl implements UserAvatarRepository {

  private final UserAvatarJpaRepository userAvatarJpaRepository;

  public UserAvatarRepositoryImpl(UserAvatarJpaRepository userAvatarJpaRepository) {
    this.userAvatarJpaRepository = userAvatarJpaRepository;
  }

  @Override
  public UserAvatar createAvatar(String storageKey, String originalFilename, String contentType,
      long sizeBytes) {
    UserAvatarEntity entity = new UserAvatarEntity();
    entity.setStorageKey(storageKey);
    entity.setOriginalFilename(originalFilename);
    entity.setContentType(contentType);
    entity.setSizeBytes(sizeBytes);
    entity.setCreatedAt(Instant.now());
    return toDomain(userAvatarJpaRepository.saveAndFlush(entity));
  }

  @Override
  @Transactional(readOnly = true)
  public UserAvatar getAvatarById(Integer avatarId) {
    return userAvatarJpaRepository.findById(avatarId)
        .map(this::toDomain)
        .orElse(null);
  }

  @Override
  public void deleteAvatar(Integer avatarId) {
    userAvatarJpaRepository.deleteById(avatarId);
  }

  private UserAvatar toDomain(UserAvatarEntity entity) {
    if (entity == null) {
      return null;
    }
    return new UserAvatar(
        entity.getId(),
        entity.getStorageKey(),
        entity.getOriginalFilename(),
        entity.getContentType(),
        entity.getSizeBytes(),
        entity.getCreatedAt());
  }
}
