package com.coactivity.repository;

import com.coactivity.domain.RequestStatus;
import com.coactivity.domain.Room;
import com.coactivity.domain.RoomsRequest;
import com.coactivity.domain.User;

import java.util.List;

public interface RoomsRequestRepository {
  /*
  создание запроса на вступление в комнату

  @param User user - кто хочет вступить в комнату
  @param Room room - в какую комнату хотят вступить
  @param RequestStatus status - статус заявки
  @return RoomsRequest request
   */
  RoomsRequest createRequest(User user, Room room, RequestStatus status);

  /*
  обновление статуса заявки

  @param int requestId
  @param RequestStatus newStatus
  @return RoomsRequest request
   */
  RoomsRequest updateRequest(int requestId, RequestStatus status);

  /*
  удалить статус

  @param int requestId
   */
  void deleteRequest(int requestId);

  /*
  получить все запросы в комнату

  @param int roomId
  @return List<RoomsRequest>
   */
  List<RoomsRequest> getRoomRequest(int roomId);
}
