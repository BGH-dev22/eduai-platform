package com.plateforme.educational.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evaluation_attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"evaluation", "student", "answers"})
@ToString(exclude = {"evaluation", "student", "answers"})
public class EvaluationAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "evaluation_id", nullable = false)
    private Evaluation evaluation;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column
    private LocalDateTime completedAt;

    @Column(nullable = false)
    private Double score = 0.0;

    @Column(nullable = false)
    private Integer correctAnswers = 0;

    @Column(nullable = false)
    private Integer totalQuestions = 0;

    @Column(nullable = false)
    private boolean passed = false;

    @Column(nullable = false)
    private boolean completed = false;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionOrder ASC")
    private List<EvaluationAnswer> answers = new ArrayList<>();

    public void calculateScore() {
        if (totalQuestions > 0) {
            this.score = (double) correctAnswers / totalQuestions * 100;
            this.passed = this.score >= evaluation.getPassingScore();
        }
    }

    public void addAnswer(EvaluationAnswer answer) {
        answers.add(answer);
        answer.setAttempt(this);
    }
}
