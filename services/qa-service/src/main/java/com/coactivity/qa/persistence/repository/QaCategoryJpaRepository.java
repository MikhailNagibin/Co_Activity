package com.coactivity.qa.persistence.repository;

import com.coactivity.qa.persistence.entity.QaCategoryEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QaCategoryJpaRepository extends JpaRepository<QaCategoryEntity, Integer> {

  @Query("select c from QaCategoryEntity c where lower(c.name) = lower(:name)")
  Optional<QaCategoryEntity> findByNameIgnoreCase(@Param("name") String name);
}
