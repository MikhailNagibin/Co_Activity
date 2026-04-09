package com.coactivity.repository;

import com.coactivity.domain.RequestStatus;
import com.coactivity.domain.RoomsRequest;
import java.util.List;

public interface RoomsRequestRepository {

  /**
   * Создание запроса на вступление в комнату
   *
   * @param userId
   * @param roomId
   * @param status
   * @return
   */
  RoomsRequest createRequest(int userId, int roomId, RequestStatus status);

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
   * Получение запросов в конкретную комнату
   *
   * @param roomId
   * @return
   */
  List<RoomsRequest> getRoomRequests(int roomId);

  /**
   * Все запросы для конкретного пользователя
   *
   * @param userId
   * @return
   */
  List<RoomsRequest> getRequestsByUser(int userId);

  /**
   * Получение конкретного запроса по идентификатору.
   *
   * @param requestId уникальный идентификатор запроса
   * @return запрос или {@code null}, если не найден
   */
  RoomsRequest getRequestById(int requestId);

  RoomsRequest getRequestByUserAndRoom(int userId, int roomId);

  void deletePendingRequestsByRoom(int roomId);

  int updatePendingRequestsByRoom(int roomId, RequestStatus status);
}
