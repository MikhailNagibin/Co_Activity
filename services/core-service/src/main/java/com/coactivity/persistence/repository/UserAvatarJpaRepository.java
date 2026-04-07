package com.coactivity.persistence.repository;

import com.coactivity.persistence.entity.UserAvatarEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAvatarJpaRepository extends JpaRepository<UserAvatarEntity, Integer> {
}
