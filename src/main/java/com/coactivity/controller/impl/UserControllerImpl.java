package com.coactivity.controller.impl;

import com.coactivity.service.UserService;
import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.controller.dto.response.UserProfileResponse;

public class UserControllerImpl {
  public ApiResponse<UserProfileResponse> getUserProfile(int token) {
    return UserService.getUserProfile(token);
  }
}
