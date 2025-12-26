package com.plateforme.educational.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service LLM (Large Language Model)
 * Gère l'interaction avec le modèle de langage pour la génération de quiz
 * 
 * Note: Cette implémentation est une version simplifiée/simulée
 * Dans un environnement de production, intégrer OpenAI API ou Ollama
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LLMService {

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    private final Gson gson = new Gson();

    /**
     * Génère des questions de quiz basées sur le contexte fourni
     */
    public List<QuizQuestionDTO> generateQuizQuestions(
            String courseContext, 
            int numberOfQuestions, 
            String difficulty) {
        
        log.info("Generating {} questions with difficulty: {}", numberOfQuestions, difficulty);
        
        // Construction du prompt pour le LLM
        String prompt = buildQuizPrompt(courseContext, numberOfQuestions, difficulty);
        
        // Appel au LLM (version simulée)
        String llmResponse = callLLM(prompt);
        
        // Parser la réponse
        return parseQuizResponse(llmResponse, numberOfQuestions);
    }

    /**
     * Construit le prompt pour le LLM
     */
    private String buildQuizPrompt(String courseContext, int numberOfQuestions, String difficulty) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Tu es un assistant pédagogique expert. ");
        prompt.append("À partir du contenu de cours suivant, génère exactement ");
        prompt.append(numberOfQuestions);
        prompt.append(" questions de type QCM (questions à choix multiples).\n\n");
        prompt.append("Niveau de difficulté: ").append(difficulty).append("\n\n");
        prompt.append("CONTENU DU COURS:\n");
        prompt.append(courseContext);
        prompt.append("\n\n");
        prompt.append("INSTRUCTIONS IMPORTANTES:\n");
        prompt.append("1. Chaque question doit avoir exactement 4 options de réponse\n");
        prompt.append("2. Une seule option est correcte\n");
        prompt.append("3. Les options incorrectes doivent être plausibles\n");
        prompt.append("4. Fournis une explication pour chaque question\n");
        prompt.append("5. Base-toi UNIQUEMENT sur le contenu du cours fourni\n");
        prompt.append("6. Ne génère PAS de questions sur des connaissances externes\n\n");
        prompt.append("FORMAT DE RÉPONSE (JSON):\n");
        prompt.append("[\n");
        prompt.append("  {\n");
        prompt.append("    \"question\": \"Texte de la question?\",\n");
        prompt.append("    \"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"],\n");
        prompt.append("    \"correctAnswerIndex\": 0,\n");
        prompt.append("    \"explanation\": \"Explication de la réponse correcte\"\n");
        prompt.append("  }\n");
        prompt.append("]\n");
        
        return prompt.toString();
    }

    /**
     * Appel au LLM (version simulée)
     * IMPORTANT: Dans un environnement de production, remplacer par un vrai appel API
     */
    private String callLLM(String prompt) {
        log.warn("Using simulated LLM response. Configure OpenAI API key for real LLM integration.");
        
        // Simulation de questions (à remplacer par un vrai appel LLM)
        return generateSimulatedQuestions(prompt);
    }

    /**
     * Génère des questions basées sur le contenu réel du cours
     */
    private String generateSimulatedQuestions(String prompt) {
        // Extraire le nombre de questions demandé
        int numberOfQuestions = extractNumberOfQuestions(prompt);
        
        // Extraire le contenu du cours du prompt
        String courseContent = extractCourseContent(prompt);
        String difficulty = extractDifficulty(prompt);
        
        // Générer des questions basées sur le contenu réel
        List<QuestionTemplate> generatedQuestions = generateQuestionsFromContent(courseContent, numberOfQuestions, difficulty);
        
        JsonArray questions = new JsonArray();
        
        for (QuestionTemplate qt : generatedQuestions) {
            JsonObject question = new JsonObject();
            question.addProperty("question", qt.question);
            
            JsonArray options = new JsonArray();
            for (String option : qt.options) {
                options.add(option);
            }
            
            question.add("options", options);
            question.addProperty("correctAnswerIndex", qt.correctIndex);
            question.addProperty("explanation", qt.explanation);
            
            questions.add(question);
        }
        
        return gson.toJson(questions);
    }
    
    /**
     * Extrait le contenu du cours depuis le prompt
     */
    private String extractCourseContent(String prompt) {
        try {
            int startIndex = prompt.indexOf("CONTENU DU COURS:");
            int endIndex = prompt.indexOf("INSTRUCTIONS IMPORTANTES:");
            
            if (startIndex != -1 && endIndex != -1) {
                return prompt.substring(startIndex + 17, endIndex).trim();
            }
        } catch (Exception e) {
            log.warn("Could not extract course content from prompt");
        }
        return "";
    }
    
    /**
     * Extrait la difficulté du prompt
     */
    private String extractDifficulty(String prompt) {
        try {
            int startIndex = prompt.indexOf("Niveau de difficulté: ");
            if (startIndex != -1) {
                int endIndex = prompt.indexOf("\n", startIndex);
                if (endIndex != -1) {
                    return prompt.substring(startIndex + 22, endIndex).trim();
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract difficulty from prompt");
        }
        return "BEGINNER";
    }
    
    /**
     * Génère des questions à partir du contenu réel du cours
     */
    private List<QuestionTemplate> generateQuestionsFromContent(String content, int count, String difficulty) {
        List<QuestionTemplate> questions = new ArrayList<>();
        
        if (content == null || content.isEmpty()) {
            // Fallback si pas de contenu
            for (int i = 0; i < count; i++) {
                questions.add(createFallbackQuestion(i + 1));
            }
            return questions;
        }
        
        // Extraire les concepts clés du contenu
        List<String> concepts = extractKeyConcepts(content);
        List<String> sentences = extractImportantSentences(content);
        List<String> definitions = extractDefinitions(content);
        List<String> keyTerms = extractKeyTerms(content);
        
        log.info("Extracted {} concepts, {} sentences, {} definitions, {} key terms from course content", 
                concepts.size(), sentences.size(), definitions.size(), keyTerms.size());
        
        // Générer des questions variées basées sur le contenu
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < count; i++) {
            QuestionTemplate qt;
            
            // Varier les types de questions selon le niveau
            int questionType;
            switch (difficulty.toUpperCase()) {
                case "ADVANCED":
                    questionType = i % 5; // Plus de variété pour les questions avancées
                    break;
                case "INTERMEDIATE":
                    questionType = i % 4;
                    break;
                default: // BEGINNER
                    questionType = i % 3; // Questions plus simples
                    break;
            }
            
            switch (questionType) {
                case 0:
                    qt = generateDefinitionQuestion(definitions, sentences, concepts, i, random, difficulty);
                    break;
                case 1:
                    qt = generateConceptQuestion(concepts, content, keyTerms, i, random, difficulty);
                    break;
                case 2:
                    qt = generateComprehensionQuestion(content, sentences, i, random, difficulty);
                    break;
                case 3:
                    qt = generateApplicationQuestion(concepts, definitions, i, random, difficulty);
                    break;
                case 4:
                    qt = generateAnalysisQuestion(sentences, concepts, i, random, difficulty);
                    break;
                default:
                    qt = generateComprehensionQuestion(content, sentences, i, random, difficulty);
                    break;
            }
            
            questions.add(qt);
        }
        
        return questions;
    }

    /**
     * Extrait les définitions du contenu
     */
    private List<String> extractDefinitions(String content) {
        List<String> definitions = new ArrayList<>();
        
        if (content == null || content.isEmpty()) {
            return definitions;
        }
        
        String[] definitionPatterns = {
            "est défini comme", "se définit comme", "est un", "est une",
            "représente", "désigne", "signifie", "correspond à",
            "on appelle", "on définit", "c'est", "il s'agit de"
        };
        
        String[] lines = content.split("[.!?\\n]");
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.length() > 30 && trimmedLine.length() < 300) {
                for (String pattern : definitionPatterns) {
                    if (trimmedLine.toLowerCase().contains(pattern)) {
                        definitions.add(trimmedLine);
                        break;
                    }
                }
            }
        }
        
        return definitions;
    }

    /**
     * Extrait les termes clés du contenu
     */
    private List<String> extractKeyTerms(String content) {
        List<String> terms = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        
        if (content == null || content.isEmpty()) {
            return terms;
        }
        
        // Rechercher les mots en majuscules, entre guillemets, ou techniques
        String[] words = content.split("\\s+");
        
        for (int i = 0; i < words.length; i++) {
            String word = words[i].replaceAll("[^a-zA-ZÀ-ÿ0-9]", "");
            
            // Skip empty words
            if (word.isEmpty()) continue;
            
            if (word.length() > 3) {
                // Mots commençant par majuscule (termes techniques)
                if (Character.isUpperCase(word.charAt(0)) && !seen.contains(word.toLowerCase())) {
                    terms.add(word);
                    seen.add(word.toLowerCase());
                }
            }
            
            // Expressions de 2-3 mots importantes
            if (i < words.length - 1 && word.length() > 0) {
                String nextWord = words[i + 1].replaceAll("[^a-zA-ZÀ-ÿ]", "");
                if (!nextWord.isEmpty()) {
                    String twoWords = word + " " + nextWord;
                    if (twoWords.length() > 8 && !seen.contains(twoWords.toLowerCase())) {
                        if (Character.isUpperCase(word.charAt(0))) {
                            terms.add(twoWords);
                            seen.add(twoWords.toLowerCase());
                        }
                    }
                }
            }
        }
        
        return terms;
    }
    
    /**
     * Extrait les concepts clés du contenu
     */
    private List<String> extractKeyConcepts(String content) {
        List<String> concepts = new ArrayList<>();
        
        if (content == null || content.isEmpty()) {
            return concepts;
        }
        
        // Mots-clés techniques courants à rechercher
        String[] technicalPatterns = {
            "est un", "est une", "permet de", "consiste à", "définit", 
            "représente", "signifie", "désigne", "correspond à"
        };
        
        String[] lines = content.split("[.!?\\n]");
        
        for (String line : lines) {
            line = line.trim();
            if (line.length() > 20 && line.length() < 200) {
                for (String pattern : technicalPatterns) {
                    if (line.toLowerCase().contains(pattern)) {
                        concepts.add(line);
                        break;
                    }
                }
            }
        }
        
        // Extraire aussi les mots en majuscules ou entre guillemets
        String[] words = content.split("\\s+");
        for (String word : words) {
            if (word == null || word.isEmpty()) continue;
            
            if (word.length() > 3 && (Character.isUpperCase(word.charAt(0)) || 
                word.startsWith("\"") || word.startsWith("'"))) {
                String cleanWord = word.replaceAll("[^a-zA-ZÀ-ÿ]", "");
                if (cleanWord.length() > 3 && !concepts.contains(cleanWord)) {
                    concepts.add(cleanWord);
                }
            }
        }
        
        return concepts;
    }
    
    /**
     * Extrait les phrases importantes du contenu
     */
    private List<String> extractImportantSentences(String content) {
        List<String> sentences = new ArrayList<>();
        
        if (content == null || content.isEmpty()) {
            return sentences;
        }
        
        String[] parts = content.split("[.!?]");
        
        for (String part : parts) {
            String sentence = part.trim();
            if (sentence.length() > 30 && sentence.length() < 250) {
                sentences.add(sentence);
            }
        }
        
        return sentences;
    }
    
    /**
     * Génère une question de type définition
     */
    private QuestionTemplate generateDefinitionQuestion(List<String> definitions, List<String> sentences, 
            List<String> concepts, int index, java.util.Random random, String difficulty) {
        QuestionTemplate qt = new QuestionTemplate();
        
        // Utiliser les définitions si disponibles
        if (!definitions.isEmpty()) {
            String definition = definitions.get(index % definitions.size());
            
            // Extraire le terme défini
            String term = extractTermFromDefinition(definition);
            
            switch (difficulty.toUpperCase()) {
                case "ADVANCED":
                    qt.question = "Analysez la définition suivante et identifiez l'affirmation correcte concernant \"" + term + "\":";
                    qt.options = new String[]{
                        truncate(definition, 100),
                        "Cette définition est incomplète selon le cours",
                        "Le terme désigne un concept différent dans ce contexte",
                        "La définition présentée dans le cours est plus restrictive"
                    };
                    break;
                case "INTERMEDIATE":
                    qt.question = "Selon le cours, comment est défini(e) \"" + term + "\" ?";
                    qt.options = new String[]{
                        truncate(definition, 100),
                        "Le cours ne fournit pas de définition précise",
                        "C'est un terme utilisé différemment dans ce contexte",
                        "La définition varie selon les sources citées"
                    };
                    break;
                default: // BEGINNER
                    qt.question = "Que signifie \"" + term + "\" selon le cours ?";
                    qt.options = new String[]{
                        truncate(definition, 100),
                        "Ce terme n'est pas défini dans le cours",
                        "C'est un concept non abordé",
                        "Aucune définition n'est donnée"
                    };
                    break;
            }
            qt.correctIndex = 0;
            qt.explanation = "Le cours définit ce terme ainsi: " + truncate(definition, 150);
        } else if (!sentences.isEmpty()) {
            String sentence = sentences.get(index % sentences.size());
            qt.question = "Quelle affirmation correspond au contenu du cours ?";
            qt.options = new String[]{
                truncate(sentence, 100),
                "Cette information n'apparaît pas dans le cours",
                "Le cours présente une perspective différente",
                "Ce point n'est pas abordé"
            };
            qt.correctIndex = 0;
            qt.explanation = "Cette information provient directement du cours.";
        } else {
            qt = createFallbackQuestion(index + 1);
        }
        
        return qt;
    }
    
    /**
     * Extrait le terme principal d'une définition
     */
    private String extractTermFromDefinition(String definition) {
        if (definition == null || definition.isEmpty()) {
            return "ce terme";
        }
        
        String[] patterns = {"est défini comme", "se définit comme", "est un", "est une",
            "représente", "désigne", "signifie", "correspond à"};
        
        for (String pattern : patterns) {
            int idx = definition.toLowerCase().indexOf(pattern);
            if (idx > 0) {
                return truncate(definition.substring(0, idx).trim(), 40);
            }
        }
        
        // Prendre les premiers mots
        String[] words = definition.split("\\s+");
        if (words.length == 0 || words[0].isEmpty()) {
            return "ce terme";
        }
        if (words.length > 2) {
            return words[0] + " " + words[1];
        }
        return words[0];
    }
    
    /**
     * Génère une question sur un concept
     */
    private QuestionTemplate generateConceptQuestion(List<String> concepts, String content, 
            List<String> keyTerms, int index, java.util.Random random, String difficulty) {
        QuestionTemplate qt = new QuestionTemplate();
        
        String term = !keyTerms.isEmpty() ? keyTerms.get(index % keyTerms.size()) : 
                      (!concepts.isEmpty() ? concepts.get(index % concepts.size()) : "ce concept");
        
        // Trouver un contexte autour du terme
        String context = findContextForTerm(content, term);
        
        switch (difficulty.toUpperCase()) {
            case "ADVANCED":
                qt.question = "Dans le contexte du cours, quelle analyse est correcte concernant \"" + term + "\" ?";
                qt.options = new String[]{
                    truncate(context, 100),
                    "Le cours présente ce concept sous un angle critique",
                    "Cette notion est remise en question dans le cours",
                    "L'approche du cours est plus nuancée sur ce point"
                };
                break;
            case "INTERMEDIATE":
                qt.question = "Comment le cours aborde-t-il le concept de \"" + term + "\" ?";
                qt.options = new String[]{
                    truncate(context, 100),
                    "Ce concept est mentionné mais non développé",
                    "Le cours ne traite pas directement de ce sujet",
                    "Cette notion est présentée différemment"
                };
                break;
            default: // BEGINNER
                qt.question = "Que dit le cours à propos de \"" + term + "\" ?";
                qt.options = new String[]{
                    truncate(context, 100),
                    "Ce sujet n'est pas abordé dans le cours",
                    "Le cours ne mentionne pas cet élément",
                    "Aucune information disponible sur ce point"
                };
                break;
        }
        qt.correctIndex = 0;
        qt.explanation = "Le cours présente cette information concernant " + term + ".";
        
        return qt;
    }
    
    /**
     * Trouve le contexte autour d'un terme dans le contenu
     */
    private String findContextForTerm(String content, String term) {
        if (content == null || content.isEmpty()) {
            return "Information du cours";
        }
        if (term == null || term.isEmpty()) {
            return truncate(content, 120);
        }
        
        int idx = content.toLowerCase().indexOf(term.toLowerCase());
        if (idx >= 0) {
            int start = Math.max(0, idx - 20);
            int end = Math.min(content.length(), idx + term.length() + 100);
            return content.substring(start, end).trim();
        }
        
        // Fallback: prendre une portion aléatoire
        if (content.length() > 100) {
            int start = Math.min(content.length() - 100, Math.abs(term.hashCode()) % (content.length() / 2));
            return content.substring(start, Math.min(start + 120, content.length())).trim();
        }
        return content;
    }
    
    /**
     * Génère une question de compréhension
     */
    private QuestionTemplate generateComprehensionQuestion(String content, List<String> sentences, 
            int index, java.util.Random random, String difficulty) {
        QuestionTemplate qt = new QuestionTemplate();
        
        String sentence = !sentences.isEmpty() ? sentences.get(index % sentences.size()) : 
                          truncate(content, 150);
        
        switch (difficulty.toUpperCase()) {
            case "ADVANCED":
                qt.question = "En analysant le contenu du cours, quelle conclusion peut-on tirer ?";
                qt.options = new String[]{
                    truncate(sentence, 100),
                    "Le cours suggère une interprétation différente",
                    "Cette conclusion n'est pas soutenue par le cours",
                    "L'analyse du cours mène à une autre conclusion"
                };
                break;
            case "INTERMEDIATE":
                qt.question = "Selon le cours, quelle affirmation est exacte ?";
                qt.options = new String[]{
                    truncate(sentence, 100),
                    "Le cours nuance cette affirmation",
                    "Cette information n'est pas confirmée par le cours",
                    "Le cours présente une vision différente"
                };
                break;
            default: // BEGINNER
                qt.question = "Laquelle de ces informations est présente dans le cours ?";
                qt.options = new String[]{
                    truncate(sentence, 100),
                    "Cette information n'est pas dans le cours",
                    "Le cours ne parle pas de cela",
                    "Ce point n'est pas abordé"
                };
                break;
        }
        qt.correctIndex = 0;
        qt.explanation = "Cette information se trouve dans le contenu du cours.";
        
        return qt;
    }
    
    /**
     * Génère une question d'application (niveau avancé)
     */
    private QuestionTemplate generateApplicationQuestion(List<String> concepts, List<String> definitions,
            int index, java.util.Random random, String difficulty) {
        QuestionTemplate qt = new QuestionTemplate();
        
        String concept = !concepts.isEmpty() ? concepts.get(index % concepts.size()) : "ce concept";
        String definition = !definitions.isEmpty() ? definitions.get(index % definitions.size()) : concept;
        
        qt.question = "Comment pourrait-on appliquer le concept de \"" + truncate(concept, 30) + "\" selon le cours ?";
        qt.options = new String[]{
            "En suivant les principes énoncés: " + truncate(definition, 70),
            "Cette application n'est pas suggérée dans le cours",
            "Le cours ne propose pas d'application pratique",
            "L'application diffère de ce qui est présenté"
        };
        qt.correctIndex = 0;
        qt.explanation = "Le cours présente des éléments permettant cette application.";
        
        return qt;
    }
    
    /**
     * Génère une question d'analyse (niveau avancé)
     */
    private QuestionTemplate generateAnalysisQuestion(List<String> sentences, List<String> concepts,
            int index, java.util.Random random, String difficulty) {
        QuestionTemplate qt = new QuestionTemplate();
        
        String sentence = !sentences.isEmpty() ? sentences.get(index % sentences.size()) : "";
        String concept = !concepts.isEmpty() ? concepts.get(index % concepts.size()) : "ce sujet";
        
        qt.question = "Quelle analyse du cours concernant \"" + truncate(concept, 30) + "\" est la plus précise ?";
        qt.options = new String[]{
            truncate(sentence, 100),
            "Le cours adopte une perspective critique sur ce point",
            "L'analyse du cours est plus superficielle",
            "Le cours ne fournit pas d'analyse approfondie"
        };
        qt.correctIndex = 0;
        qt.explanation = "Cette analyse correspond au traitement du sujet dans le cours.";
        
        return qt;
    }
    
    /**
     * Crée une question de secours
     */
    private QuestionTemplate createFallbackQuestion(int index) {
        QuestionTemplate qt = new QuestionTemplate();
        qt.question = "Question " + index + " sur le contenu du cours:";
        qt.options = new String[]{
            "Réponse basée sur le cours",
            "Option alternative",
            "Autre possibilité",
            "Dernière option"
        };
        qt.correctIndex = 0;
        qt.explanation = "Cette question est générée automatiquement.";
        return qt;
    }
    
    /**
     * Tronque une chaîne à une longueur maximale
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Classe interne pour stocker les templates de questions
     */
    private static class QuestionTemplate {
        String question;
        String[] options;
        int correctIndex;
        String explanation;
    }

    /**
     * Parse la réponse du LLM
     */
    private List<QuizQuestionDTO> parseQuizResponse(String response, int expectedQuestions) {
        List<QuizQuestionDTO> questions = new ArrayList<>();
        
        try {
            JsonArray jsonArray = gson.fromJson(response, JsonArray.class);
            
            for (int i = 0; i < Math.min(jsonArray.size(), expectedQuestions); i++) {
                JsonObject jsonQuestion = jsonArray.get(i).getAsJsonObject();
                
                QuizQuestionDTO dto = new QuizQuestionDTO();
                dto.setQuestion(jsonQuestion.get("question").getAsString());
                dto.setCorrectAnswerIndex(jsonQuestion.get("correctAnswerIndex").getAsInt());
                dto.setExplanation(jsonQuestion.get("explanation").getAsString());
                
                List<String> options = new ArrayList<>();
                JsonArray jsonOptions = jsonQuestion.getAsJsonArray("options");
                for (int j = 0; j < jsonOptions.size(); j++) {
                    options.add(jsonOptions.get(j).getAsString());
                }
                dto.setOptions(options);
                
                questions.add(dto);
            }
        } catch (Exception e) {
            log.error("Error parsing LLM response: {}", e.getMessage());
            // En cas d'erreur, retourner des questions par défaut
            return generateFallbackQuestions(expectedQuestions);
        }
        
        return questions;
    }

    /**
     * Extrait le nombre de questions du prompt
     */
    private int extractNumberOfQuestions(String prompt) {
        try {
            String[] parts = prompt.split("génère exactement ");
            if (parts.length > 1) {
                String numberPart = parts[1].split(" ")[0];
                return Integer.parseInt(numberPart);
            }
        } catch (Exception e) {
            log.warn("Could not extract number of questions, using default: 5");
        }
        return 5;
    }

    /**
     * Génère des questions de secours en cas d'erreur
     */
    private List<QuizQuestionDTO> generateFallbackQuestions(int count) {
        List<QuizQuestionDTO> questions = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            QuizQuestionDTO dto = new QuizQuestionDTO();
            dto.setQuestion("Question de secours " + (i + 1));
            dto.setOptions(List.of("Option A", "Option B", "Option C", "Option D"));
            dto.setCorrectAnswerIndex(0);
            dto.setExplanation("Explication de la réponse");
            questions.add(dto);
        }
        
        return questions;
    }

    /**
     * DTO pour les questions de quiz
     */
    public static class QuizQuestionDTO {
        private String question;
        private List<String> options;
        private Integer correctAnswerIndex;
        private String explanation;

        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        
        public List<String> getOptions() { return options; }
        public void setOptions(List<String> options) { this.options = options; }
        
        public Integer getCorrectAnswerIndex() { return correctAnswerIndex; }
        public void setCorrectAnswerIndex(Integer correctAnswerIndex) { 
            this.correctAnswerIndex = correctAnswerIndex; 
        }
        
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
    }
}
