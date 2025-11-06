package com.coactivity.repository;

import com.coactivity.domain.User;
import java.util.AbstractMap;

public interface UserRepository {
  /*
  создание нового пользователя

  @param features - мапа с ключами - полями в бд, значения - значение соответствующего поля
  @return User
   */

  User createUser(String login, String password, String birthday, String country, String city, String description, int avatar_id);

  /*
  обновление данных о пользователе (кроме пароля)
  @param features - мапа с ключами - полями в бд, значения - значение соответствующего поля
   */

  User updateUser(String login, String password, String birthday, String country, String city, String description, int avatar_id);


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

  User getUser(String login, String password);
}
