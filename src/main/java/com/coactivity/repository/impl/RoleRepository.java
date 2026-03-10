package com.coactivity.repository.impl;

import com.coactivity.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(String name);

    default Role getByIndex(int index) {
        return findById(index).orElseThrow(() ->
            new IllegalArgumentException("Invalid role index: " + index));
    }

    default Role getOwner() {
        return findByName(Role.OWNER).orElseThrow();
    }

    default Role getAdmin() {
        return findByName(Role.ADMIN).orElseThrow();
    }

    default Role getParticipant() {
        return findByName(Role.PARTICIPANT).orElseThrow();
    }
}