package com.coactivity.repository;

import com.coactivity.domain.RequestStatus;
import com.coactivity.domain.Room;
import com.coactivity.domain.RoomsRequest;
import com.coactivity.domain.User;

import java.util.List;

public interface RoomsRequestRepository {
  /**
   * Создание запроса на вступление в комнату
   *
   * @param userId
   * @param roomId
   * @param statusId
   * @return
   */
  RoomsRequest createRequest(int userId, int roomId, int statusId);

  /**
   * Обновление запроса
   *
   * @param requestId
   * @param status
   * @return
   */
  RoomsRequest updateRequest(int requestId, RequestStatus status);

  /**
   * Удаление запроса
   *
   * @param requestId
   */
  void deleteRequest(int requestId);

  /**
   * запросы в конкретную комнату
   *
   * @param roomId
   * @return
   */
  List<RoomsRequest> getRoomRequest(int roomId);

  /**
   * Все запросы для конкретного пользователя
   *
   * @param userId
   * @return
   */
  List<RoomsRequest> getRequestsByUser(int userId);
}
