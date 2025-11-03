package com.coactivity.repository;

import com.coactivity.domain.BulletinBoard;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;

import java.time.Instant;

public interface BulletinBoardRepository {
  /*
  создание доски объявлений

  @param room к какой комнате прикрепили доску
  @param content текст доски
  @param author кто внес последние изменения
  @param updatedAt когда внесли посление изменения
  @return BulletinBoard
   */
  BulletinBoard createBulletinBoard(Room room, String content, User Author, Instant updatedAt);

  /*
  изменение данных на доске объявлений
  @param room к какой комнате прикрепили доску
  @param content текст доски
  @param author кто внес последние изменения
  @param updatedAt когда внесли посление изменения
  @return BulletinBoard
   */
  BulletinBoard updateBulletinBoard(Room room, String content, User Author, Instant updatedAt);

  /*
  получение доски объявлений для конкретной комнаты

  @param room
  @return BulletinBoard
   */
  BulletinBoard getBulletinBoard(Room room);

  /*
  удаление доски объявлений у комнаты

  @param room
   */
  void deleteBulletinBoard(Room room);
}
