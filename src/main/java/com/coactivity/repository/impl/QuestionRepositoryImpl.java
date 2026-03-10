package com.coactivity.repository.impl;

import com.coactivity.domain.Category;
import com.coactivity.domain.Question;
import com.coactivity.domain.User;
import com.coactivity.repository.impl.CategoryRepository;
import com.coactivity.repository.QuestionRepository;
import com.coactivity.service.exception.ValidationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Transactional
public class QuestionRepositoryImpl implements QuestionRepository {

  @PersistenceContext
  private EntityManager entityManager;

  private final UserRepositoryImpl userRepository;
  private final CategoryRepository categoryRepository;

  public QuestionRepositoryImpl(UserRepositoryImpl userRepository,
                                CategoryRepository categoryRepository) {
    this.userRepository = userRepository;
    this.categoryRepository = categoryRepository;
  }

  @Override
  public Question createQuestion(Integer userId, String question, String category) {
    Category categoryEntity = categoryRepository.findByName(category.toUpperCase())
        .orElseThrow(() -> new ValidationException("Category not found: " + category));

    User owner = userRepository.getUserById(userId);

    Question newQuestion = new Question();
    newQuestion.setOwner(owner);
    newQuestion.setQuestion(question);
    newQuestion.setCategory(categoryEntity);

    entityManager.persist(newQuestion);
    return newQuestion;
  }

  public Integer getCategoryIdByName(String categoryName) {
    return categoryRepository.findByName(categoryName.toUpperCase())
        .map(Category::getId)
        .orElse(null);
  }

  public Category getCategoryById(Integer categoryId) {
    return categoryRepository.findById(categoryId)
        .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));
  }

  @Override
  public List<Question> getAllQuestions() {
    return entityManager.createQuery(
            "SELECT q FROM Question q WHERE q.id > 0 ORDER BY q.id",
            Question.class)
        .getResultList();
  }

  public Question getQuestionById(Integer questionId) {
    try {
      return entityManager.createQuery(
              "SELECT q FROM Question q WHERE q.id = :id AND q.id > 0",
              Question.class)
          .setParameter("id", questionId)
          .getSingleResult();
    } catch (NoResultException e) {
      throw new RuntimeException("Question not found with id: " + questionId);
    }
  }

  @Override
  public Question updateQuestion(Integer questionId, String question, Integer categoryId) {
    Question existingQuestion = getQuestionById(questionId);
    Category category = getCategoryById(categoryId);

    existingQuestion.setQuestion(question);
    existingQuestion.setCategory(category);

    return entityManager.merge(existingQuestion);
  }

  @Override
  public void deleteQuestion(Integer questionId) {
    deleteAllWithQuestion(questionId);

    Question question = getQuestionById(questionId);
    entityManager.remove(question);
  }

  private void deleteAllWithQuestion(Integer questionId) {
    entityManager.createQuery("DELETE FROM Answer a WHERE a.questionId = :questionId AND a.questionId > 0")
        .setParameter("questionId", questionId)
        .executeUpdate();
  }

  public List<Question> getQuestionsByCategory(Integer categoryId) {
    Category category = getCategoryById(categoryId);
    return entityManager.createQuery(
            "SELECT q FROM Question q WHERE q.category = :category AND q.id > 0 ORDER BY q.id",
            Question.class)
        .setParameter("category", category)
        .getResultList();
  }

  public List<Question> getQuestionsByUser(Integer userId) {
    User user = userRepository.getUserById(userId);
    return entityManager.createQuery(
            "SELECT q FROM Question q WHERE q.owner = :user AND q.id > 0 ORDER BY q.id",
            Question.class)
        .setParameter("user", user)
        .getResultList();
  }

  public boolean questionExists(Integer questionId) {
    Long count = entityManager.createQuery(
            "SELECT COUNT(q) FROM Question q WHERE q.id = :id AND q.id > 0",
            Long.class)
        .setParameter("id", questionId)
        .getSingleResult();
    return count > 0;
  }

  public int getQuestionsCount() {
    return entityManager.createQuery(
            "SELECT COUNT(q) FROM Question q WHERE q.id > 0",
            Long.class)
        .getSingleResult()
        .intValue();
  }

  public Question createNullQuestion() {
    User nullUser = userRepository.getUserById(0);
    Category defaultCategory = categoryRepository.findByName(Category.SPORT)
        .orElseThrow(() -> new RuntimeException("Default category not found"));

    Question question = new Question();
    question.setId(0);
    question.setOwner(nullUser);
    question.setQuestion("Test");
    question.setCategory(defaultCategory);

    entityManager.persist(question);
    return question;
  }
}
