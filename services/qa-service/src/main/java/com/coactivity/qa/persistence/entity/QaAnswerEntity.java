package com.coactivity.qa.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "answers")
public class QaAnswerEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "question_id", nullable = false)
  private QaQuestionEntity question;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "prev_ans_id")
  private QaAnswerEntity previousAnswer;

  @Column(name = "answer")
  private String answer;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner", nullable = false)
  private QaUserEntity owner;

  @Column(name = "created_at")
  private Instant createdAt;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public QaQuestionEntity getQuestion() {
    return question;
  }

  public void setQuestion(QaQuestionEntity question) {
    this.question = question;
  }

  public QaAnswerEntity getPreviousAnswer() {
    return previousAnswer;
  }

  public void setPreviousAnswer(QaAnswerEntity previousAnswer) {
    this.previousAnswer = previousAnswer;
  }

  public String getAnswer() {
    return answer;
  }

  public void setAnswer(String answer) {
    this.answer = answer;
  }

  public QaUserEntity getOwner() {
    return owner;
  }

  public void setOwner(QaUserEntity owner) {
    this.owner = owner;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
