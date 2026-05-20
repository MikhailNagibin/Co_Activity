package com.coactivity.repository;

import com.coactivity.controller.dto.response.UserSummaryResponse;
import com.coactivity.domain.Category;
import com.coactivity.persistence.CoreLookupMapper;
import com.coactivity.persistence.entity.CategoryEntity;
import com.coactivity.persistence.entity.QaAnswerEntity;
import com.coactivity.persistence.entity.QaQuestionEntity;
import com.coactivity.persistence.entity.UserEntity;
import com.coactivity.persistence.repository.CategoryLookupRepository;
import com.coactivity.persistence.repository.QaAnswerJpaRepository;
import com.coactivity.persistence.repository.QaQuestionJpaRepository;
import com.coactivity.persistence.repository.UserJpaRepository;
import com.coactivity.util.AvatarUrlResolver;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class QaRepository {

  private final CategoryLookupRepository categoryLookupRepository;
  private final QaQuestionJpaRepository questionJpaRepository;
  private final QaAnswerJpaRepository answerJpaRepository;
  private final UserJpaRepository userJpaRepository;

  public QaRepository(CategoryLookupRepository categoryLookupRepository,
      QaQuestionJpaRepository questionJpaRepository,
      QaAnswerJpaRepository answerJpaRepository,
      UserJpaRepository userJpaRepository) {
    this.categoryLookupRepository = categoryLookupRepository;
    this.questionJpaRepository = questionJpaRepository;
    this.answerJpaRepository = answerJpaRepository;
    this.userJpaRepository = userJpaRepository;
  }

  public Optional<Integer> findCategoryIdByName(String categoryName) {
    String normalized = CoreLookupMapper.toDbCategoryName(categoryName);
    return categoryLookupRepository.findByNameIgnoreCase(normalized).map(CategoryEntity::getId);
  }

  public Optional<QuestionEntity> findQuestionById(Integer questionId) {
    return questionJpaRepository.findById(questionId).map(this::mapQuestion);
  }

  public List<QuestionEntity> findQuestions(Integer categoryId) {
    return findQuestions(categoryId, null);
  }

  public List<QuestionEntity> findQuestions(Integer categoryId, String query) {
    String normalizedQuery = query != null ? query.trim() : null;
    boolean hasQuery = normalizedQuery != null && !normalizedQuery.isEmpty();

    if (categoryId != null && hasQuery) {
      return questionJpaRepository
          .findAllByCategory_IdAndQuestionContainingIgnoreCaseOrderById(categoryId,
              normalizedQuery)
          .stream()
          .map(this::mapQuestion)
          .toList();
    }
    if (hasQuery) {
      return questionJpaRepository.findAllByQuestionContainingIgnoreCaseOrderById(normalizedQuery)
          .stream()
          .map(this::mapQuestion)
          .toList();
    }
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
  public QuestionEntity updateQuestion(Integer questionId, String question, Integer categoryId) {
    QaQuestionEntity entity = questionJpaRepository.findById(questionId)
        .orElseThrow(() -> new IllegalStateException("Question not found: " + questionId));
    CategoryEntity category = categoryLookupRepository.findById(categoryId)
        .orElseThrow(() -> new IllegalStateException("Category not found: " + categoryId));
    entity.setQuestion(question);
    entity.setCategory(category);
    return mapQuestion(questionJpaRepository.saveAndFlush(entity));
  }

  @Transactional
  public void deleteQuestion(Integer questionId) {
    answerJpaRepository.deleteAllByQuestionId(questionId);
    questionJpaRepository.deleteById(questionId);
  }

  @Transactional
  public QuestionEntity createQuestion(Integer userId, String question, Integer categoryId) {
    UserEntity owner = userJpaRepository.findById(userId)
        .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
    CategoryEntity category = categoryLookupRepository.findById(categoryId)
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
    UserEntity owner = userJpaRepository.findById(ownerId)
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

  public Optional<AnswerEntity> findAnswerById(Integer answerId) {
    return answerJpaRepository.findById(answerId).map(this::mapAnswer);
  }

  @Transactional
  public AnswerEntity updateAnswer(Integer answerId, String answer) {
    QaAnswerEntity entity = answerJpaRepository.findById(answerId)
        .orElseThrow(() -> new IllegalStateException("Answer not found: " + answerId));
    entity.setAnswer(answer);
    return mapAnswer(answerJpaRepository.saveAndFlush(entity));
  }

  @Transactional
  public void deleteAnswer(Integer answerId) {
    for (QaAnswerEntity child : answerJpaRepository.findAllByPreviousAnswer_Id(answerId)) {
      child.setPreviousAnswer(null);
    }
    answerJpaRepository.flush();
    answerJpaRepository.deleteById(answerId);
  }

  private QuestionEntity mapQuestion(QaQuestionEntity entity) {
    return new QuestionEntity(
        entity.getId(),
        entity.getOwner().getId(),
        entity.getCategory().getId(),
        CoreLookupMapper.toCategory(entity.getCategory().getName()),
        entity.getQuestion(),
        mapUserSummary(entity.getOwner()));
  }

  private AnswerEntity mapAnswer(QaAnswerEntity entity) {
    return new AnswerEntity(
        entity.getId(),
        entity.getQuestion().getId(),
        entity.getPreviousAnswer() != null ? entity.getPreviousAnswer().getId() : null,
        entity.getOwner().getId(),
        entity.getAnswer(),
        entity.getCreatedAt() != null ? entity.getCreatedAt() : Instant.now(),
        mapUserSummary(entity.getOwner()));
  }

  private UserSummaryResponse mapUserSummary(UserEntity entity) {
    return new UserSummaryResponse(
        entity.getId(),
        entity.getUserName(),
        entity.getDataOfBirth(),
        entity.getCity(),
        entity.getCountry(),
        entity.getDescription(),
        entity.getAvatarId(),
        AvatarUrlResolver.resolveUserAvatarUrl(
            entity.getId(),
            entity.getAvatarFile() != null ? entity.getAvatarFile().getId() : null),
        null);
  }

  public record QuestionEntity(
      Integer id,
      Integer ownerId,
      Integer categoryId,
      Category category,
      String question,
      UserSummaryResponse author) {
  }

  public record AnswerEntity(
      Integer id,
      Integer questionId,
      Integer previousAnswerId,
      Integer ownerId,
      String answer,
      Instant createdAt,
      UserSummaryResponse author) {
  }
}
