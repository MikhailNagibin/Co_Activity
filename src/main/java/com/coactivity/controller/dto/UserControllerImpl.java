package com.coactivity.controller.dto;

import com.coactivity.Service.UserService;
import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.controller.dto.response.UserProfileResponse;

public class UserControllerImpl {
  ApiResponse<UserProfileResponse> getUserProfile(String token) {
    return UserService.getUserProfile(token);
  }
}
