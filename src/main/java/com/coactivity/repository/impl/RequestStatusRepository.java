package com.coactivity.repository.impl;

import com.coactivity.domain.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RequestStatusRepository extends JpaRepository<RequestStatus, Integer> {
    Optional<RequestStatus> findByName(String name);

    @Query("SELECT rs FROM RequestStatus rs WHERE LOWER(rs.name) = LOWER(:name)")
    Optional<RequestStatus> findByNameIgnoreCase(@Param("name") String name);

    default RequestStatus fromDatabase(String dbValue) {
        return findByName(dbValue).orElseThrow(() ->
            new IllegalArgumentException("Unknown status: '" + dbValue + "'"));
    }

    default RequestStatus getConsideration() {
        return findByName(RequestStatus.CONSIDERATION).orElseThrow();
    }

    default RequestStatus getAccepted() {
        return findByName(RequestStatus.ACCEPTED).orElseThrow();
    }

    default RequestStatus getRefused() {
        return findByName(RequestStatus.REFUSED).orElseThrow();
    }

    default RequestStatus getRefusedWithBan() {
        return findByName(RequestStatus.REFUSED_WITH_BAN).orElseThrow();
    }
}
