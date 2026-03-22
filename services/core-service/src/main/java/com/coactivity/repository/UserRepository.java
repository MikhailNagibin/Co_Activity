package com.coactivity.repository;

import com.coactivity.controller.dto.request.UserProfileUpdateRequest;
import com.coactivity.controller.dto.request.UserRegistrationRequest;
import com.coactivity.domain.Notification;
import com.coactivity.domain.User;

public interface UserRepository {

  /**
   * Создание нового пользователя
   *
   * @param request
   * @return
   */
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
   * Получение юзера по логину и паролю
   *
   * @param login
   * @param password
   * @return
   */

  User getUser(String login, String password);

  /**
   * Получение юзера по id
   *
   * @param userId
   * @return
   */
  User getUserById(Integer userId);

  User getUserByLogin(String login);

  void updatePassword(Integer userId, String newPassword);

  void setNotification(Integer id, Notification notification);

  void removeNotification(Integer id, Notification notification);
}
