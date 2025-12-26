package com.plateforme.educational.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "evaluations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"questions", "attempts", "course", "creator"})
@ToString(exclude = {"questions", "attempts", "course", "creator"})
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne
    @JoinColumn(name = "creator_id")
    private User creator;

    @Column(nullable = false)
    private Double passingScore = 70.0; // Seuil de réussite par défaut: 70%

    @Column(nullable = false)
    private Integer duration = 30; // Durée en minutes

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime startDate;

    @Column
    private LocalDateTime endDate;

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionOrder ASC")
    private List<EvaluationQuestion> questions = new ArrayList<>();

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL)
    private Set<EvaluationAttempt> attempts = new HashSet<>();

    public void addQuestion(EvaluationQuestion question) {
        questions.add(question);
        question.setEvaluation(this);
        question.setQuestionOrder(questions.size());
    }

    public void removeQuestion(EvaluationQuestion question) {
        questions.remove(question);
        question.setEvaluation(null);
        // Réorganiser les ordres
        for (int i = 0; i < questions.size(); i++) {
            questions.get(i).setQuestionOrder(i + 1);
        }
    }

    public int getTotalQuestions() {
        return questions.size();
    }
}
