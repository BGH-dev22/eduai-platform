package com.plateforme.educational.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Représente un fragment de cours indexé pour le système RAG
 */
@Entity
@Table(name = "course_chunks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"course"})
@ToString(exclude = {"course"})
public class CourseChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Integer chunkIndex;

    // Embedding vector (simplified - in production, use vector database)
    @Column(length = 10000)
    private String embeddingVector;

    @Column
    private Integer startPosition;

    @Column
    private Integer endPosition;
}
