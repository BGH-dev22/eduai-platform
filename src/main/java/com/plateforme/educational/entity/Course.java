package com.plateforme.educational.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.plateforme.educational.entity.CourseFile;

@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"enrolledStudents", "chunks", "quizAttempts", "creator", "evaluations", "files"})
@ToString(exclude = {"enrolledStudents", "chunks", "quizAttempts", "creator", "evaluations", "files"})
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 5000)
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean published = false;

    @Column(nullable = false)
    private boolean indexed = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "creator_id")
    private User creator;

    @ManyToMany(mappedBy = "enrolledCourses")
    private Set<User> enrolledStudents = new HashSet<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    private Set<CourseChunk> chunks = new HashSet<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    private Set<QuizAttempt> quizAttempts = new HashSet<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CourseFile> files = new HashSet<>();

    @Column(columnDefinition = "TEXT")
    private String videoLinks; // Liens vidéos séparés par des retours à la ligne

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    private Set<Evaluation> evaluations = new HashSet<>();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
