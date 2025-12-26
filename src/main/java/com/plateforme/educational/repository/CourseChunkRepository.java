package com.plateforme.educational.repository;

import com.plateforme.educational.entity.Course;
import com.plateforme.educational.entity.CourseChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseChunkRepository extends JpaRepository<CourseChunk, Long> {
    
    List<CourseChunk> findByCourse(Course course);
    
    List<CourseChunk> findByCourseId(Long courseId);
    
    void deleteByCourse(Course course);
}
