import domain.Room;
import domain.User;

import java.util.AbstractMap;
import java.util.List;

interface ApiController {
  /*
  идентификация пользователя

  @param email - почта пользователя
  @return есть ли пользователь с такой почтой
   */
  boolean identification(String email);

  /*
  аунтефикация пользователя

  @param password - пароль пользователя
  @return совпадает ли хеш пароля с хешом пароля индетифицированного пользователя
   */
  boolean authentication(String password);

  /*
  авторизация пользователя

  @return пользователь, который прошел авторизацию и аунтефикацию
   */
  User authorization();
  /*
  создает пользователя по данным, собраным с формы регистрации (из UI)

  @param data мапа с ключами - колонки в бд, значения - данные о пользователе
   */
  void createUser(AbstractMap.SimpleEntry<String, String> data);
  /*
  Возвращает все комнаты

  @return rooms
   */
  List<Room> getAllRooms();

  /*
  возвращает комнату по id пользователя-участника

  @param userId идентификатор пользователя
  @return allRooms возвращает список всех комнат,
  где указанный пользователь является участником
   */
  List<Room> getAllRoomsByParticipant(int UserId);

  /*
  возвращает комнату по id владельца

  @param userId идентификатор пользователя
  @return allRooms список комнат
   */
  List<Room> getAllRoomsByOwner(int userId);

  /*
  создает комнату по данным из формы создания комнаты (из UI)

  @param data мапа с данными из формы
   */
  void createRoom(AbstractMap.SimpleEntry<String, String> data);


  /*
  Возвращает все запросы в конкретную комнату

  @param roomId id комнаты
  @return users список пользователей, которые хотять вступить в комнату
   */
  List<User> getUsersRequestByRoomId(int roomId);

  /*
  Возвращает всех пользователей, которые находяться в комнате

  @param RoomId идентификатор комнаты
  @return users список пользователей, находящихся в комнате
   */
  List<User> getUsersInRoom(int RoomId);

  /*
  Возвращает список список комнат, в которые пользователь подал заявку

  @param userId идентификатор пользователя
  @return rooms список комнат
   */
  List<Room> getRoomsByUsersRequests(int userId);

  /*
    Возвращает список забаненых комнат для конкретного пользователя

    @param userID индентификатор пользователя
    @return rooms список комнат
   */
  List<Room> getBanRoomByUserId(int userId);

  /*
  возвращает все картинки для конкретной комнаты

  @param roomId идентификатор комнаты
  @return pictures список названий картинок
   */
  List<String> getPicturesForRoom(int roomId);

  /*
  Возвращает все вопросы

  @return questions
   */
  List<String> getAllQuestion();

  /*

   */
}