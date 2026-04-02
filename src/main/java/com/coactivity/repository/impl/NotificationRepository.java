package com.coactivity.repository.impl;

import com.coactivity.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    // Поиск с учётом регистра
    Optional<Notification> findByName(String name);

    @Query("SELECT n FROM Notification n WHERE LOWER(n.name) = LOWER(:name)")
    Optional<Notification> findByNameIgnoreCase(@Param("name") String name);

    default Notification getByIndex(int index) {
        return findById(index).orElseThrow(() ->
            new IllegalArgumentException("Invalid notification index: " + index));
    }
}