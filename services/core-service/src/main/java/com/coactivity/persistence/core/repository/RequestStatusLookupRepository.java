package com.coactivity.persistence.core.repository;

import com.coactivity.persistence.core.entity.RequestStatusEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequestStatusLookupRepository extends JpaRepository<RequestStatusEntity, Integer> {

  @Query("select rs from RequestStatusEntity rs where lower(rs.statusInfo) = lower(:name)")
  Optional<RequestStatusEntity> findByStatusInfoIgnoreCase(@Param("name") String name);
}
