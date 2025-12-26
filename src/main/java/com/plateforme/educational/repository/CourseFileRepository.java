package com.plateforme.educational.repository;

import com.plateforme.educational.entity.Course;
import com.plateforme.educational.entity.CourseFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseFileRepository extends JpaRepository<CourseFile, Long> {
    List<CourseFile> findByCourse(Course course);
}
