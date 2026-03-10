package com.coactivity.repository.impl;

import com.coactivity.domain.Answer;
import com.coactivity.domain.User;
import com.coactivity.repository.AnswerRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
@Transactional
public class AnswerRepositoryImpl implements AnswerRepository {

  @PersistenceContext
  private EntityManager entityManager;

  private final UserRepositoryImpl userRepository;

  public AnswerRepositoryImpl(UserRepositoryImpl userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public Answer createAnswer(Integer questionId, Integer previousAnswerId, String currentAnswer,
                             Integer ownerId) {
    User owner = userRepository.getUserById(ownerId);

    Answer answer = new Answer();
    answer.setQuestionId(questionId);
    answer.setPreviousAnswerId(previousAnswerId);
    answer.setAnswer(currentAnswer);
    answer.setOwnerId(owner);
    answer.setCreatedAt(Instant.now());

    entityManager.persist(answer);
    return answer;
  }

  @Override
  public List<Answer> getAnswers(Integer questionId) {
    return entityManager.createQuery(
            "SELECT a FROM Answer a WHERE a.questionId = :questionId ORDER BY a.createdAt",
            Answer.class)
        .setParameter("questionId", questionId)
        .getResultList();
  }

  @Override
  public void deleteAnswer(Answer answer) {
    Answer managedAnswer = entityManager.find(Answer.class, answer.getId());
    if (managedAnswer != null) {
      entityManager.remove(managedAnswer);
    } else {
      throw new RuntimeException("Answer not found");
    }
  }

  public void createNullAnswer() {
    User nullUser = userRepository.getUserById(0);
    if (nullUser == null) {
      throw new RuntimeException("Null user not found");
    }

    Answer answer = new Answer();
    answer.setId(0);
    answer.setQuestionId(0);
    answer.setPreviousAnswerId(0);
    answer.setAnswer("test");
    answer.setOwnerId(nullUser);
    answer.setCreatedAt(Instant.now());

    entityManager.persist(answer);
  }
}
