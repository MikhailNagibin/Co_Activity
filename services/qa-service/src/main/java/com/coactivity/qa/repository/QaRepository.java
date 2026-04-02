package com.coactivity.qa.repository;

import com.coactivity.qa.domain.Category;
import com.coactivity.qa.dto.response.UserSummaryResponse;
import com.coactivity.qa.persistence.QaLookupMapper;
import com.coactivity.qa.persistence.entity.QaAnswerEntity;
import com.coactivity.qa.persistence.entity.QaCategoryEntity;
import com.coactivity.qa.persistence.entity.QaQuestionEntity;
import com.coactivity.qa.persistence.entity.QaUserEntity;
import com.coactivity.qa.persistence.repository.QaAnswerJpaRepository;
import com.coactivity.qa.persistence.repository.QaCategoryJpaRepository;
import com.coactivity.qa.persistence.repository.QaQuestionJpaRepository;
import com.coactivity.qa.persistence.repository.QaUserJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class QaRepository {

  private final QaCategoryJpaRepository categoryJpaRepository;
  private final QaQuestionJpaRepository questionJpaRepository;
  private final QaAnswerJpaRepository answerJpaRepository;
  private final QaUserJpaRepository userJpaRepository;

  public QaRepository(QaCategoryJpaRepository categoryJpaRepository,
      QaQuestionJpaRepository questionJpaRepository,
      QaAnswerJpaRepository answerJpaRepository,
      QaUserJpaRepository userJpaRepository) {
    this.categoryJpaRepository = categoryJpaRepository;
    this.questionJpaRepository = questionJpaRepository;
    this.answerJpaRepository = answerJpaRepository;
    this.userJpaRepository = userJpaRepository;
  }

  public Optional<Integer> findCategoryIdByName(String categoryName) {
    String normalized = QaLookupMapper.toDbCategoryName(categoryName);
    return categoryJpaRepository.findByNameIgnoreCase(normalized)
        .map(QaCategoryEntity::getId);
  }

  public Optional<QuestionEntity> findQuestionById(Integer questionId) {
    return questionJpaRepository.findById(questionId)
        .map(this::mapQuestion);
  }

  public List<QuestionEntity> findQuestions(Integer categoryId) {
    if (categoryId != null) {
      return questionJpaRepository.findAllByCategory_IdOrderById(categoryId).stream()
          .map(this::mapQuestion)
          .toList();
    }

    return questionJpaRepository.findAll().stream()
        .sorted(java.util.Comparator.comparing(QaQuestionEntity::getId))
        .map(this::mapQuestion)
        .toList();
  }

  @Transactional
  public QuestionEntity createQuestion(Integer userId, String question, Integer categoryId) {
    QaUserEntity owner = userJpaRepository.findById(userId)
        .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
    QaCategoryEntity category = categoryJpaRepository.findById(categoryId)
        .orElseThrow(() -> new IllegalStateException("Category not found: " + categoryId));

    QaQuestionEntity entity = new QaQuestionEntity();
    entity.setOwner(owner);
    entity.setQuestion(question);
    entity.setCategory(category);

    return mapQuestion(questionJpaRepository.saveAndFlush(entity));
  }

  public boolean questionExists(Integer questionId) {
    return questionJpaRepository.existsById(questionId);
  }

  public boolean answerExistsForQuestion(Integer answerId, Integer questionId) {
    return answerJpaRepository.existsByIdAndQuestion_Id(answerId, questionId);
  }

  @Transactional
  public AnswerEntity createAnswer(Integer questionId, Integer previousAnswerId, String answer,
      Integer ownerId) {
    QaQuestionEntity question = questionJpaRepository.findById(questionId)
        .orElseThrow(() -> new IllegalStateException("Question not found: " + questionId));
    QaUserEntity owner = userJpaRepository.findById(ownerId)
        .orElseThrow(() -> new IllegalStateException("User not found: " + ownerId));

    QaAnswerEntity entity = new QaAnswerEntity();
    entity.setQuestion(question);
    entity.setOwner(owner);
    entity.setAnswer(answer);
    entity.setCreatedAt(Instant.now());
    if (previousAnswerId != null) {
      entity.setPreviousAnswer(answerJpaRepository.findById(previousAnswerId)
          .orElseThrow(() -> new IllegalStateException(
              "Previous answer not found: " + previousAnswerId)));
    }

    return mapAnswer(answerJpaRepository.saveAndFlush(entity));
  }

  public List<AnswerEntity> findAnswersByQuestionId(Integer questionId) {
    return answerJpaRepository.findAllByQuestion_IdOrderById(questionId).stream()
        .map(this::mapAnswer)
        .toList();
  }

  public Optional<UserSummaryResponse> findUserSummaryById(Integer userId) {
    return userJpaRepository.findById(userId)
        .map(this::mapUserSummary);
  }

  private QuestionEntity mapQuestion(QaQuestionEntity entity) {
    return new QuestionEntity(
        entity.getId(),
        entity.getOwner().getId(),
        entity.getCategory().getId(),
        QaLookupMapper.toCategoryEnum(entity.getCategory().getName()),
        entity.getQuestion());
  }

  private AnswerEntity mapAnswer(QaAnswerEntity entity) {
    return new AnswerEntity(
        entity.getId(),
        entity.getQuestion().getId(),
        entity.getPreviousAnswer() != null ? entity.getPreviousAnswer().getId() : null,
        entity.getOwner().getId(),
        entity.getAnswer(),
        entity.getCreatedAt() != null ? entity.getCreatedAt() : Instant.now());
  }

  private UserSummaryResponse mapUserSummary(QaUserEntity entity) {
    return new UserSummaryResponse(
        entity.getId(),
        entity.getUserName(),
        entity.getDateOfBirth(),
        entity.getCity(),
        entity.getCountry(),
        entity.getDescription(),
        entity.getAvatarId());
  }

  public record QuestionEntity(
      Integer id,
      Integer ownerId,
      Integer categoryId,
      Category category,
      String question) {
  }

  public record AnswerEntity(
      Integer id,
      Integer questionId,
      Integer previousAnswerId,
      Integer ownerId,
      String answer,
      Instant createdAt) {
  }
}
