package com.coactivity.repository;

import com.coactivity.controller.dto.request.UserProfileUpdateRequest;
import com.coactivity.controller.dto.request.UserRegistrationRequest;
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
  void updateUser(int userId, UserProfileUpdateRequest request);


  /**
   * Удаление пользователя
   *
   * @param id - идентификатор пользователя
   */
  void deleteUser(int id);

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
   * @param id
   * @return
   */
  User getUserById(int id);
}
