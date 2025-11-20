package com.coactivity.service;

import com.coactivity.controller.dto.request.AnswerRequest;
import com.coactivity.controller.dto.request.QuestionRequest;
import com.coactivity.controller.dto.response.AnswerResponse;
import com.coactivity.controller.dto.response.QuestionResponse;
import com.coactivity.controller.dto.response.QuestionWithAnswersResponse;
import com.coactivity.domain.Answer;
import com.coactivity.domain.Question;
import com.coactivity.repository.impl.AnswerRepositoryImpl;
import com.coactivity.repository.impl.QuestionRepositoryImpl;
import com.coactivity.repository.impl.UserRepositoryImpl;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QAService {

    private final QuestionRepositoryImpl questionRepository;
    private final AnswerRepositoryImpl answerRepository;
    private final UserRepositoryImpl userRepository;

    public QAService(QuestionRepositoryImpl questionRepository, AnswerRepositoryImpl answerRepository, UserRepositoryImpl userRepository) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.userRepository = userRepository;
    }

    public QuestionResponse askQuestion(Integer userId, QuestionRequest request) {
        // 1. Validate input
        if (request == null || request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            throw new IllegalArgumentException("Question text cannot be empty");
        }
        if (request.getCategoryId() == null) {
            throw new IllegalArgumentException("Category ID is required");
        }

        // 2. Create domain object via repository
        // Note: This call uses Category.getByIndex(categoryId), which is problematic if categoryId is a DB ID.
        // For now, we assume the request's categoryId aligns with the enum's index or is handled correctly by the repository.
        Question domainQuestion = questionRepository.createQuestion(userId, request.getQuestion(), request.getCategoryId());

        // 3. Map domain object to DTO
        // The author is the user making the request
        var author = userRepository.getUserById(userId);
        // Assuming User has a method to convert to UserProfileResponse, or we build it manually.
        // For now, we'll create a UserProfileResponse manually from the User domain object.
        var authorProfile = new com.coactivity.controller.dto.response.UserProfileResponse();
        authorProfile.setId(author.getId());
        authorProfile.setEmail(author.getLogin()); // Assuming 'login' is the email
        authorProfile.setUsername(author.getUsername());
        authorProfile.setDateOfBirth(author.getDataOfBirth());
        authorProfile.setCity(author.getCity());
        authorProfile.setCountry(author.getCountry());
        authorProfile.setDescription(author.getDescription());
        authorProfile.setAvatarId(author.getAvatarId());
        // Assuming author.getNotifications() returns List<Notification>
        // authorProfile.setNotifications(author.getNotifications()); // Requires mapping

        QuestionResponse response = new QuestionResponse();
        response.setId(domainQuestion.getId());
        response.setCategory(domainQuestion.getCategory());
        response.setQuestion(domainQuestion.getQuestion());
        response.setAuthor(authorProfile);
        // We don't set answerCount, createdAt, or lastActivityAt here as per instructions.

        return response;
    }

    public AnswerResponse answerQuestion(Integer userId, AnswerRequest request) {
        // 1. Validate input
        if (request == null || request.getAnswer() == null || request.getAnswer().trim().isEmpty()) {
            throw new IllegalArgumentException("Answer text cannot be empty");
        }
        if (request.getQuestionId() == null) {
            throw new IllegalArgumentException("Question ID is required");
        }

        // 2. Check if the parent question exists
        Question parentQuestion = questionRepository.getQuestionById(request.getQuestionId());
        if (parentQuestion == null) {
            throw new IllegalArgumentException("Question not found");
        }

        // 3. Check if the previous answer exists (if provided)
        // We must iterate through all answers for the question to check if the previous answer exists
        if (request.getPreviousAnswerId() != null) {
            List<Answer> allAnswers = answerRepository.getAnswers(request.getQuestionId());
            boolean prevAnswerExists = allAnswers.stream()
                    .anyMatch(answer -> answer.getId().equals(request.getPreviousAnswerId()));
            if (!prevAnswerExists) {
                throw new IllegalArgumentException("Previous answer not found");
            }
        }

        // 4. Create domain object via repository
        Answer domainAnswer = answerRepository.createAnswer(request.getQuestionId(), request.getPreviousAnswerId(), request.getAnswer(), userId);

        // 5. Map domain object to DTO
        var author = userRepository.getUserById(userId);
        var authorProfile = new com.coactivity.controller.dto.response.UserProfileResponse();
        authorProfile.setId(author.getId());
        authorProfile.setEmail(author.getLogin());
        authorProfile.setUsername(author.getUsername());
        authorProfile.setDateOfBirth(author.getDataOfBirth());
        authorProfile.setCity(author.getCity());
        authorProfile.setCountry(author.getCountry());
        authorProfile.setDescription(author.getDescription());
        authorProfile.setAvatarId(author.getAvatarId());

        AnswerResponse response = new AnswerResponse();
        response.setId(domainAnswer.getId());
        response.setQuestionId(domainAnswer.getQuestionId());
        response.setPreviousAnswerId(domainAnswer.getPreviousAnswerId());
        response.setAnswer(domainAnswer.getAnswer());
        response.setAuthor(authorProfile);
        response.setCreatedAt(domainAnswer.getCreatedAt());
        // Initialize replies list
        response.setReplies(new ArrayList<>());

        return response;
    }

    public List<QuestionResponse> getQuestions(Integer categoryId) {
        List<Question> domainQuestions;

        if (categoryId != null) {
            // We need to filter manually since there's no repository method for category
            domainQuestions = questionRepository.getAllQuestions().stream()
                    .filter(q -> q.getCategory().equals(com.coactivity.domain.Category.getByIndex(categoryId))) // This assumes getByIndex works correctly for DB IDs
                    .collect(java.util.stream.Collectors.toList());
        } else {
            domainQuestions = questionRepository.getAllQuestions();
        }

        List<QuestionResponse> responses = new ArrayList<>();
        for (Question q : domainQuestions) {
            // Map each domain question to DTO
            var author = q.getOwner(); // Assuming getOwner() returns the User
            var authorProfile = new com.coactivity.controller.dto.response.UserProfileResponse();
            authorProfile.setId(author.getId());
            authorProfile.setEmail(author.getLogin());
            authorProfile.setUsername(author.getUsername());
            authorProfile.setDateOfBirth(author.getDataOfBirth());
            authorProfile.setCity(author.getCity());
            authorProfile.setCountry(author.getCountry());
            authorProfile.setDescription(author.getDescription());
            authorProfile.setAvatarId(author.getAvatarId());

            QuestionResponse resp = new QuestionResponse();
            resp.setId(q.getId());
            resp.setCategory(q.getCategory());
            resp.setQuestion(q.getQuestion());
            resp.setAuthor(authorProfile);
            // We don't set answerCount, createdAt, or lastActivityAt here as per instructions.

            responses.add(resp);
        }

        return responses;
    }

    public QuestionWithAnswersResponse getQuestionWithAnswers(Integer questionId) {
        // 1. Get the question
        Question domainQuestion = questionRepository.getQuestionById(questionId);
        if (domainQuestion == null) {
            // Лучше бросить исключение, чем возвращать null
            throw new IllegalArgumentException("Question not found");
        }

        // 2. Map the question to DTO
        QuestionResponse questionResponse = new QuestionResponse();
        questionResponse.setId(domainQuestion.getId());
        questionResponse.setCategory(domainQuestion.getCategory());
        questionResponse.setQuestion(domainQuestion.getQuestion());
        // Assuming q.getOwner() returns the User object
        var author = domainQuestion.getOwner();
        var authorProfile = new com.coactivity.controller.dto.response.UserProfileResponse();
        authorProfile.setId(author.getId());
        authorProfile.setEmail(author.getLogin());
        authorProfile.setUsername(author.getUsername());
        authorProfile.setDateOfBirth(author.getDataOfBirth());
        authorProfile.setCity(author.getCity());
        authorProfile.setCountry(author.getCountry());
        authorProfile.setDescription(author.getDescription());
        authorProfile.setAvatarId(author.getAvatarId());
        questionResponse.setAuthor(authorProfile);
        // We don't set answerCount, createdAt, or lastActivityAt here as per instructions.

        // Get all answers for the question
        List<Answer> allAnswers = answerRepository.getAnswers(questionId);

        // Build the hierarchical answer structure
        // First, create DTOs for all answers
        Map<Integer, AnswerResponse> answerResponseMap = new HashMap<>();
        for (Answer a : allAnswers) {
            // Fetch the author of the answer from the repository
            var answerAuthor = userRepository.getUserById(a.getOwnerId().getId()); // Assuming ownerId is a User object
            var answerAuthorProfile = new com.coactivity.controller.dto.response.UserProfileResponse();
            answerAuthorProfile.setId(answerAuthor.getId());
            answerAuthorProfile.setEmail(answerAuthor.getLogin());
            answerAuthorProfile.setUsername(answerAuthor.getUsername());
            answerAuthorProfile.setDateOfBirth(answerAuthor.getDataOfBirth());
            answerAuthorProfile.setCity(answerAuthor.getCity());
            answerAuthorProfile.setCountry(answerAuthor.getCountry());
            answerAuthorProfile.setDescription(answerAuthor.getDescription());
            answerAuthorProfile.setAvatarId(answerAuthor.getAvatarId());

            AnswerResponse answerResp = new AnswerResponse();
            answerResp.setId(a.getId());
            answerResp.setQuestionId(a.getQuestionId());
            answerResp.setPreviousAnswerId(a.getPreviousAnswerId());
            answerResp.setAnswer(a.getAnswer());
            answerResp.setAuthor(answerAuthorProfile);
            answerResp.setCreatedAt(a.getCreatedAt());
            answerResp.setReplies(new ArrayList<>()); // Initialize replies list

            answerResponseMap.put(answerResp.getId(), answerResp);
        }

        // Now build the tree: top-level answers and nested replies
        List<AnswerResponse> topLevelAnswers = new ArrayList<>();
        for (AnswerResponse answerResp : answerResponseMap.values()) {
            Integer parentId = answerResp.getPreviousAnswerId();
            if (parentId == null) {
                // This is a top-level answer
                topLevelAnswers.add(answerResp);
            } else {
                // This is a nested answer, add it to the parent's replies
                AnswerResponse parent = answerResponseMap.get(parentId);
                if (parent != null) { // Just in case
                    parent.getReplies().add(answerResp);
                }
            }
        }

        // Create and return the final response
        return new QuestionWithAnswersResponse(questionResponse, topLevelAnswers);
    }
}