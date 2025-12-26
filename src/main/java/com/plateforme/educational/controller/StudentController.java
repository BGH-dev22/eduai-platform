package com.plateforme.educational.controller;

import com.plateforme.educational.entity.*;
import com.plateforme.educational.repository.QuizAttemptRepository;
import com.plateforme.educational.service.AIAgentService;
import com.plateforme.educational.service.CourseFileService;
import com.plateforme.educational.service.CourseService;
import com.plateforme.educational.service.EvaluationService;
import com.plateforme.educational.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final CourseService courseService;
    private final UserService userService;
    private final AIAgentService aiAgentService;
    private final QuizAttemptRepository quizAttemptRepository;
    private final EvaluationService evaluationService;
    private final CourseFileService courseFileService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        User student = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Student not found"));
        
        List<Course> enrolledCourses = courseService.findCoursesByStudent(student.getId());
        List<QuizAttempt> recentAttempts = quizAttemptRepository
                .findByStudentOrderByAttemptDateDesc(student);
        
        // Récupérer les cours où l'étudiant a réussi des évaluations
        List<Map<String, Object>> passedCourses = evaluationService.getPassedCoursesByStudent(student);
        List<EvaluationAttempt> passedEvaluations = evaluationService.getPassedAttemptsByStudent(student);
        
        model.addAttribute("student", student);
        model.addAttribute("courses", enrolledCourses);
        model.addAttribute("recentAttempts", recentAttempts.stream().limit(5).toList());
        model.addAttribute("totalCourses", enrolledCourses.size());
        model.addAttribute("passedCourses", passedCourses);
        model.addAttribute("passedEvaluations", passedEvaluations);
        model.addAttribute("totalPassed", passedEvaluations.size());
        
        return "student/dashboard";
    }

    @GetMapping("/courses")
    public String listCourses(Model model, Authentication authentication) {
        User student = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Student not found"));
        
        List<Course> courses = courseService.findCoursesByStudent(student.getId());
        model.addAttribute("courses", courses);
        
        return "student/courses";
    }

    @GetMapping("/courses/{id}")
    public String viewCourse(@PathVariable Long id, 
                            Model model, 
                            Authentication authentication) {
        User student = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Student not found"));
        
        Course course = courseService.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        
        // Vérifier que l'étudiant est inscrit
        if (!courseService.isStudentEnrolled(id, student.getId())) {
            throw new RuntimeException("You are not enrolled in this course");
        }
        
        List<QuizAttempt> attempts = quizAttemptRepository
                .findByStudentAndCourseOrderByAttemptDateDesc(student, course);
        
        // Préparer la liste des liens vidéos
        List<String> videoLinksList = new java.util.ArrayList<>();
        if (course.getVideoLinks() != null && !course.getVideoLinks().isEmpty()) {
            String[] links = course.getVideoLinks().split("[\\r\\n]+");
            for (String link : links) {
                String trimmed = link.trim();
                if (!trimmed.isEmpty()) {
                    videoLinksList.add(trimmed);
                }
            }
        }
        
        model.addAttribute("course", course);
        model.addAttribute("attempts", attempts);
        model.addAttribute("canGenerateQuiz", course.isIndexed());
        model.addAttribute("videoLinks", videoLinksList);
        
        return "student/course-detail";
    }

    @GetMapping("/courses/{courseId}/files/{fileId}")
    public ResponseEntity<Resource> downloadCourseFile(@PathVariable Long courseId,
                                                       @PathVariable Long fileId,
                                                       Authentication authentication) {
        User student = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Student not found"));
        
        Course course = courseService.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        
        // Vérifier que l'étudiant est inscrit au cours
        if (!courseService.isStudentEnrolled(courseId, student.getId())) {
            throw new RuntimeException("You are not enrolled in this course");
        }
        
        CourseFile file = courseFileService.getFile(fileId);
        
        if (!file.getCourse().getId().equals(course.getId())) {
            throw new RuntimeException("File not associated with this course");
        }
        
        Resource resource = courseFileService.loadAsResource(fileId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalFilename() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, file.getContentType())
                .body(resource);
    }

    @PostMapping("/courses/{id}/generate-quiz")
    public String generateQuiz(@PathVariable Long id, 
                              @RequestParam(defaultValue = "BEGINNER") String difficulty,
                              @RequestParam(defaultValue = "10") int numQuestions,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            User student = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Student not found"));
            
            Course course = courseService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Course not found"));
            
            if (!course.isIndexed()) {
                throw new RuntimeException("Ce cours n'a pas encore été indexé");
            }
            
            // L'IA agentique génère le quiz avec le niveau choisi
            QuizAttempt attempt = aiAgentService.generateCustomQuiz(student, course, difficulty, numQuestions);
            
            redirectAttributes.addFlashAttribute("success", 
                    "Quiz " + difficulty + " généré avec " + numQuestions + " questions !");
            return "redirect:/student/quiz/" + attempt.getId();
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/student/courses/" + id;
        }
    }

    @GetMapping("/quiz/{id}")
    public String takeQuiz(@PathVariable Long id, 
                          Model model, 
                          Authentication authentication) {
        User student = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Student not found"));
        
        QuizAttempt attempt = quizAttemptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        
        // Vérifier que le quiz appartient à l'étudiant
        if (!attempt.getStudent().getId().equals(student.getId())) {
            throw new RuntimeException("This quiz doesn't belong to you");
        }
        
        model.addAttribute("attempt", attempt);
        model.addAttribute("course", attempt.getCourse());
        model.addAttribute("questions", attempt.getQuestions());
        
        return "student/quiz";
    }

    @PostMapping("/quiz/{id}/submit")
    public String submitQuiz(@PathVariable Long id, 
                            @RequestParam Map<String, String> answers,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        User student = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Student not found"));
        
        QuizAttempt attempt = quizAttemptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        
        if (!attempt.getStudent().getId().equals(student.getId())) {
            throw new RuntimeException("This quiz doesn't belong to you");
        }
        
        // Traiter les réponses
        for (QuizQuestion question : attempt.getQuestions()) {
            String answerKey = "question_" + question.getId();
            if (answers.containsKey(answerKey)) {
                try {
                    int answerIndex = Integer.parseInt(answers.get(answerKey));
                    question.setStudentAnswerIndex(answerIndex);
                    question.checkAnswer();
                } catch (NumberFormatException e) {
                    // Ignorer les réponses invalides
                }
            }
        }
        
        // L'IA agentique évalue et fait une recommandation
        String recommendation = aiAgentService.evaluateAndDecide(attempt);
        
        redirectAttributes.addFlashAttribute("success", "Quiz soumis avec succès");
        redirectAttributes.addFlashAttribute("recommendation", recommendation);
        
        return "redirect:/student/quiz/" + id + "/results";
    }

    @GetMapping("/quiz/{id}/results")
    public String viewResults(@PathVariable Long id, 
                             Model model, 
                             Authentication authentication) {
        User student = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Student not found"));
        
        QuizAttempt attempt = quizAttemptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        
        if (!attempt.getStudent().getId().equals(student.getId())) {
            throw new RuntimeException("This quiz doesn't belong to you");
        }
        
        model.addAttribute("attempt", attempt);
        model.addAttribute("course", attempt.getCourse());
        model.addAttribute("questions", attempt.getQuestions());
        
        return "student/quiz-results";
    }

    @GetMapping("/history")
    public String viewHistory(Model model, Authentication authentication) {
        User student = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Student not found"));
        
        List<QuizAttempt> attempts = quizAttemptRepository
                .findByStudentOrderByAttemptDateDesc(student);
        
        model.addAttribute("attempts", attempts);
        
        return "student/history";
    }

    // ===== Évaluations du professeur =====

    @GetMapping("/evaluations")
    public String listAllEvaluations(Model model, Authentication authentication) {
        User student = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Étudiant non trouvé"));
        
        // Récupérer tous les cours de l'étudiant
        List<Course> enrolledCourses = courseService.findCoursesByStudent(student.getId());
        
        // Récupérer toutes les évaluations de tous les cours
        List<Map<String, Object>> allEvaluations = new java.util.ArrayList<>();
        for (Course course : enrolledCourses) {
            List<Evaluation> courseEvaluations = evaluationService.findAvailableForStudent(course.getId());
            for (Evaluation eval : courseEvaluations) {
                Map<String, Object> evalInfo = new HashMap<>();
                evalInfo.put("evaluation", eval);
                evalInfo.put("course", course);
                evalInfo.put("passed", evaluationService.hasStudentPassed(student, eval));
                allEvaluations.add(evalInfo);
            }
        }
        
        model.addAttribute("allEvaluations", allEvaluations);
        model.addAttribute("courses", enrolledCourses);
        
        return "student/evaluations";
    }

    @GetMapping("/courses/{courseId}/evaluations")
    public String listCourseEvaluations(@PathVariable Long courseId,
                                  Model model,
                                  Authentication authentication) {
        User student = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Étudiant non trouvé"));
        
        Course course = courseService.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));
        
        if (!courseService.isStudentEnrolled(courseId, student.getId())) {
            throw new RuntimeException("Vous n'êtes pas inscrit à ce cours");
        }
        
        List<Evaluation> evaluations = evaluationService.findAvailableForStudent(courseId);
        
        // Vérifier les évaluations déjà passées
        Map<Long, Boolean> passedStatus = new HashMap<>();
        for (Evaluation eval : evaluations) {
            passedStatus.put(eval.getId(), evaluationService.hasStudentPassed(student, eval));
        }
        
        model.addAttribute("course", course);
        model.addAttribute("evaluations", evaluations);
        model.addAttribute("passedStatus", passedStatus);
        
        return "student/course-evaluations";
    }

    @GetMapping("/evaluations/{id}/start")
    public String startEvaluation(@PathVariable Long id,
                                  Model model,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        User student = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Étudiant non trouvé"));
        
        Evaluation evaluation = evaluationService.findById(id)
                .orElseThrow(() -> new RuntimeException("Évaluation non trouvée"));
        
        // Vérifier que l'étudiant est inscrit au cours
        if (!courseService.isStudentEnrolled(evaluation.getCourse().getId(), student.getId())) {
            redirectAttributes.addFlashAttribute("error", "Vous n'êtes pas inscrit à ce cours");
            return "redirect:/student/courses";
        }
        
        // Démarrer ou reprendre la tentative
        EvaluationAttempt attempt = evaluationService.startAttempt(id, student);
        
        model.addAttribute("evaluation", evaluation);
        model.addAttribute("attempt", attempt);
        model.addAttribute("questions", evaluation.getQuestions());
        
        return "student/evaluation-take";
    }

    @PostMapping("/evaluations/{attemptId}/submit")
    public String submitEvaluation(@PathVariable Long attemptId,
                                   @RequestParam Map<String, String> allParams,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        User student = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Étudiant non trouvé"));
        
        EvaluationAttempt attempt = evaluationService.findAttemptById(attemptId)
                .orElseThrow(() -> new RuntimeException("Tentative non trouvée"));
        
        if (!attempt.getStudent().getId().equals(student.getId())) {
            throw new RuntimeException("Cette tentative ne vous appartient pas");
        }
        
        // Extraire les réponses
        Map<Long, Integer> answers = new HashMap<>();
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            if (entry.getKey().startsWith("question_")) {
                try {
                    Long questionId = Long.parseLong(entry.getKey().replace("question_", ""));
                    Integer answerIndex = Integer.parseInt(entry.getValue());
                    answers.put(questionId, answerIndex);
                } catch (NumberFormatException e) {
                    // Ignorer
                }
            }
        }
        
        // Soumettre et calculer le score
        attempt = evaluationService.submitAttempt(attemptId, answers);
        
        if (attempt.isPassed()) {
            redirectAttributes.addFlashAttribute("success", 
                    "🎉 Félicitations ! Vous avez réussi avec " + 
                    String.format("%.1f", attempt.getScore()) + "% !");
        } else {
            redirectAttributes.addFlashAttribute("error", 
                    "Vous avez obtenu " + String.format("%.1f", attempt.getScore()) + 
                    "%. Le seuil de réussite est de " + attempt.getEvaluation().getPassingScore() + "%.");
        }
        
        return "redirect:/student/evaluations/" + attemptId + "/results";
    }

    @GetMapping("/evaluations/{attemptId}/results")
    public String viewEvaluationResults(@PathVariable Long attemptId,
                                        Model model,
                                        Authentication authentication) {
        User student = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Étudiant non trouvé"));
        
        EvaluationAttempt attempt = evaluationService.findAttemptById(attemptId)
                .orElseThrow(() -> new RuntimeException("Tentative non trouvée"));
        
        if (!attempt.getStudent().getId().equals(student.getId())) {
            throw new RuntimeException("Cette tentative ne vous appartient pas");
        }
        
        model.addAttribute("attempt", attempt);
        model.addAttribute("evaluation", attempt.getEvaluation());
        model.addAttribute("answers", attempt.getAnswers());
        
        return "student/evaluation-results";
    }
}
