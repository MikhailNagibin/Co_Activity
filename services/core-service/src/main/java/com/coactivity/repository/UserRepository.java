package com.coactivity.repository;

import com.coactivity.controller.dto.request.UserProfileUpdateRequest;
import com.coactivity.controller.dto.request.UserRegistrationRequest;
import com.coactivity.domain.Notification;
import com.coactivity.domain.User;

public interface UserRepository {

  User createUser(UserRegistrationRequest request);

  /**
   * Обновление персональных данных
   *
   * @param userId
   * @param request
   */
  void updateUser(Integer userId, UserProfileUpdateRequest request);


  /**
   * Удаление пользователя
   *
   * @param userId идентификатор пользователя
   */
  void deleteUser(Integer userId);

  /**
   * Получение юзера по id
   *
   * @param userId
   * @return
   */
  User getUserById(Integer userId);

  void setNotification(Integer id, Notification notification);

  void removeNotification(Integer id, Notification notification);
}
