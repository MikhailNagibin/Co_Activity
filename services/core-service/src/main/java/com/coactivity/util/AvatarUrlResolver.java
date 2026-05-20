package com.coactivity.util;

public final class AvatarUrlResolver {

  private AvatarUrlResolver() {
  }

  public static String resolveUserAvatarUrl(Integer userId, Integer avatarFileId) {
    if (userId == null || avatarFileId == null) {
      return null;
    }
    return "/api/users/" + userId + "/avatar";
  }
}
