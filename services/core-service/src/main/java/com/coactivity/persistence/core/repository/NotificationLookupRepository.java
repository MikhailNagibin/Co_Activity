package com.coactivity.persistence.core.repository;

import com.coactivity.persistence.core.entity.NotificationEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationLookupRepository extends JpaRepository<NotificationEntity, Integer> {

  @Query("select n from NotificationEntity n where lower(n.notificationName) = lower(:name)")
  Optional<NotificationEntity> findByNotificationNameIgnoreCase(@Param("name") String name);
}
