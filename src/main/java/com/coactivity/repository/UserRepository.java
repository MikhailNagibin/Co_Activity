package com.coactivity.repository;

import com.coactivity.domain.User;
import java.util.AbstractMap;

public interface UserRepository {
  /*
  создание нового пользователя

  @param features - мапа с ключами - полями в бд, значения - значение соответствующего поля
  @return User
   */

  User createUser(AbstractMap.SimpleEntry<String, String> features);

  /*
  обновление данных о пользователе (кроме пароля)
  @param features - мапа с ключами - полями в бд, значения - значение соответствующего поля
   */

  void updateUser(AbstractMap.SimpleEntry<String, String> features);

  /*
  удаление пользователя

  @param id - идентификатор пользователя
   */
  void deleteUser(int id);

  /*
  Получение юзера

  @param login
  @param password
  @return User
   */

  User getUser(int login, String password);
}
