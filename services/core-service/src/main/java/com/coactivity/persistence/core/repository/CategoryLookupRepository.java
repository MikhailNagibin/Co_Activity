package com.coactivity.persistence.core.repository;

import com.coactivity.persistence.core.entity.CategoryEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryLookupRepository extends JpaRepository<CategoryEntity, Integer> {

  @Query("select c from CategoryEntity c where lower(c.name) = lower(:name)")
  Optional<CategoryEntity> findByNameIgnoreCase(@Param("name") String name);
}
