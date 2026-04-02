package com.coactivity.qa.persistence.repository;

import com.coactivity.qa.persistence.entity.QaQuestionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QaQuestionJpaRepository extends JpaRepository<QaQuestionEntity, Integer> {

  @Override
  @EntityGraph(attributePaths = {"category"})
  List<QaQuestionEntity> findAll();

  @Override
  @EntityGraph(attributePaths = {"category"})
  java.util.Optional<QaQuestionEntity> findById(Integer id);

  @EntityGraph(attributePaths = {"category"})
  List<QaQuestionEntity> findAllByCategory_IdOrderById(Integer categoryId);
}
