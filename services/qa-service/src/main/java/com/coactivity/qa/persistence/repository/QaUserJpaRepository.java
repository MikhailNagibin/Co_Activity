package com.coactivity.qa.persistence.repository;

import com.coactivity.qa.persistence.entity.QaUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QaUserJpaRepository extends JpaRepository<QaUserEntity, Integer> {
}
