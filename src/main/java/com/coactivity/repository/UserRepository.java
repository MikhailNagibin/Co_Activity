package com.coactivity.repository;

import com.coactivity.controller.dto.request.UserProfileUpdateRequest;
import com.coactivity.controller.dto.request.UserRegistrationRequest;
import com.coactivity.domain.User;
import java.sql.SQLException;
import java.util.List;

public interface UserRepository {

  User createUser(UserRegistrationRequest request);

  void updateUser(Integer userId, UserProfileUpdateRequest request);

  void deleteUser(Integer userId);

  User getUser(String login, String password);

  User getUserById(Integer userId);

  User getUserByLogin(String login);

  void printAllCategories() throws SQLException;

  List<Integer> getAllUsers();
}