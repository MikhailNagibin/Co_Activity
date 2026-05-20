package com.coactivity.service;

import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.repository.UserFollowRepository;
import com.coactivity.repository.UserRepository;
import com.coactivity.service.exception.ConflictException;
import com.coactivity.service.exception.ResourceNotFoundException;
import com.coactivity.service.exception.ValidationException;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserFollowService {

  private final UserRepository userRepository;
  private final UserFollowRepository userFollowRepository;

  public UserFollowService(UserRepository userRepository, UserFollowRepository userFollowRepository) {
    this.userRepository = userRepository;
    this.userFollowRepository = userFollowRepository;
  }

  @Transactional
  public void followUser(Integer followerId, Integer followedId) {
    Integer effectiveFollowerId = requireUserId(followerId, "Follower user id is required");
    Integer effectiveFollowedId = requireUserId(followedId, "Followed user id is required");
    if (Objects.equals(effectiveFollowerId, effectiveFollowedId)) {
      throw new ValidationException("User cannot follow itself");
    }

    ensureUserExists(effectiveFollowerId);
    ensureUserExists(effectiveFollowedId);

    if (userFollowRepository.existsFollow(effectiveFollowerId, effectiveFollowedId)) {
      throw new ConflictException("ALREADY_FOLLOWING", "Already following this user");
    }

    try {
      userFollowRepository.createFollow(effectiveFollowerId, effectiveFollowedId);
    } catch (RuntimeException exception) {
      if (userFollowRepository.existsFollow(effectiveFollowerId, effectiveFollowedId)) {
        throw new ConflictException("ALREADY_FOLLOWING", "Already following this user", exception);
      }
      throw exception;
    }
  }

  @Transactional
  public void unfollowUser(Integer followerId, Integer followedId) {
    Integer effectiveFollowerId = requireUserId(followerId, "Follower user id is required");
    Integer effectiveFollowedId = requireUserId(followedId, "Followed user id is required");

    ensureUserExists(effectiveFollowerId);
    ensureUserExists(effectiveFollowedId);

    boolean deleted = userFollowRepository.deleteFollow(effectiveFollowerId, effectiveFollowedId);
    if (!deleted) {
      throw new ConflictException("NOT_FOLLOWING", "Not following this user");
    }
  }

  @Transactional(readOnly = true)
  public List<UserSummaryResponse> getFollowingUsers(Integer followerId) {
    Integer effectiveFollowerId = requireUserId(followerId, "Follower user id is required");
    ensureUserExists(effectiveFollowerId);
    return userFollowRepository.getFollowingUsers(effectiveFollowerId);
  }

  @Transactional(readOnly = true)
  public List<UserSummaryResponse> getFollowerUsers(Integer followedId) {
    Integer effectiveFollowedId = requireUserId(followedId, "Followed user id is required");
    ensureUserExists(effectiveFollowedId);
    return userFollowRepository.getFollowerUsers(effectiveFollowedId);
  }

  private Integer requireUserId(Integer userId, String message) {
    if (userId == null) {
      throw new ValidationException(message);
    }
    return userId;
  }

  private void ensureUserExists(Integer userId) {
    if (userRepository.getUserById(userId) == null) {
      throw new ResourceNotFoundException("USER_NOT_FOUND", "User not found");
    }
  }
}
