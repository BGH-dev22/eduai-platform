package com.plateforme.educational.service;

import com.plateforme.educational.entity.Course;
import com.plateforme.educational.entity.CourseFile;
import com.plateforme.educational.repository.CourseFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CourseFileService {

    private final CourseFileRepository courseFileRepository;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private Path uploadRoot;

    @PostConstruct
    void init() {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadRoot);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le répertoire racine d'upload", e);
        }
    }

    public void storeFiles(Course course, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        
        // Filtrer les fichiers vides
        List<MultipartFile> validFiles = files.stream()
                .filter(f -> f != null && !f.isEmpty() && f.getOriginalFilename() != null && !f.getOriginalFilename().isEmpty())
                .toList();
        
        if (validFiles.isEmpty()) {
            return;
        }

        Path courseDir = uploadRoot.resolve("courses").resolve(String.valueOf(course.getId()));
        try {
            Files.createDirectories(courseDir);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le répertoire d'upload", e);
        }

        for (MultipartFile file : validFiles) {
            String originalName = StringUtils.cleanPath(file.getOriginalFilename());
            String storedName = UUID.randomUUID() + "-" + originalName;
            Path destination = courseDir.resolve(storedName);

            try {
                Files.copy(file.getInputStream(), destination);
            } catch (IOException e) {
                throw new RuntimeException("Erreur lors de l'upload du fichier " + originalName, e);
            }

            CourseFile courseFile = new CourseFile();
            courseFile.setCourse(course);
            courseFile.setOriginalFilename(originalName);
            courseFile.setStoredFilename(storedName);
            courseFile.setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
            courseFile.setSize(file.getSize());
            courseFile.setStoragePath(destination.toString());

            course.getFiles().add(courseFile);
            courseFileRepository.save(courseFile);
        }
    }

    public void deleteAllFilesForCourse(Course course) {
        List<CourseFile> files = courseFileRepository.findByCourse(course);
        for (CourseFile file : files) {
            try {
                Files.deleteIfExists(Paths.get(file.getStoragePath()));
            } catch (IOException e) {
                log.warn("Impossible de supprimer le fichier {}", file.getStoragePath(), e);
            }
        }
        courseFileRepository.deleteAll(files);
        course.getFiles().clear();
    }

    public Resource loadAsResource(Long fileId) {
        CourseFile file = courseFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("Fichier introuvable"));
        try {
            Path path = Paths.get(file.getStoragePath());
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
        } catch (Exception e) {
            log.error("Erreur lors du chargement du fichier {}", fileId, e);
        }
        throw new IllegalArgumentException("Fichier illisible");
    }

    public CourseFile getFile(Long fileId) {
        return courseFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("Fichier introuvable"));
    }

    public String concatenateTextFiles(Course course) {
        return course.getFiles().stream()
                .filter(file -> isTextType(file.getContentType()))
                .map(file -> readFileSafe(file.getStoragePath()))
                .filter(content -> !content.isBlank())
                .collect(Collectors.joining("\n\n"));
    }

    private boolean isTextType(String contentType) {
        if (contentType == null) {
            return false;
        }
        return contentType.startsWith("text/") || contentType.contains("json") || contentType.contains("xml");
    }

    private String readFileSafe(String path) {
        try {
            return Files.readString(Paths.get(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Lecture impossible pour le fichier {}", path, e);
            return "";
        }
    }
}
