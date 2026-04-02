package com.coactivity.qa.persistence.repository;

import com.coactivity.qa.persistence.entity.QaAnswerEntity;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QaAnswerJpaRepository extends JpaRepository<QaAnswerEntity, Integer> {

  boolean existsByIdAndQuestion_Id(Integer answerId, Integer questionId);

  @EntityGraph(attributePaths = {"question"})
  List<QaAnswerEntity> findAllByQuestion_IdOrderById(Integer questionId);
}
