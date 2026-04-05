package com.coactivity.persistence.repository;

import com.coactivity.persistence.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserEntity, Integer> {

  Optional<UserEntity> findByLogin(String login);

  Optional<UserEntity> findByLoginAndPassword(String login, String password);
}
