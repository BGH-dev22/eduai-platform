package com.plateforme.educational.repository;

import com.plateforme.educational.entity.Course;
import com.plateforme.educational.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    List<Evaluation> findByCourse(Course course);

    List<Evaluation> findByCourseId(Long courseId);

    List<Evaluation> findByActiveTrue();

    List<Evaluation> findByCourseAndActiveTrue(Course course);

    @Query("SELECT e FROM Evaluation e WHERE e.course.id = :courseId AND e.active = true " +
           "AND (e.startDate IS NULL OR e.startDate <= :now) " +
           "AND (e.endDate IS NULL OR e.endDate >= :now)")
    List<Evaluation> findAvailableEvaluations(@Param("courseId") Long courseId, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(e) FROM Evaluation e WHERE e.course.id = :courseId")
    long countByCourseId(@Param("courseId") Long courseId);
}
