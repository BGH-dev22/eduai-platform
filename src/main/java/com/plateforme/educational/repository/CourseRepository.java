package com.plateforme.educational.repository;

import com.plateforme.educational.entity.Course;
import com.plateforme.educational.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    
    List<Course> findByPublishedTrue();
    
    List<Course> findByCreator(User creator);
    
    @Query("SELECT c FROM Course c JOIN c.enrolledStudents s WHERE s.id = :studentId")
    List<Course> findCoursesByStudentId(@Param("studentId") Long studentId);
    
    @Query("SELECT c FROM Course c WHERE c.published = true AND c.indexed = true")
    List<Course> findPublishedAndIndexedCourses();
}
