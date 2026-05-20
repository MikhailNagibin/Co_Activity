package com.coactivity.persistence.repository;

import com.coactivity.persistence.entity.UserNotificationEntity;
import com.coactivity.persistence.entity.UserNotificationId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserNotificationJpaRepository
    extends JpaRepository<UserNotificationEntity, UserNotificationId> {

  @EntityGraph(attributePaths = {"notification"})
  List<UserNotificationEntity> findAllByUser_Id(Integer userId);

  Optional<UserNotificationEntity> findByUser_IdAndNotification_Id(Integer userId,
      Integer notificationId);

  void deleteAllByUser_Id(Integer userId);
}
