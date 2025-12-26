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
@Table(name = "quiz_attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"student", "course", "questions"})
@ToString(exclude = {"student", "course", "questions"})
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private LocalDateTime attemptDate = LocalDateTime.now();

    @Column(nullable = false)
    private Integer totalQuestions;

    @Column(nullable = false)
    private Integer correctAnswers = 0;

    @Column(nullable = false)
    private Double score = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DifficultyLevel difficulty;

    @Column(nullable = false)
    private boolean passed = false;

    @OneToMany(mappedBy = "quizAttempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizQuestion> questions = new ArrayList<>();

    // Métadonnées pour l'IA agentique
    @Column(length = 2000)
    private String agentDecision;

    @Column
    private String recommendedAction;

    public enum DifficultyLevel {
        BEGINNER,
        INTERMEDIATE,
        ADVANCED
    }

    public void calculateScore() {
        if (totalQuestions > 0) {
            this.score = (correctAnswers * 100.0) / totalQuestions;
            this.passed = this.score >= 70.0; // Seuil de validation à 70%
        }
    }
}
