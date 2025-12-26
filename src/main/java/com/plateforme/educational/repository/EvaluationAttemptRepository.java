package com.plateforme.educational.repository;

import com.plateforme.educational.entity.Evaluation;
import com.plateforme.educational.entity.EvaluationAttempt;
import com.plateforme.educational.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationAttemptRepository extends JpaRepository<EvaluationAttempt, Long> {

    List<EvaluationAttempt> findByStudentAndEvaluation(User student, Evaluation evaluation);

    List<EvaluationAttempt> findByStudent(User student);

    List<EvaluationAttempt> findByEvaluation(Evaluation evaluation);

    List<EvaluationAttempt> findByEvaluationIdOrderByScoreDesc(Long evaluationId);

    Optional<EvaluationAttempt> findByStudentAndEvaluationAndCompletedFalse(User student, Evaluation evaluation);

    @Query("SELECT ea FROM EvaluationAttempt ea WHERE ea.student.id = :studentId AND ea.completed = true ORDER BY ea.completedAt DESC")
    List<EvaluationAttempt> findCompletedAttemptsByStudent(@Param("studentId") Long studentId);

    @Query("SELECT COUNT(ea) FROM EvaluationAttempt ea WHERE ea.evaluation.id = :evaluationId AND ea.passed = true")
    long countPassedAttempts(@Param("evaluationId") Long evaluationId);

    @Query("SELECT COUNT(ea) FROM EvaluationAttempt ea WHERE ea.evaluation.id = :evaluationId AND ea.completed = true")
    long countCompletedAttempts(@Param("evaluationId") Long evaluationId);

    @Query("SELECT AVG(ea.score) FROM EvaluationAttempt ea WHERE ea.evaluation.id = :evaluationId AND ea.completed = true")
    Double getAverageScore(@Param("evaluationId") Long evaluationId);

    boolean existsByStudentAndEvaluationAndPassedTrue(User student, Evaluation evaluation);

    List<EvaluationAttempt> findByStudentAndPassedTrue(User student);

    @Query("SELECT ea FROM EvaluationAttempt ea WHERE ea.student = :student AND ea.passed = true ORDER BY ea.completedAt DESC")
    List<EvaluationAttempt> findPassedAttemptsByStudentOrderByDate(@Param("student") User student);

    @Query("SELECT ea FROM EvaluationAttempt ea WHERE ea.completed = true ORDER BY ea.completedAt DESC")
    List<EvaluationAttempt> findAllByCompletedTrueOrderByCompletedAtDesc();
}
