package com.coactivity.repository.impl;

import com.coactivity.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    Optional<Notification> findByName(String name);

    default Notification getByIndex(int index) {
        return findById(index).orElseThrow(() ->
            new IllegalArgumentException("Invalid notification index: " + index));
    }
}