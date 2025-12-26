package com.plateforme.educational.service;

import com.plateforme.educational.entity.*;
import com.plateforme.educational.repository.QuizAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service d'IA Agentique
 * Superviseur intelligent qui contrôle et adapte la génération de quiz
 * Utilise maintenant le QuizGeneratorAgent pour une génération plus intelligente
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AIAgentService {

    private final RAGService ragService;
    private final LLMService llmService;
    private final QuizGeneratorAgent quizGeneratorAgent;
    private final QuizAttemptRepository quizAttemptRepository;

    // Seuils de performance
    private static final double EXCELLENT_SCORE = 90.0;
    private static final double GOOD_SCORE = 75.0;
    private static final double PASSING_SCORE = 70.0;
    private static final double POOR_SCORE = 50.0;

    /**
     * Génère un quiz adapté en analysant l'historique de l'étudiant
     * Utilise le QuizGeneratorAgent pour une génération intelligente
     */
    public QuizAttempt generateAdaptiveQuiz(User student, Course course) {
        log.info("AI Agent: Generating adaptive intelligent quiz for student {} on course {}", 
                student.getUsername(), course.getTitle());

        // Étape 1: Analyser l'historique de l'étudiant
        StudentPerformanceAnalysis analysis = analyzeStudentPerformance(student, course);
        
        // Étape 2: Décider des paramètres du quiz
        QuizParameters parameters = decideQuizParameters(analysis);
        
        // Étape 3: Générer les questions via l'agent intelligent
        log.info("Using QuizGeneratorAgent for adaptive question generation");
        
        List<QuizGeneratorAgent.GeneratedQuestion> intelligentQuestions = 
            quizGeneratorAgent.generateQuestions(
                course, 
                parameters.getDifficulty().name(), 
                parameters.getNumberOfQuestions()
            );
        
        // Étape 4: Créer la tentative de quiz
        QuizAttempt attempt = createQuizAttemptFromAgent(student, course, intelligentQuestions, parameters);
        
        // Étape 5: Enregistrer la décision de l'agent
        attempt.setAgentDecision(buildAgentDecision(analysis, parameters));
        
        return quizAttemptRepository.save(attempt);
    }

    /**
     * Génère un quiz avec les paramètres choisis par l'étudiant
     * Utilise le QuizGeneratorAgent pour une génération intelligente basée sur le contenu réel
     */
    public QuizAttempt generateCustomQuiz(User student, Course course, String difficulty, int numQuestions) {
        log.info("AI Agent: Generating intelligent quiz for student {} on course {} with difficulty {} and {} questions", 
                student.getUsername(), course.getTitle(), difficulty, numQuestions);

        // Convertir la difficulté
        QuizAttempt.DifficultyLevel diffLevel;
        try {
            diffLevel = QuizAttempt.DifficultyLevel.valueOf(difficulty.toUpperCase());
        } catch (Exception e) {
            diffLevel = QuizAttempt.DifficultyLevel.BEGINNER;
        }

        // Paramètres personnalisés
        QuizParameters parameters = new QuizParameters();
        parameters.setDifficulty(diffLevel);
        parameters.setNumberOfQuestions(numQuestions);
        parameters.setContextChunks(20);

        // *** UTILISER LE NOUVEL AGENT INTELLIGENT ***
        log.info("Using QuizGeneratorAgent for intelligent question generation");
        
        List<QuizGeneratorAgent.GeneratedQuestion> intelligentQuestions = 
            quizGeneratorAgent.generateQuestions(course, difficulty, numQuestions);
        
        log.info("Generated {} intelligent questions from course content", intelligentQuestions.size());

        // Créer la tentative de quiz avec les questions intelligentes
        QuizAttempt attempt = createQuizAttemptFromAgent(student, course, intelligentQuestions, parameters);
        
        // Enregistrer le choix de l'étudiant
        attempt.setAgentDecision("Quiz intelligent généré - Niveau: " + difficulty + 
                                  ", Questions: " + numQuestions + 
                                  " (Agent: QuizGeneratorAgent)");

        return quizAttemptRepository.save(attempt);
    }

    /**
     * Évalue les résultats et décide de la suite pédagogique
     */
    public String evaluateAndDecide(QuizAttempt attempt) {
        log.info("AI Agent: Evaluating quiz attempt {}", attempt.getId());
        
        // Calculer le score
        int correctAnswers = (int) attempt.getQuestions().stream()
                .filter(QuizQuestion::isCorrect)
                .count();
        
        attempt.setCorrectAnswers(correctAnswers);
        attempt.calculateScore();
        
        // Décision basée sur le score
        String recommendation = makeRecommendation(attempt);
        attempt.setRecommendedAction(recommendation);
        
        quizAttemptRepository.save(attempt);
        
        return recommendation;
    }

    /**
     * Analyse les performances passées de l'étudiant
     */
    private StudentPerformanceAnalysis analyzeStudentPerformance(User student, Course course) {
        List<QuizAttempt> previousAttempts = quizAttemptRepository
                .findByStudentAndCourseOrderByAttemptDateDesc(student, course);
        
        StudentPerformanceAnalysis analysis = new StudentPerformanceAnalysis();
        analysis.setTotalAttempts(previousAttempts.size());
        
        if (previousAttempts.isEmpty()) {
            analysis.setAverageScore(0.0);
            analysis.setCurrentLevel(QuizAttempt.DifficultyLevel.BEGINNER);
            analysis.setFirstAttempt(true);
            return analysis;
        }
        
        // Calculer la moyenne
        double averageScore = previousAttempts.stream()
                .mapToDouble(QuizAttempt::getScore)
                .average()
                .orElse(0.0);
        analysis.setAverageScore(averageScore);
        
        // Analyser la progression
        if (previousAttempts.size() >= 2) {
            double recentScore = previousAttempts.get(0).getScore();
            double previousScore = previousAttempts.get(1).getScore();
            analysis.setProgressing(recentScore > previousScore);
        }
        
        // Déterminer le niveau actuel
        QuizAttempt lastAttempt = previousAttempts.get(0);
        analysis.setCurrentLevel(lastAttempt.getDifficulty());
        analysis.setLastScore(lastAttempt.getScore());
        
        return analysis;
    }

    /**
     * Décide des paramètres du quiz en fonction de l'analyse
     */
    private QuizParameters decideQuizParameters(StudentPerformanceAnalysis analysis) {
        QuizParameters params = new QuizParameters();
        
        if (analysis.isFirstAttempt()) {
            // Premier quiz: niveau débutant, 5 questions
            params.setDifficulty(QuizAttempt.DifficultyLevel.BEGINNER);
            params.setNumberOfQuestions(5);
            params.setContextChunks(5);
            log.info("AI Agent Decision: First attempt - Starting with BEGINNER level");
        } else {
            // Adapter selon les performances
            double avgScore = analysis.getAverageScore();
            
            if (avgScore >= EXCELLENT_SCORE && analysis.isProgressing()) {
                // Excellent performance: augmenter la difficulté
                params.setDifficulty(QuizAttempt.DifficultyLevel.ADVANCED);
                params.setNumberOfQuestions(8);
                params.setContextChunks(10);
                log.info("AI Agent Decision: Excellent performance - Upgrading to ADVANCED");
            } else if (avgScore >= GOOD_SCORE) {
                // Bonne performance: niveau intermédiaire
                params.setDifficulty(QuizAttempt.DifficultyLevel.INTERMEDIATE);
                params.setNumberOfQuestions(6);
                params.setContextChunks(8);
                log.info("AI Agent Decision: Good performance - INTERMEDIATE level");
            } else if (avgScore >= PASSING_SCORE) {
                // Performance correcte: maintenir ou légèrement augmenter
                params.setDifficulty(analysis.getCurrentLevel());
                params.setNumberOfQuestions(5);
                params.setContextChunks(6);
                log.info("AI Agent Decision: Passing performance - Maintaining current level");
            } else {
                // Performance faible: retour au niveau débutant
                params.setDifficulty(QuizAttempt.DifficultyLevel.BEGINNER);
                params.setNumberOfQuestions(5);
                params.setContextChunks(5);
                log.info("AI Agent Decision: Low performance - Back to BEGINNER");
            }
        }
        
        return params;
    }

    /**
     * Crée l'objet QuizAttempt à partir des questions générées
     */
    private QuizAttempt createQuizAttempt(
            User student, 
            Course course, 
            List<LLMService.QuizQuestionDTO> questionDTOs,
            QuizParameters parameters) {
        
        QuizAttempt attempt = new QuizAttempt();
        attempt.setStudent(student);
        attempt.setCourse(course);
        attempt.setDifficulty(parameters.getDifficulty());
        attempt.setTotalQuestions(questionDTOs.size());
        
        for (LLMService.QuizQuestionDTO dto : questionDTOs) {
            QuizQuestion question = new QuizQuestion();
            question.setQuizAttempt(attempt);
            question.setQuestionText(dto.getQuestion());
            question.setOptions(dto.getOptions());
            question.setCorrectAnswerIndex(dto.getCorrectAnswerIndex());
            question.setExplanation(dto.getExplanation());
            
            attempt.getQuestions().add(question);
        }
        
        return attempt;
    }

    /**
     * Crée l'objet QuizAttempt à partir des questions générées par l'agent intelligent
     */
    private QuizAttempt createQuizAttemptFromAgent(
            User student, 
            Course course, 
            List<QuizGeneratorAgent.GeneratedQuestion> generatedQuestions,
            QuizParameters parameters) {
        
        QuizAttempt attempt = new QuizAttempt();
        attempt.setStudent(student);
        attempt.setCourse(course);
        attempt.setDifficulty(parameters.getDifficulty());
        attempt.setTotalQuestions(generatedQuestions.size());
        
        for (QuizGeneratorAgent.GeneratedQuestion gq : generatedQuestions) {
            QuizQuestion question = new QuizQuestion();
            question.setQuizAttempt(attempt);
            question.setQuestionText(gq.question);
            question.setOptions(gq.options);
            question.setCorrectAnswerIndex(gq.correctIndex);
            question.setExplanation(gq.explanation);
            
            attempt.getQuestions().add(question);
        }
        
        return attempt;
    }

    /**
     * Construit un message décrivant la décision de l'agent
     */
    private String buildAgentDecision(StudentPerformanceAnalysis analysis, QuizParameters params) {
        StringBuilder decision = new StringBuilder();
        decision.append("Agent IA - Analyse et décision:\n");
        decision.append("- Tentatives précédentes: ").append(analysis.getTotalAttempts()).append("\n");
        
        if (!analysis.isFirstAttempt()) {
            decision.append("- Score moyen: ").append(String.format("%.1f%%", analysis.getAverageScore())).append("\n");
            decision.append("- Dernier score: ").append(String.format("%.1f%%", analysis.getLastScore())).append("\n");
            decision.append("- Progression: ").append(analysis.isProgressing() ? "Oui" : "Non").append("\n");
        }
        
        decision.append("- Niveau choisi: ").append(params.getDifficulty()).append("\n");
        decision.append("- Nombre de questions: ").append(params.getNumberOfQuestions()).append("\n");
        
        return decision.toString();
    }

    /**
     * Fait une recommandation basée sur les résultats
     */
    private String makeRecommendation(QuizAttempt attempt) {
        double score = attempt.getScore();
        
        if (score >= EXCELLENT_SCORE) {
            return "Excellent travail ! Vous maîtrisez parfaitement ce cours. " +
                   "Vous êtes prêt pour valider cette formation. " +
                   "Tentative suivante: niveau avancé.";
        } else if (score >= GOOD_SCORE) {
            return "Très bien ! Vous avez une bonne compréhension du cours. " +
                   "Continuez à pratiquer pour atteindre l'excellence. " +
                   "Tentative suivante: niveau maintenu ou augmenté.";
        } else if (score >= PASSING_SCORE) {
            return "Bien joué ! Vous avez passé le quiz. " +
                   "Révisez les points moins maîtrisés pour améliorer votre score. " +
                   "Tentative suivante: niveau similaire.";
        } else if (score >= POOR_SCORE) {
            return "Vous avez des bases, mais il faut réviser davantage. " +
                   "Relisez le cours attentivement avant de réessayer. " +
                   "Tentative suivante: retour au niveau débutant.";
        } else {
            return "Le cours nécessite plus de travail. " +
                   "Prenez le temps de bien lire et comprendre le contenu. " +
                   "Tentative suivante: niveau débutant avec support renforcé.";
        }
    }

    /**
     * Classe interne pour l'analyse des performances
     */
    private static class StudentPerformanceAnalysis {
        private int totalAttempts;
        private double averageScore;
        private double lastScore;
        private QuizAttempt.DifficultyLevel currentLevel;
        private boolean progressing;
        private boolean firstAttempt;

        public int getTotalAttempts() { return totalAttempts; }
        public void setTotalAttempts(int totalAttempts) { this.totalAttempts = totalAttempts; }
        
        public double getAverageScore() { return averageScore; }
        public void setAverageScore(double averageScore) { this.averageScore = averageScore; }
        
        public double getLastScore() { return lastScore; }
        public void setLastScore(double lastScore) { this.lastScore = lastScore; }
        
        public QuizAttempt.DifficultyLevel getCurrentLevel() { return currentLevel; }
        public void setCurrentLevel(QuizAttempt.DifficultyLevel currentLevel) { 
            this.currentLevel = currentLevel; 
        }
        
        public boolean isProgressing() { return progressing; }
        public void setProgressing(boolean progressing) { this.progressing = progressing; }
        
        public boolean isFirstAttempt() { return firstAttempt; }
        public void setFirstAttempt(boolean firstAttempt) { this.firstAttempt = firstAttempt; }
    }

    /**
     * Classe interne pour les paramètres du quiz
     */
    private static class QuizParameters {
        private QuizAttempt.DifficultyLevel difficulty;
        private int numberOfQuestions;
        private int contextChunks;

        public QuizAttempt.DifficultyLevel getDifficulty() { return difficulty; }
        public void setDifficulty(QuizAttempt.DifficultyLevel difficulty) { 
            this.difficulty = difficulty; 
        }
        
        public int getNumberOfQuestions() { return numberOfQuestions; }
        public void setNumberOfQuestions(int numberOfQuestions) { 
            this.numberOfQuestions = numberOfQuestions; 
        }
        
        public int getContextChunks() { return contextChunks; }
        public void setContextChunks(int contextChunks) { this.contextChunks = contextChunks; }
    }
}
