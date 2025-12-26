package com.plateforme.educational.service;

import com.plateforme.educational.entity.Course;
import com.plateforme.educational.entity.CourseChunk;
import com.plateforme.educational.entity.CourseFile;
import com.plateforme.educational.repository.CourseChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service RAG (Retrieval-Augmented Generation)
 * Gère l'indexation et la récupération du contenu des cours
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RAGService {

    private final CourseChunkRepository chunkRepository;
    private static final int CHUNK_SIZE = 500; // Nombre de caractères par chunk
    private static final int CHUNK_OVERLAP = 100; // Chevauchement entre chunks

    /**
     * Indexe un cours en le découpant en chunks
     */
    public void indexCourse(Course course, String aggregatedContent) {
        // Afficher la mémoire disponible pour debug
        long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        log.info("MAX MEMORY: {} MB", maxMemory);
        log.info("Starting indexation for course: {}", course.getTitle());
        
        // Supprimer les anciens chunks si existants
        deleteIndex(course);
        
        String content = aggregatedContent;
        if (content == null || content.isEmpty()) {
            log.warn("Course content is empty");
            return;
        }
        
        // VERSION SIMPLE : UN SEUL CHUNK
        // Limiter à 1000 caractères max
        if (content.length() > 1000) {
            content = content.substring(0, 1000);
        }
        
        CourseChunk chunk = new CourseChunk();
        chunk.setCourse(course);
        chunk.setContent(content);
        chunk.setChunkIndex(0);
        chunk.setStartPosition(0);
        chunk.setEndPosition(content.length());
        chunk.setEmbeddingVector("hash-" + content.hashCode());
        
        chunkRepository.save(chunk);
        
        log.info("Indexed 1 chunk for course: {}", course.getTitle());
    }

    /**
     * Supprime l'index d'un cours
     */
    public void deleteIndex(Course course) {
        chunkRepository.deleteByCourse(course);
        log.info("Deleted index for course: {}", course.getTitle());
    }

    /**
     * Récupère les chunks les plus pertinents pour une requête
     */
    public List<CourseChunk> retrieveRelevantChunks(Course course, String query, int topK) {
        List<CourseChunk> allChunks = chunkRepository.findByCourse(course);
        
        // Calculer la similarité (version simplifiée avec recherche de mots-clés)
        return allChunks.stream()
                .map(chunk -> {
                    double score = calculateSimilarity(chunk.getContent(), query);
                    return new ScoredChunk(chunk, score);
                })
                .sorted(Comparator.comparingDouble(ScoredChunk::getScore).reversed())
                .limit(topK)
                .map(ScoredChunk::getChunk)
                .collect(Collectors.toList());
    }

    /**
     * Récupère tout le contexte d'un cours pour la génération de quiz
     */
    public String getCourseContext(Course course, int maxChunks) {
        List<CourseChunk> chunks = chunkRepository.findByCourse(course);
        
        return chunks.stream()
                .sorted(Comparator.comparingInt(CourseChunk::getChunkIndex))
                .limit(maxChunks)
                .map(CourseChunk::getContent)
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * Récupère TOUT le contenu du cours: description + contenu textuel + fichiers
     * Cette méthode assemble toutes les sources de contenu pour une génération de quiz optimale
     */
    public String getFullCourseContext(Course course) {
        StringBuilder fullContext = new StringBuilder();
        
        // 1. Titre et description du cours
        fullContext.append("=== TITRE DU COURS ===\n");
        fullContext.append(course.getTitle()).append("\n\n");
        
        if (course.getDescription() != null && !course.getDescription().isEmpty()) {
            fullContext.append("=== DESCRIPTION ===\n");
            fullContext.append(course.getDescription()).append("\n\n");
        }
        
        // 2. Contenu principal du cours
        if (course.getContent() != null && !course.getContent().isEmpty()) {
            fullContext.append("=== CONTENU PRINCIPAL DU COURS ===\n");
            fullContext.append(course.getContent()).append("\n\n");
        }
        
        // 3. Contenu des chunks indexés
        List<CourseChunk> chunks = chunkRepository.findByCourse(course);
        if (!chunks.isEmpty()) {
            fullContext.append("=== CONTENU INDEXÉ ===\n");
            chunks.stream()
                    .sorted(Comparator.comparingInt(CourseChunk::getChunkIndex))
                    .forEach(chunk -> fullContext.append(chunk.getContent()).append("\n"));
            fullContext.append("\n");
        }
        
        // 4. Contenu des fichiers attachés (texte, PDF parsé, etc.)
        if (course.getFiles() != null && !course.getFiles().isEmpty()) {
            fullContext.append("=== FICHIERS ATTACHÉS ===\n");
            for (CourseFile file : course.getFiles()) {
                String fileContent = extractFileContent(file);
                if (!fileContent.isEmpty()) {
                    fullContext.append("--- ").append(file.getOriginalFilename()).append(" ---\n");
                    fullContext.append(fileContent).append("\n\n");
                }
            }
        }
        
        String result = fullContext.toString();
        log.info("Full course context assembled: {} characters from course '{}'", result.length(), course.getTitle());
        
        return result;
    }

    /**
     * Extrait le contenu textuel d'un fichier
     */
    private String extractFileContent(CourseFile file) {
        if (file == null || file.getStoragePath() == null) {
            return "";
        }
        
        String contentType = file.getContentType();
        
        // Fichiers texte
        if (isTextFile(contentType, file.getOriginalFilename())) {
            try {
                return Files.readString(Paths.get(file.getStoragePath()), StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.warn("Could not read text file: {}", file.getOriginalFilename());
                return "";
            }
        }
        
        // TODO: Ajouter support pour PDF (avec Apache PDFBox)
        // TODO: Ajouter support pour Word (avec Apache POI)
        
        return "";
    }

    /**
     * Vérifie si un fichier est de type texte
     */
    private boolean isTextFile(String contentType, String filename) {
        if (contentType != null) {
            if (contentType.startsWith("text/") || 
                contentType.contains("json") || 
                contentType.contains("xml") ||
                contentType.contains("javascript")) {
                return true;
            }
        }
        
        if (filename != null) {
            String lower = filename.toLowerCase();
            return lower.endsWith(".txt") || lower.endsWith(".md") || 
                   lower.endsWith(".json") || lower.endsWith(".xml") ||
                   lower.endsWith(".html") || lower.endsWith(".java") ||
                   lower.endsWith(".py") || lower.endsWith(".js") ||
                   lower.endsWith(".css") || lower.endsWith(".sql");
        }
        
        return false;
    }

    /**
     * Découpe le texte en chunks avec chevauchement (VERSION ULTRA-SIMPLIFIÉE)
     */
    private List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        
        // Limite de sécurité
        if (text.length() > 2000) {
            text = text.substring(0, 2000);
        }
        
        // Découpage simple sans boucle complexe
        int start = 0;
        int maxChunks = 10; // Limite de sécurité
        int chunkCount = 0;
        
        while (start < text.length() && chunkCount < maxChunks) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
                chunkCount++;
            }
            
            start = end; // Pas de chevauchement pour simplifier
            
            // Sécurité absolue : si on n'avance pas, on sort
            if (end == start) break;
        }
        
        // Si aucun chunk, ajouter le texte entier
        if (chunks.isEmpty()) {
            chunks.add(text);
        }
        
        return chunks;
    }

    /**
     * Génère un embedding simplifié (hash)
     * Dans une vraie implémentation, on utiliserait un modèle d'embedding
     */
    private String generateSimpleEmbedding(String text) {
        return String.valueOf(text.toLowerCase().hashCode());
    }

    /**
     * Calcule une similarité simplifiée basée sur les mots communs
     */
    private double calculateSimilarity(String text, String query) {
        Set<String> textWords = new HashSet<>(Arrays.asList(
                text.toLowerCase().split("\\W+")));
        Set<String> queryWords = new HashSet<>(Arrays.asList(
                query.toLowerCase().split("\\W+")));
        
        long commonWords = queryWords.stream()
                .filter(textWords::contains)
                .count();
        
        if (queryWords.isEmpty()) {
            return 0.0;
        }
        
        return (double) commonWords / queryWords.size();
    }

    /**
     * Classe interne pour associer un score à un chunk
     */
    private static class ScoredChunk {
        private final CourseChunk chunk;
        private final double score;

        public ScoredChunk(CourseChunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }

        public CourseChunk getChunk() {
            return chunk;
        }

        public double getScore() {
            return score;
        }
    }
}
