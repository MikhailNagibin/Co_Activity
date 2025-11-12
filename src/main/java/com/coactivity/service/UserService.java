package com.coactivity.service;

import com.coactivity.DataRepository;
import com.coactivity.controller.dto.request.UserProfileUpdateRequest;
import com.coactivity.controller.dto.response.ApiResponse;
import com.coactivity.controller.dto.response.UserProfileResponse;
import com.coactivity.domain.User;
import com.coactivity.repository.impl.UserRepositoryImpl;

public class UserService {
  private UserRepositoryImpl users;
  private DataRepository repository;

  public UserService() {
    repository = new DataRepository();
    users = new UserRepositoryImpl(repository);
  }

  public ApiResponse<UserProfileResponse> getUserProfile(int token) {
    var response = new UserProfileResponse();
    try {
      User user = users.getUserById(token);

      response.setId(user.getId());
      response.setCity(user.getCity());
      response.setAvatarId(user.getAvatarId());
      response.setDescription(user.getDescription());
      response.setCountry(user.getCountry());
      response.setEmail(user.getLogin());
      response.setUsername(user.getUsername());
      response.setDateOfBirth(user.getDataOfBirth());

      return ApiResponse.success(response);
    } catch (Exception e) {
      return ApiResponse.success(null);
    }
  }
  public ApiResponse updateUserProfile(int token, UserProfileUpdateRequest request) {
    User user = users.getUserById(token);
    try {
      users.updateUser(user, user.getPassword(), request.getDateOfBirth(), request.getCountry(), request.getCity(),
        request.getDescription(), request.getAvatarId(), request.getUsername());
      return ApiResponse.success(null);
    } catch (Exception e) {
      return ApiResponse.error(null);
    }
  }
}
