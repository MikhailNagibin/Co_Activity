package com.coactivity.repository;

import com.coactivity.domain.User;

import java.time.Instant;
import java.util.AbstractMap;

public interface UserRepository {
  /**
   * создание нового пользователя
   *
   * @param login
   * @param password
   * @param birthday
   * @param country
   * @param city
   * @param description
   * @param avatarId
   * @return
   */

  User createUser(String login, String username, String password, Instant birthday, String country, String city, String description, int avatarId);

  /**
   * обновления персональных данных
   *
   * @param user
   * @param password
   * @param birthday
   * @param country
   * @param city
   * @param description
   * @param avatarId
   */

  void updateUser(User user, String password, Instant birthday, String country, String city, String description, int avatarId, String username);


  /**
   * удаление пользователя
   *
   * @param id - идентификатор пользователя
   */
  void deleteUser(int id);

  /**
   *  Получение юзера
   *
   * @param login
   * @param password
   * @return
   */

  User getUser(int login, String password);

  /**
   *
   * @param id
   * @return
   */
  User getUserById(int id);
}
