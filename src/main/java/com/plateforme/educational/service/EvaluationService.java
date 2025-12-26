package com.plateforme.educational.service;

import com.plateforme.educational.entity.*;
import com.plateforme.educational.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final EvaluationQuestionRepository questionRepository;
    private final EvaluationAttemptRepository attemptRepository;
    private final EvaluationAnswerRepository answerRepository;
    private final CourseRepository courseRepository;

    // ==================== Gestion des évaluations ====================

    public Evaluation createEvaluation(Evaluation evaluation, Long courseId, User creator) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));
        
        evaluation.setCourse(course);
        evaluation.setCreator(creator);
        evaluation.setCreatedAt(LocalDateTime.now());
        
        return evaluationRepository.save(evaluation);
    }

    public Evaluation updateEvaluation(Long id, Evaluation updatedEvaluation) {
        Evaluation evaluation = findById(id)
                .orElseThrow(() -> new RuntimeException("Évaluation non trouvée"));
        
        evaluation.setTitle(updatedEvaluation.getTitle());
        evaluation.setDescription(updatedEvaluation.getDescription());
        evaluation.setPassingScore(updatedEvaluation.getPassingScore());
        evaluation.setDuration(updatedEvaluation.getDuration());
        evaluation.setActive(updatedEvaluation.isActive());
        evaluation.setStartDate(updatedEvaluation.getStartDate());
        evaluation.setEndDate(updatedEvaluation.getEndDate());
        
        return evaluationRepository.save(evaluation);
    }

    public void deleteEvaluation(Long id) {
        evaluationRepository.deleteById(id);
    }

    public Optional<Evaluation> findById(Long id) {
        return evaluationRepository.findById(id);
    }

    public List<Evaluation> findAll() {
        return evaluationRepository.findAll();
    }

    public List<Evaluation> findByCourseId(Long courseId) {
        return evaluationRepository.findByCourseId(courseId);
    }

    public List<Evaluation> findAvailableForStudent(Long courseId) {
        return evaluationRepository.findAvailableEvaluations(courseId, LocalDateTime.now());
    }

    // ==================== Gestion des questions ====================

    public EvaluationQuestion addQuestion(Long evaluationId, EvaluationQuestion question) {
        Evaluation evaluation = findById(evaluationId)
                .orElseThrow(() -> new RuntimeException("Évaluation non trouvée"));
        
        evaluation.addQuestion(question);
        evaluationRepository.save(evaluation);
        
        return question;
    }

    public void addQuestions(Long evaluationId, List<EvaluationQuestion> questions) {
        Evaluation evaluation = findById(evaluationId)
                .orElseThrow(() -> new RuntimeException("Évaluation non trouvée"));
        
        for (EvaluationQuestion question : questions) {
            evaluation.addQuestion(question);
        }
        
        evaluationRepository.save(evaluation);
    }

    public void updateQuestion(Long questionId, EvaluationQuestion updatedQuestion) {
        EvaluationQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question non trouvée"));
        
        question.setQuestionText(updatedQuestion.getQuestionText());
        question.setOptions(updatedQuestion.getOptions());
        question.setCorrectAnswerIndex(updatedQuestion.getCorrectAnswerIndex());
        question.setExplanation(updatedQuestion.getExplanation());
        question.setPoints(updatedQuestion.getPoints());
        
        questionRepository.save(question);
    }

    public void deleteQuestion(Long questionId) {
        EvaluationQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question non trouvée"));
        
        Evaluation evaluation = question.getEvaluation();
        evaluation.removeQuestion(question);
        
        evaluationRepository.save(evaluation);
    }

    public List<EvaluationQuestion> getQuestions(Long evaluationId) {
        return questionRepository.findByEvaluationIdOrderByQuestionOrderAsc(evaluationId);
    }

    // ==================== Gestion des tentatives d'évaluation ====================

    public EvaluationAttempt startAttempt(Long evaluationId, User student) {
        Evaluation evaluation = findById(evaluationId)
                .orElseThrow(() -> new RuntimeException("Évaluation non trouvée"));
        
        // Vérifier si l'étudiant a déjà une tentative en cours
        Optional<EvaluationAttempt> existingAttempt = attemptRepository
                .findByStudentAndEvaluationAndCompletedFalse(student, evaluation);
        
        if (existingAttempt.isPresent()) {
            return existingAttempt.get();
        }
        
        EvaluationAttempt attempt = new EvaluationAttempt();
        attempt.setEvaluation(evaluation);
        attempt.setStudent(student);
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setTotalQuestions(evaluation.getTotalQuestions());
        
        return attemptRepository.save(attempt);
    }

    public EvaluationAttempt submitAttempt(Long attemptId, Map<Long, Integer> answers) {
        EvaluationAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Tentative non trouvée"));
        
        if (attempt.isCompleted()) {
            throw new RuntimeException("Cette évaluation a déjà été soumise");
        }
        
        int correctCount = 0;
        List<EvaluationQuestion> questions = attempt.getEvaluation().getQuestions();
        
        for (EvaluationQuestion question : questions) {
            EvaluationAnswer answer = new EvaluationAnswer();
            answer.setQuestion(question);
            answer.setQuestionOrder(question.getQuestionOrder());
            
            Integer selectedIndex = answers.get(question.getId());
            answer.setSelectedAnswerIndex(selectedIndex);
            answer.checkAnswer();
            
            if (answer.isCorrect()) {
                correctCount++;
            }
            
            attempt.addAnswer(answer);
        }
        
        attempt.setCorrectAnswers(correctCount);
        attempt.setCompletedAt(LocalDateTime.now());
        attempt.setCompleted(true);
        attempt.calculateScore();
        
        log.info("Évaluation soumise - Étudiant: {}, Score: {}%, Réussi: {}", 
                attempt.getStudent().getUsername(), 
                String.format("%.1f", attempt.getScore()),
                attempt.isPassed());
        
        return attemptRepository.save(attempt);
    }

    public Optional<EvaluationAttempt> findAttemptById(Long attemptId) {
        return attemptRepository.findById(attemptId);
    }

    public List<EvaluationAttempt> getAttemptsByEvaluation(Long evaluationId) {
        return attemptRepository.findByEvaluationIdOrderByScoreDesc(evaluationId);
    }

    public List<EvaluationAttempt> getAttemptsByStudent(User student) {
        return attemptRepository.findCompletedAttemptsByStudent(student.getId());
    }

    public boolean hasStudentPassed(User student, Evaluation evaluation) {
        return attemptRepository.existsByStudentAndEvaluationAndPassedTrue(student, evaluation);
    }

    // ==================== Statistiques ====================

    public Map<String, Object> getEvaluationStats(Long evaluationId) {
        Map<String, Object> stats = new HashMap<>();
        
        long totalAttempts = attemptRepository.countCompletedAttempts(evaluationId);
        long passedAttempts = attemptRepository.countPassedAttempts(evaluationId);
        Double avgScore = attemptRepository.getAverageScore(evaluationId);
        
        stats.put("totalAttempts", totalAttempts);
        stats.put("passedAttempts", passedAttempts);
        stats.put("failedAttempts", totalAttempts - passedAttempts);
        stats.put("passRate", totalAttempts > 0 ? (double) passedAttempts / totalAttempts * 100 : 0);
        stats.put("averageScore", avgScore != null ? avgScore : 0);
        
        return stats;
    }

    public List<Map<String, Object>> getStudentResults(Long evaluationId) {
        List<EvaluationAttempt> attempts = attemptRepository.findByEvaluationIdOrderByScoreDesc(evaluationId);
        List<Map<String, Object>> results = new ArrayList<>();
        
        for (EvaluationAttempt attempt : attempts) {
            if (attempt.isCompleted()) {
                Map<String, Object> result = new HashMap<>();
                result.put("studentName", attempt.getStudent().getFullName());
                result.put("studentUsername", attempt.getStudent().getUsername());
                result.put("score", attempt.getScore());
                result.put("passed", attempt.isPassed());
                result.put("correctAnswers", attempt.getCorrectAnswers());
                result.put("totalQuestions", attempt.getTotalQuestions());
                result.put("completedAt", attempt.getCompletedAt());
                results.add(result);
            }
        }
        
        return results;
    }

    // ==================== Cours réussis pour étudiant ====================

    public List<Map<String, Object>> getPassedCoursesByStudent(User student) {
        List<EvaluationAttempt> passedAttempts = attemptRepository.findByStudentAndPassedTrue(student);
        Map<Long, Map<String, Object>> coursesMap = new HashMap<>();
        
        for (EvaluationAttempt attempt : passedAttempts) {
            Course course = attempt.getEvaluation().getCourse();
            Long courseId = course.getId();
            
            if (!coursesMap.containsKey(courseId)) {
                Map<String, Object> courseInfo = new HashMap<>();
                courseInfo.put("courseId", courseId);
                courseInfo.put("courseTitle", course.getTitle());
                courseInfo.put("evaluationsPassed", 1);
                courseInfo.put("bestScore", attempt.getScore());
                courseInfo.put("lastPassedAt", attempt.getCompletedAt());
                coursesMap.put(courseId, courseInfo);
            } else {
                Map<String, Object> courseInfo = coursesMap.get(courseId);
                courseInfo.put("evaluationsPassed", (Integer) courseInfo.get("evaluationsPassed") + 1);
                if (attempt.getScore() > (Double) courseInfo.get("bestScore")) {
                    courseInfo.put("bestScore", attempt.getScore());
                }
            }
        }
        
        return new ArrayList<>(coursesMap.values());
    }

    public List<EvaluationAttempt> getPassedAttemptsByStudent(User student) {
        return attemptRepository.findByStudentAndPassedTrue(student);
    }

    // ==================== Tous les résultats pour Admin ====================

    public List<EvaluationAttempt> getAllCompletedAttempts() {
        return attemptRepository.findAllByCompletedTrueOrderByCompletedAtDesc();
    }

    public Map<String, Object> getGlobalStats() {
        List<EvaluationAttempt> allAttempts = attemptRepository.findAllByCompletedTrueOrderByCompletedAtDesc();
        Map<String, Object> stats = new HashMap<>();
        
        long totalAttempts = allAttempts.size();
        long passedAttempts = allAttempts.stream().filter(EvaluationAttempt::isPassed).count();
        double avgScore = allAttempts.stream()
                .mapToDouble(EvaluationAttempt::getScore)
                .average()
                .orElse(0.0);
        
        stats.put("totalAttempts", totalAttempts);
        stats.put("passedAttempts", passedAttempts);
        stats.put("failedAttempts", totalAttempts - passedAttempts);
        stats.put("passRate", totalAttempts > 0 ? (double) passedAttempts / totalAttempts * 100 : 0);
        stats.put("averageScore", avgScore);
        
        return stats;
    }
}
