package com.coactivity.repository;

import com.coactivity.controller.dto.request.UserProfileUpdateRequest;
import com.coactivity.controller.dto.request.UserRegistrationRequest;
import com.coactivity.domain.User;
import java.util.Optional;

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

  Optional<User> getUserByLogin(String login);
}
