package com.plateforme.educational.repository;

import com.plateforme.educational.entity.EvaluationQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationQuestionRepository extends JpaRepository<EvaluationQuestion, Long> {

    List<EvaluationQuestion> findByEvaluationIdOrderByQuestionOrderAsc(Long evaluationId);

    void deleteByEvaluationId(Long evaluationId);
}
