package com.plateforme.educational.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"quizAttempt"})
@ToString(exclude = {"quizAttempt"})
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "quiz_attempt_id", nullable = false)
    private QuizAttempt quizAttempt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @ElementCollection
    @CollectionTable(name = "quiz_options", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "option_text", columnDefinition = "TEXT")
    @OrderColumn(name = "option_order")
    private List<String> options = new ArrayList<>();

    @Column(nullable = false)
    private Integer correctAnswerIndex;

    @Column
    private Integer studentAnswerIndex;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(nullable = false)
    private boolean correct = false;

    public void checkAnswer() {
        if (studentAnswerIndex != null) {
            this.correct = studentAnswerIndex.equals(correctAnswerIndex);
        }
    }
}
