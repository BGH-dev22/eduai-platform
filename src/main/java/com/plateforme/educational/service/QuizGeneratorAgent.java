package com.plateforme.educational.service;

import com.plateforme.educational.entity.Course;
import com.plateforme.educational.entity.CourseFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Agent intelligent de génération de quiz
 * Analyse le contenu du cours et des fichiers pour créer des questions pertinentes
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizGeneratorAgent {

    /**
     * Structure représentant un concept extrait du cours
     */
    public static class ConceptInfo {
        public String term;
        public String definition;
        public String context;
        public String source; // "course" ou nom du fichier
        
        public ConceptInfo(String term, String definition, String context, String source) {
            this.term = term;
            this.definition = definition;
            this.context = context;
            this.source = source;
        }
    }

    /**
     * Structure pour une question générée
     */
    public static class GeneratedQuestion {
        public String question;
        public List<String> options;
        public int correctIndex;
        public String explanation;
        public String difficulty;
        
        public GeneratedQuestion() {
            this.options = new ArrayList<>();
        }
    }

    /**
     * Génère des questions intelligentes basées sur le contenu du cours
     */
    public List<GeneratedQuestion> generateQuestions(Course course, String difficulty, int numQuestions) {
        log.info("Agent de génération: Analyse du cours '{}' pour {} questions de niveau {}", 
                course.getTitle(), numQuestions, difficulty);
        
        // 1. Collecter tout le contenu
        String fullContent = collectAllContent(course);
        log.info("Contenu collecté: {} caractères", fullContent.length());
        
        // 2. Extraire les concepts clés
        List<ConceptInfo> concepts = extractConcepts(fullContent, course.getTitle());
        log.info("Concepts extraits: {}", concepts.size());
        
        // 3. Extraire les faits importants
        List<String> facts = extractFacts(fullContent);
        log.info("Faits extraits: {}", facts.size());
        
        // 4. Générer les questions
        List<GeneratedQuestion> questions = new ArrayList<>();
        Random random = new Random();
        
        int questionsPerType = numQuestions / 4 + 1;
        
        // Questions de définition (basées sur les concepts)
        for (int i = 0; i < questionsPerType && questions.size() < numQuestions && i < concepts.size(); i++) {
            GeneratedQuestion q = createDefinitionQuestion(concepts.get(i), difficulty, random);
            if (q != null) questions.add(q);
        }
        
        // Questions de compréhension (basées sur les faits)
        for (int i = 0; i < questionsPerType && questions.size() < numQuestions && i < facts.size(); i++) {
            GeneratedQuestion q = createFactQuestion(facts.get(i), difficulty, random);
            if (q != null) questions.add(q);
        }
        
        // Questions d'application (niveau intermédiaire+)
        if (!difficulty.equalsIgnoreCase("BEGINNER")) {
            for (int i = 0; i < questionsPerType && questions.size() < numQuestions && i < concepts.size(); i++) {
                GeneratedQuestion q = createApplicationQuestion(concepts.get(i), difficulty, random);
                if (q != null) questions.add(q);
            }
        }
        
        // Questions d'analyse (niveau avancé)
        if (difficulty.equalsIgnoreCase("ADVANCED")) {
            for (int i = 0; i < questionsPerType && questions.size() < numQuestions && i < facts.size(); i++) {
                GeneratedQuestion q = createAnalysisQuestion(facts, i, difficulty, random);
                if (q != null) questions.add(q);
            }
        }
        
        // Compléter avec des questions supplémentaires si nécessaire
        while (questions.size() < numQuestions && !concepts.isEmpty()) {
            int idx = random.nextInt(concepts.size());
            GeneratedQuestion q = createDefinitionQuestion(concepts.get(idx), difficulty, random);
            if (q != null) questions.add(q);
        }
        
        // Mélanger les questions
        Collections.shuffle(questions);
        
        log.info("Questions générées: {}", questions.size());
        return questions.stream().limit(numQuestions).collect(Collectors.toList());
    }

    /**
     * Collecte tout le contenu du cours (texte + fichiers)
     */
    private String collectAllContent(Course course) {
        StringBuilder content = new StringBuilder();
        
        // Titre et description
        content.append(course.getTitle()).append("\n\n");
        if (course.getDescription() != null) {
            content.append(course.getDescription()).append("\n\n");
        }
        
        // Contenu principal
        if (course.getContent() != null) {
            content.append(course.getContent()).append("\n\n");
        }
        
        // Fichiers attachés
        if (course.getFiles() != null) {
            for (CourseFile file : course.getFiles()) {
                String fileContent = extractFileContent(file);
                if (!fileContent.isEmpty()) {
                    content.append("\n--- Fichier: ").append(file.getOriginalFilename()).append(" ---\n");
                    content.append(fileContent).append("\n");
                }
            }
        }
        
        return content.toString();
    }

    /**
     * Extrait le contenu d'un fichier (texte ou PDF)
     */
    private String extractFileContent(CourseFile file) {
        if (file == null || file.getStoragePath() == null) {
            return "";
        }
        
        String filename = file.getOriginalFilename().toLowerCase();
        String contentType = file.getContentType();
        
        try {
            // Fichiers texte
            if (isTextFile(filename, contentType)) {
                return Files.readString(Paths.get(file.getStoragePath()), StandardCharsets.UTF_8);
            }
            
            // PDF - extraction basique
            if (filename.endsWith(".pdf") || (contentType != null && contentType.contains("pdf"))) {
                return extractPdfContent(file.getStoragePath());
            }
            
        } catch (Exception e) {
            log.warn("Erreur lecture fichier {}: {}", file.getOriginalFilename(), e.getMessage());
        }
        
        return "";
    }

    private boolean isTextFile(String filename, String contentType) {
        if (contentType != null && contentType.startsWith("text/")) return true;
        return filename.endsWith(".txt") || filename.endsWith(".md") || 
               filename.endsWith(".json") || filename.endsWith(".xml") ||
               filename.endsWith(".html") || filename.endsWith(".java") ||
               filename.endsWith(".py") || filename.endsWith(".js");
    }

    /**
     * Extraction du contenu PDF avec Apache PDFBox
     */
    private String extractPdfContent(String path) {
        try {
            File pdfFile = new File(path);
            if (!pdfFile.exists()) {
                log.warn("PDF file not found: {}", path);
                return "";
            }
            
            try (PDDocument document = Loader.loadPDF(pdfFile)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                
                // Nettoyer le texte
                text = text.replaceAll("\\s+", " ").trim();
                
                log.info("Extracted {} characters from PDF: {}", text.length(), path);
                return text;
            }
        } catch (IOException e) {
            log.warn("Error extracting PDF content from {}: {}", path, e.getMessage());
            return "";
        }
    }

    /**
     * Extrait les concepts clés du contenu (termes + définitions)
     */
    private List<ConceptInfo> extractConcepts(String content, String courseTitle) {
        List<ConceptInfo> concepts = new ArrayList<>();
        Set<String> seenTerms = new HashSet<>();
        
        // Patterns pour détecter les définitions
        String[] definitionPatterns = {
            "(.{3,50})\\s+(?:est|sont|désigne|représente|signifie|correspond à|se définit comme)\\s+(.{10,200})",
            "(?:Un|Une|Le|La|Les)\\s+(.{3,40})\\s+(?:est|sont)\\s+(.{10,200})",
            "(.{3,50})\\s*:\\s*(.{10,200})",
            "(?:On appelle|On définit)\\s+(.{3,50})\\s+(.{10,200})",
            "(.{3,50})\\s*=\\s*(.{10,200})"
        };
        
        String[] sentences = content.split("[.!?\\n]+");
        
        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.length() < 20 || sentence.length() > 500) continue;
            
            for (String patternStr : definitionPatterns) {
                try {
                    Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(sentence);
                    
                    if (matcher.find() && matcher.groupCount() >= 2) {
                        String term = cleanTerm(matcher.group(1));
                        String definition = matcher.group(2).trim();
                        
                        if (term.length() >= 3 && term.length() <= 50 && 
                            definition.length() >= 10 && !seenTerms.contains(term.toLowerCase())) {
                            
                            seenTerms.add(term.toLowerCase());
                            concepts.add(new ConceptInfo(term, definition, sentence, "course"));
                        }
                    }
                } catch (Exception e) {
                    // Ignorer les erreurs de regex
                }
            }
        }
        
        // Extraire aussi les termes importants (mots en majuscules, termes techniques)
        Pattern termPattern = Pattern.compile("\\b([A-Z][a-zA-ZÀ-ÿ]{2,})\\b");
        Matcher termMatcher = termPattern.matcher(content);
        
        while (termMatcher.find() && concepts.size() < 30) {
            String term = termMatcher.group(1);
            if (term.length() >= 4 && !seenTerms.contains(term.toLowerCase()) && 
                !isCommonWord(term)) {
                seenTerms.add(term.toLowerCase());
                // Trouver le contexte autour du terme
                int start = Math.max(0, termMatcher.start() - 50);
                int end = Math.min(content.length(), termMatcher.end() + 150);
                String context = content.substring(start, end).trim();
                concepts.add(new ConceptInfo(term, context, context, "course"));
            }
        }
        
        log.debug("Concepts extraits: {}", concepts.stream().map(c -> c.term).collect(Collectors.joining(", ")));
        return concepts;
    }

    private String cleanTerm(String term) {
        return term.replaceAll("^(Un|Une|Le|La|Les|L')\\s*", "").trim();
    }

    private boolean isCommonWord(String word) {
        Set<String> common = Set.of(
            "Dans", "Pour", "Avec", "Sans", "Cette", "Cela", "Donc", "Mais", "Puis",
            "Quel", "Quoi", "Comment", "Pourquoi", "Quand", "Alors", "Ainsi", "Aussi",
            "Bien", "Très", "Plus", "Moins", "Tout", "Tous", "Toute", "Toutes"
        );
        return common.contains(word);
    }

    /**
     * Extrait les faits importants du contenu
     */
    private List<String> extractFacts(String content) {
        List<String> facts = new ArrayList<>();
        Set<String> seenFacts = new HashSet<>();
        
        String[] sentences = content.split("[.!?]+");
        
        for (String sentence : sentences) {
            sentence = sentence.trim();
            
            // Filtrer les phrases de bonne longueur contenant des informations
            if (sentence.length() >= 30 && sentence.length() <= 300) {
                // Vérifier que c'est une phrase informative
                if (containsInformation(sentence) && !seenFacts.contains(sentence.toLowerCase())) {
                    seenFacts.add(sentence.toLowerCase());
                    facts.add(sentence);
                }
            }
        }
        
        // Limiter et mélanger
        Collections.shuffle(facts);
        return facts.stream().limit(20).collect(Collectors.toList());
    }

    private boolean containsInformation(String sentence) {
        // Mots indicateurs d'information
        String[] indicators = {
            "permet", "est", "sont", "peut", "doit", "représente", "contient",
            "utilise", "produit", "génère", "crée", "fonctionne", "sert",
            "comprend", "inclut", "nécessite", "requiert", "implique",
            "consiste", "caractérise", "définit", "décrit", "explique"
        };
        
        String lower = sentence.toLowerCase();
        for (String indicator : indicators) {
            if (lower.contains(indicator)) return true;
        }
        return false;
    }

    /**
     * Crée une question de définition
     */
    private GeneratedQuestion createDefinitionQuestion(ConceptInfo concept, String difficulty, Random random) {
        GeneratedQuestion q = new GeneratedQuestion();
        q.difficulty = difficulty;
        
        String term = concept.term;
        String definition = concept.definition;
        
        // Formulation de la question selon le niveau
        switch (difficulty.toUpperCase()) {
            case "ADVANCED":
                q.question = "Selon le cours, quelle affirmation décrit le mieux \"" + term + "\" ?";
                break;
            case "INTERMEDIATE":
                q.question = "Comment le cours définit-il \"" + term + "\" ?";
                break;
            default: // BEGINNER
                q.question = "Que signifie \"" + term + "\" dans ce cours ?";
                break;
        }
        
        // Bonne réponse (tronquée si trop longue)
        String correctAnswer = truncate(definition, 120);
        q.options.add(correctAnswer);
        q.correctIndex = 0;
        
        // Générer des mauvaises réponses plausibles
        q.options.add(generateWrongDefinition(term, 1, random));
        q.options.add(generateWrongDefinition(term, 2, random));
        q.options.add(generateWrongDefinition(term, 3, random));
        
        // Mélanger les options
        shuffleOptionsKeepingCorrect(q, random);
        
        q.explanation = "La bonne réponse est basée sur le contenu du cours: " + truncate(concept.context, 150);
        
        return q;
    }

    /**
     * Crée une question basée sur un fait
     */
    private GeneratedQuestion createFactQuestion(String fact, String difficulty, Random random) {
        GeneratedQuestion q = new GeneratedQuestion();
        q.difficulty = difficulty;
        
        switch (difficulty.toUpperCase()) {
            case "ADVANCED":
                q.question = "Quelle affirmation du cours est exacte ?";
                break;
            case "INTERMEDIATE":
                q.question = "Selon le contenu étudié, laquelle de ces affirmations est correcte ?";
                break;
            default:
                q.question = "Laquelle de ces informations est mentionnée dans le cours ?";
                break;
        }
        
        q.options.add(truncate(fact, 120));
        q.correctIndex = 0;
        
        // Mauvaises réponses
        q.options.add(generateWrongFact(fact, 1));
        q.options.add(generateWrongFact(fact, 2));
        q.options.add("Cette information n'est pas abordée dans le cours");
        
        shuffleOptionsKeepingCorrect(q, random);
        
        q.explanation = "Cette information provient directement du cours.";
        
        return q;
    }

    /**
     * Crée une question d'application
     */
    private GeneratedQuestion createApplicationQuestion(ConceptInfo concept, String difficulty, Random random) {
        GeneratedQuestion q = new GeneratedQuestion();
        q.difficulty = difficulty;
        
        q.question = "Comment pourrait-on appliquer le concept de \"" + concept.term + "\" selon le cours ?";
        
        q.options.add("En suivant les principes décrits: " + truncate(concept.definition, 80));
        q.correctIndex = 0;
        
        q.options.add("Ce concept n'a pas d'application pratique mentionnée");
        q.options.add("L'application nécessite des connaissances non couvertes par le cours");
        q.options.add("Le cours ne propose pas d'application pour ce concept");
        
        shuffleOptionsKeepingCorrect(q, random);
        
        q.explanation = "Le cours explique: " + truncate(concept.context, 150);
        
        return q;
    }

    /**
     * Crée une question d'analyse
     */
    private GeneratedQuestion createAnalysisQuestion(List<String> facts, int index, String difficulty, Random random) {
        if (facts.isEmpty()) return null;
        
        GeneratedQuestion q = new GeneratedQuestion();
        q.difficulty = difficulty;
        
        String fact = facts.get(index % facts.size());
        
        q.question = "En analysant le contenu du cours, quelle conclusion peut-on tirer ?";
        
        q.options.add(truncate(fact, 120));
        q.correctIndex = 0;
        
        q.options.add("Cette conclusion n'est pas soutenue par le cours");
        q.options.add("Le cours suggère une interprétation différente");
        q.options.add("L'analyse du cours mène à une autre conclusion");
        
        shuffleOptionsKeepingCorrect(q, random);
        
        q.explanation = "Cette conclusion est basée sur le contenu du cours.";
        
        return q;
    }

    /**
     * Génère une mauvaise définition plausible
     */
    private String generateWrongDefinition(String term, int variant, Random random) {
        String[] templates = {
            "Un concept non lié à " + term + " dans ce contexte",
            "Une notion différente qui ne correspond pas à " + term,
            "Un élément sans rapport avec la définition de " + term,
            "Une interprétation incorrecte de " + term,
            term + " n'est pas défini de cette manière dans le cours",
            "Cette description ne correspond pas à " + term
        };
        return templates[(variant + random.nextInt(templates.length)) % templates.length];
    }

    /**
     * Génère un faux fait plausible
     */
    private String generateWrongFact(String fact, int variant) {
        // Inverser ou modifier légèrement le fait
        String modified = fact;
        
        if (variant == 1) {
            modified = modified.replace(" est ", " n'est pas ");
            modified = modified.replace(" sont ", " ne sont pas ");
            modified = modified.replace(" peut ", " ne peut pas ");
            if (modified.equals(fact)) {
                modified = "Le contraire de ce qui est affirmé dans le cours";
            }
        } else {
            modified = "Une information qui n'apparaît pas dans le cours étudié";
        }
        
        return truncate(modified, 120);
    }

    /**
     * Mélange les options tout en gardant trace de la bonne réponse
     */
    private void shuffleOptionsKeepingCorrect(GeneratedQuestion q, Random random) {
        String correctAnswer = q.options.get(q.correctIndex);
        Collections.shuffle(q.options, random);
        q.correctIndex = q.options.indexOf(correctAnswer);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        text = text.trim().replaceAll("\\s+", " ");
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
