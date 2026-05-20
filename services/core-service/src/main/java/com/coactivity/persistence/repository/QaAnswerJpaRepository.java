package com.coactivity.persistence.repository;

import com.coactivity.persistence.entity.QaAnswerEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QaAnswerJpaRepository extends JpaRepository<QaAnswerEntity, Integer> {

  @Override
  @EntityGraph(attributePaths = {"question", "owner", "previousAnswer"})
  Optional<QaAnswerEntity> findById(Integer id);

  boolean existsByIdAndQuestion_Id(Integer answerId, Integer questionId);

  @EntityGraph(attributePaths = {"question", "owner", "previousAnswer"})
  List<QaAnswerEntity> findAllByQuestion_IdOrderById(Integer questionId);

  List<QaAnswerEntity> findAllByPreviousAnswer_Id(Integer answerId);

  @Modifying
  @Query("delete from QaAnswerEntity answer where answer.question.id = :questionId")
  void deleteAllByQuestionId(@Param("questionId") Integer questionId);
}
