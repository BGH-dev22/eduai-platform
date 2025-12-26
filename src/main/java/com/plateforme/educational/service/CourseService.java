package com.plateforme.educational.service;

import com.plateforme.educational.entity.Course;
import com.plateforme.educational.entity.User;
import com.plateforme.educational.repository.CourseRepository;
import com.plateforme.educational.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final RAGService ragService;
    private final CourseFileService courseFileService;

    public Course createCourse(Course course, User creator, List<MultipartFile> files) {
        course.setCreator(creator);
        Course saved = courseRepository.save(course);
        courseFileService.storeFiles(saved, files);
        return saved;
    }

    public Course updateCourse(Long id, Course courseDetails, List<MultipartFile> files) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        
        course.setTitle(courseDetails.getTitle());
        course.setDescription(courseDetails.getDescription());
        course.setContent(courseDetails.getContent());
        course.setVideoLinks(courseDetails.getVideoLinks());
        
        // Si le contenu change, il faut ré-indexer
        if (course.isIndexed()) {
            course.setIndexed(false);
        }

        courseFileService.storeFiles(course, files);
        
        return courseRepository.save(course);
    }

    public void deleteCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        
        // Supprimer l'indexation RAG
        if (course.isIndexed()) {
            ragService.deleteIndex(course);
        }

        // Supprimer les pièces jointes
        courseFileService.deleteAllFilesForCourse(course);
        
        courseRepository.deleteById(id);
    }

    public Optional<Course> findById(Long id) {
        return courseRepository.findById(id);
    }

    public List<Course> findAll() {
        return courseRepository.findAll();
    }

    public List<Course> findPublishedCourses() {
        return courseRepository.findByPublishedTrue();
    }

    public List<Course> findCoursesByStudent(Long studentId) {
        return courseRepository.findCoursesByStudentId(studentId);
    }

    public Course publishCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        
        if (course.getContent() == null || course.getContent().isEmpty()) {
            throw new IllegalStateException("Cannot publish course without content");
        }
        
        course.setPublished(true);
        return courseRepository.save(course);
    }

    public Course unpublishCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        
        course.setPublished(false);
        return courseRepository.save(course);
    }

    public Course enrollStudent(Long courseId, Long studentId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        
        if (student.getRole() != User.Role.STUDENT) {
            throw new IllegalArgumentException("User is not a student");
        }
        
        course.getEnrolledStudents().add(student);
        student.getEnrolledCourses().add(course);
        
        return courseRepository.save(course);
    }

    public Course unenrollStudent(Long courseId, Long studentId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        
        course.getEnrolledStudents().remove(student);
        student.getEnrolledCourses().remove(course);
        
        return courseRepository.save(course);
    }

    public boolean isStudentEnrolled(Long courseId, Long studentId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        
        return course.getEnrolledStudents().stream()
                .anyMatch(student -> student.getId().equals(studentId));
    }

    public Course indexCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        
        if (!course.isPublished()) {
            throw new IllegalStateException("Cannot index unpublished course");
        }
        
        String attachmentsText = courseFileService.concatenateTextFiles(course);
        String combinedContent = course.getContent();
        if (!attachmentsText.isBlank()) {
            combinedContent = combinedContent + "\n\n" + attachmentsText;
        }

        ragService.indexCourse(course, combinedContent);
        course.setIndexed(true);
        
        return courseRepository.save(course);
    }
}
