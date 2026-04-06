package com.coactivity.persistence.repository;

import com.coactivity.persistence.entity.QaAnswerEntity;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QaAnswerJpaRepository extends JpaRepository<QaAnswerEntity, Integer> {

  boolean existsByIdAndQuestion_Id(Integer answerId, Integer questionId);

  @EntityGraph(attributePaths = {"question", "owner", "previousAnswer"})
  List<QaAnswerEntity> findAllByQuestion_IdOrderById(Integer questionId);
}
