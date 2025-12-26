package com.plateforme.educational.repository;

import com.plateforme.educational.entity.QuizAttempt;
import com.plateforme.educational.entity.User;
import com.plateforme.educational.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    
    List<QuizAttempt> findByStudentOrderByAttemptDateDesc(User student);
    
    List<QuizAttempt> findByCourseOrderByAttemptDateDesc(Course course);
    
    List<QuizAttempt> findByStudentAndCourseOrderByAttemptDateDesc(User student, Course course);
    
    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.student.id = :studentId AND qa.course.id = :courseId ORDER BY qa.attemptDate DESC")
    List<QuizAttempt> findByStudentIdAndCourseIdOrderByAttemptDateDesc(
        @Param("studentId") Long studentId, 
        @Param("courseId") Long courseId
    );
    
    @Query("SELECT AVG(qa.score) FROM QuizAttempt qa WHERE qa.student.id = :studentId AND qa.course.id = :courseId")
    Double getAverageScoreByStudentAndCourse(
        @Param("studentId") Long studentId, 
        @Param("courseId") Long courseId
    );
}
