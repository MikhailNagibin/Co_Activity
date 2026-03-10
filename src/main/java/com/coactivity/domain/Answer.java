package com.coactivity.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "answers")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Answer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "question_id")
  private Integer questionId;

  @Column(name = "previous_answer_id")
  private Integer previousAnswerId;

  @Column(name = "answer", length = 1000)
  private String answer;

  @ManyToOne
  @JoinColumn(name = "owner_id")
  private User ownerId;

  @Column(name = "created_at")
  private Instant createdAt;
}
