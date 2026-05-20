package com.coactivity.persistence.repository;

import com.coactivity.persistence.entity.QaQuestionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QaQuestionJpaRepository extends JpaRepository<QaQuestionEntity, Integer> {

  @Override
  @EntityGraph(attributePaths = {"category", "owner"})
  List<QaQuestionEntity> findAll();

  @Override
  @EntityGraph(attributePaths = {"category", "owner"})
  Optional<QaQuestionEntity> findById(Integer id);

  @EntityGraph(attributePaths = {"category", "owner"})
  List<QaQuestionEntity> findAllByCategory_IdOrderById(Integer categoryId);

  @EntityGraph(attributePaths = {"category", "owner"})
  List<QaQuestionEntity> findAllByQuestionContainingIgnoreCaseOrderById(String query);

  @EntityGraph(attributePaths = {"category", "owner"})
  List<QaQuestionEntity> findAllByCategory_IdAndQuestionContainingIgnoreCaseOrderById(
      Integer categoryId, String query);
}
