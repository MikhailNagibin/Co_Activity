package com.coactivity.repository.impl;

import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.persistence.entity.UserEntity;
import com.coactivity.persistence.entity.UserFollowEntity;
import com.coactivity.persistence.entity.UserFollowId;
import com.coactivity.persistence.repository.UserFollowJpaRepository;
import com.coactivity.persistence.repository.UserJpaRepository;
import com.coactivity.repository.UserFollowRepository;
import com.coactivity.util.AvatarUrlResolver;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class UserFollowRepositoryImpl implements UserFollowRepository {

  private final UserFollowJpaRepository userFollowJpaRepository;
  private final UserJpaRepository userJpaRepository;

  public UserFollowRepositoryImpl(UserFollowJpaRepository userFollowJpaRepository,
      UserJpaRepository userJpaRepository) {
    this.userFollowJpaRepository = userFollowJpaRepository;
    this.userJpaRepository = userJpaRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsFollow(Integer followerId, Integer followedId) {
    return userFollowJpaRepository.existsByFollower_IdAndFollowed_Id(followerId, followedId);
  }

  @Override
  public void createFollow(Integer followerId, Integer followedId) {
    UserFollowEntity entity = new UserFollowEntity();
    entity.setId(new UserFollowId(followerId, followedId));
    entity.setFollower(userJpaRepository.getReferenceById(followerId));
    entity.setFollowed(userJpaRepository.getReferenceById(followedId));
    entity.setCreatedAt(Instant.now());
    userFollowJpaRepository.saveAndFlush(entity);
  }

  @Override
  public boolean deleteFollow(Integer followerId, Integer followedId) {
    return userFollowJpaRepository.deleteByFollower_IdAndFollowed_Id(followerId, followedId) > 0;
  }

  @Override
  @Transactional(readOnly = true)
  public long countFollowers(Integer userId) {
    return userFollowJpaRepository.countByFollowed_Id(userId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserSummaryResponse> getFollowingUsers(Integer followerId) {
    return userFollowJpaRepository.findAllByFollower_IdOrderByCreatedAtDescFollowed_IdDesc(followerId)
        .stream()
        .map(UserFollowEntity::getFollowed)
        .map(this::mapUserToSummary)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserSummaryResponse> getFollowerUsers(Integer followedId) {
    return userFollowJpaRepository.findAllByFollowed_IdOrderByCreatedAtDescFollower_IdDesc(followedId)
        .stream()
        .map(UserFollowEntity::getFollower)
        .map(this::mapUserToSummary)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> getFollowerEmails(Integer followedUserId) {
    return userFollowJpaRepository.findFollowerEmailsByFollowedId(followedUserId);
  }

  private UserSummaryResponse mapUserToSummary(UserEntity user) {
    UserSummaryResponse summary = new UserSummaryResponse();
    summary.setId(user.getId());
    summary.setUserName(user.getUserName());
    summary.setDateOfBirth(user.getDataOfBirth());
    summary.setCity(user.getCity());
    summary.setCountry(user.getCountry());
    summary.setDescription(user.getDescription());
    summary.setAvatarId(user.getAvatarId());
    summary.setAvatarUrl(
        AvatarUrlResolver.resolveUserAvatarUrl(
            user.getId(),
            user.getAvatarFile() != null ? user.getAvatarFile().getId() : null));
    return summary;
  }
}
