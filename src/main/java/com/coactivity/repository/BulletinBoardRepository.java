package com.coactivity.repository;

import com.coactivity.domain.BulletinBoard;
import com.coactivity.domain.Room;
import com.coactivity.domain.User;

import java.time.Instant;

public interface BulletinBoardRepository {
  /*
  создание доски объявлений

  @param roomId идентификатор комнаты, к которой прикрепляется доска
  @param content текст объявления
  @param authorId идентификатор пользователя, создавшего доску
  @param updatedAt время создания доски
  @return BulletinBoard созданная доска объявлений
   */
  BulletinBoard createBulletinBoard(int roomId, String content, int authorId, Instant updatedAt);

  /*
  изменение данных на доске объявлений

  @param roomId идентификатор комнаты, к которой прикреплена доска
  @param content новый текст объявления
  @param authorId идентификатор пользователя, внесшего изменения
  @param updatedAt время последнего обновления
  @return BulletinBoard обновленная доска объявлений
   */
  BulletinBoard updateBulletinBoard(int roomId, String content, int authorId, Instant updatedAt);

  /*
  получение доски объявлений для конкретной комнаты

  @param roomId идентификатор комнаты
  @return BulletinBoard доска объявлений комнаты или null, если не найдена
   */
  BulletinBoard getBulletinBoard(int roomId);

  /*
  удаление доски объявлений у комнаты

  @param roomId идентификатор комнаты
   */
  void deleteBulletinBoard(int roomId);
}