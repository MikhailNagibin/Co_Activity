package com.coactivity.repository;

import com.coactivity.domain.BulletinBoard;

public interface BulletinBoardRepository {

  /**
   *
   * @param roomId
   * @param content
   * @param authorId
   * @return
   */
  BulletinBoard createBulletinBoard(Integer roomId, String content, Integer authorId);

  /**
   *
   * @param roomId
   * @param content
   * @param authorId
   * @return BulletinBoard обновленная доска объявлений
   */
  BulletinBoard updateBulletinBoard(Integer roomId, String content, Integer authorId);

  /**
   * Проверяет, есть ли у комнаты доска объявлений
   *
   * @param roomId
   * @return
   */
  boolean isBulletinBoardExists(Integer roomId);

  /**
   *
   * @param roomId
   * @return
   */
  BulletinBoard getBulletinBoard(Integer roomId);

  /**
   *
   * @param roomId
   */
  void deleteBulletinBoard(Integer roomId);
}