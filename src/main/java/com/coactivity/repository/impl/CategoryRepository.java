package com.coactivity.repository.impl;

import com.coactivity.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    @Query("SELECT c FROM Category c WHERE LOWER(c.name) = LOWER(:name)")
    Optional<Category> findByName(String name);

    default Category getByIndex(int index) {
        return findById(index).orElseThrow(() ->
            new IllegalArgumentException("Invalid category index: " + index));
    }
}