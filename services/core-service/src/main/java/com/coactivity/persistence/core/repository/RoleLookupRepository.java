package com.coactivity.persistence.core.repository;

import com.coactivity.persistence.core.entity.RoleEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleLookupRepository extends JpaRepository<RoleEntity, Integer> {

  @Query("select r from RoleEntity r where lower(r.roleName) = lower(:name)")
  Optional<RoleEntity> findByRoleNameIgnoreCase(@Param("name") String name);
}
