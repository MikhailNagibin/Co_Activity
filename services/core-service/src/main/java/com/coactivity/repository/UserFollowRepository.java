package com.coactivity.repository;

import com.coactivity.controller.dto.response.UserSummaryResponse;
import java.util.List;

public interface UserFollowRepository {

  boolean existsFollow(Integer followerId, Integer followedId);

  void createFollow(Integer followerId, Integer followedId);

  boolean deleteFollow(Integer followerId, Integer followedId);

  long countFollowers(Integer userId);

  List<UserSummaryResponse> getFollowingUsers(Integer followerId);

  List<UserSummaryResponse> getFollowerUsers(Integer followedId);

  List<String> getFollowerEmails(Integer followedUserId);
}
