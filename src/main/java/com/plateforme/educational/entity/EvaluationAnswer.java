package com.plateforme.educational.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "evaluation_answers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"attempt", "question"})
@ToString(exclude = {"attempt", "question"})
public class EvaluationAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "attempt_id", nullable = false)
    private EvaluationAttempt attempt;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private EvaluationQuestion question;

    @Column(nullable = false)
    private Integer questionOrder;

    @Column
    private Integer selectedAnswerIndex;

    @Column(nullable = false)
    private boolean correct = false;

    public void checkAnswer() {
        if (selectedAnswerIndex != null && question != null) {
            this.correct = selectedAnswerIndex.equals(question.getCorrectAnswerIndex());
        }
    }
}
