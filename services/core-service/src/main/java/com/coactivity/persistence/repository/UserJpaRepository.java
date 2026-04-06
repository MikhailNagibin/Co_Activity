package com.coactivity.persistence.repository;

import com.coactivity.persistence.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserEntity, Integer> {

  Optional<UserEntity> findByEmailNormalized(String emailNormalized);

  boolean existsByEmailNormalized(String emailNormalized);

  boolean existsByUserNameIgnoreCase(String userName);

  boolean existsByUserNameIgnoreCaseAndIdNot(String userName, Integer id);
}
