package com.plateforme.educational.repository;

import com.plateforme.educational.entity.EvaluationAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationAnswerRepository extends JpaRepository<EvaluationAnswer, Long> {

    List<EvaluationAnswer> findByAttemptIdOrderByQuestionOrderAsc(Long attemptId);
}
