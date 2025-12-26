package com.plateforme.educational.controller;

import com.plateforme.educational.dto.CourseFormDTO;
import com.plateforme.educational.entity.Course;
import com.plateforme.educational.entity.Evaluation;
import com.plateforme.educational.entity.EvaluationAttempt;
import com.plateforme.educational.entity.EvaluationQuestion;
import com.plateforme.educational.entity.User;
import com.plateforme.educational.service.CourseFileService;
import com.plateforme.educational.service.CourseService;
import com.plateforme.educational.service.EvaluationService;
import com.plateforme.educational.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final CourseService courseService;
    private final UserService userService;
    private final CourseFileService courseFileService;
    private final EvaluationService evaluationService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<Course> courses = courseService.findAll();
        List<User> students = userService.findAllStudents();
        
        // Calculer les statistiques
        long publishedCourses = courses.stream().filter(Course::isPublished).count();
        long indexedCourses = courses.stream().filter(Course::isIndexed).count();
        
        model.addAttribute("courses", courses);
        model.addAttribute("students", students);
        model.addAttribute("totalCourses", courses.size());
        model.addAttribute("totalStudents", students.size());
        model.addAttribute("publishedCourses", publishedCourses);
        model.addAttribute("indexedCourses", indexedCourses);
        
        return "admin/dashboard";
    }

    // ===== Gestion des cours =====

    @GetMapping("/courses")
    public String listCourses(Model model) {
        model.addAttribute("courses", courseService.findAll());
        return "admin/courses";
    }

    @GetMapping("/courses/new")
    public String newCourseForm(Model model) {
        model.addAttribute("course", new CourseFormDTO());
        return "admin/course-form";
    }

    @PostMapping("/courses")
    public String createCourse(@ModelAttribute CourseFormDTO courseForm,
                              @RequestParam(value = "files", required = false) List<MultipartFile> files,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        User admin = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        
        Course course = new Course();
        course.setTitle(courseForm.getTitle());
        course.setDescription(courseForm.getDescription());
        course.setContent(courseForm.getContent());
        course.setVideoLinks(courseForm.getVideoLinks());
        
        courseService.createCourse(course, admin, files);
        redirectAttributes.addFlashAttribute("success", "Cours créé avec succès");
        return "redirect:/admin/courses";
    }

    @GetMapping("/courses/{id}/edit")
    public String editCourseForm(@PathVariable Long id, Model model) {
        Course course = courseService.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        
        CourseFormDTO courseForm = new CourseFormDTO();
        courseForm.setId(course.getId());
        courseForm.setTitle(course.getTitle());
        courseForm.setDescription(course.getDescription());
        courseForm.setContent(course.getContent());
        courseForm.setVideoLinks(course.getVideoLinks());
        
        model.addAttribute("course", courseForm);
        model.addAttribute("courseFiles", course.getFiles());
        return "admin/course-form";
    }

    @PostMapping("/courses/{id}")
    public String updateCourse(@PathVariable Long id,
                              @ModelAttribute CourseFormDTO courseForm,
                              @RequestParam(value = "files", required = false) List<MultipartFile> files,
                              RedirectAttributes redirectAttributes) {
        try {
            Course courseDetails = new Course();
            courseDetails.setTitle(courseForm.getTitle());
            courseDetails.setDescription(courseForm.getDescription());
            courseDetails.setContent(courseForm.getContent());
            courseDetails.setVideoLinks(courseForm.getVideoLinks());
            
            courseService.updateCourse(id, courseDetails, files);
            redirectAttributes.addFlashAttribute("success", "Cours mis à jour avec succès");
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du cours", e);
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        return "redirect:/admin/courses";
    }

    @GetMapping("/courses/{id}/publish")
    public String publishCourse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            courseService.publishCourse(id);
            redirectAttributes.addFlashAttribute("success", "Cours publié avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/courses";
    }

    @GetMapping("/courses/{id}/unpublish")
    public String unpublishCourse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        courseService.unpublishCourse(id);
        redirectAttributes.addFlashAttribute("success", "Cours dépublié avec succès");
        return "redirect:/admin/courses";
    }

    @GetMapping("/courses/{courseId}/files/{fileId}")
    public ResponseEntity<Resource> downloadCourseFile(@PathVariable Long courseId,
                                                       @PathVariable Long fileId) {
        Course course = courseService.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        var file = courseFileService.getFile(fileId);

        if (!file.getCourse().getId().equals(course.getId())) {
            throw new RuntimeException("Fichier non rattaché à ce cours");
        }

        Resource resource = courseFileService.loadAsResource(fileId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalFilename() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, file.getContentType())
                .body(resource);
    }

    @GetMapping("/courses/{id}/index")
    public String indexCourse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            courseService.indexCourse(id);
            redirectAttributes.addFlashAttribute("success", "Cours indexé avec succès pour le RAG");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/courses";
    }

    @GetMapping("/courses/{id}/delete")
    public String deleteCourse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        courseService.deleteCourse(id);
        redirectAttributes.addFlashAttribute("success", "Cours supprimé avec succès");
        return "redirect:/admin/courses";
    }

    // ===== Gestion des étudiants =====

    @GetMapping("/students")
    public String listStudents(Model model) {
        model.addAttribute("students", userService.findAllStudents());
        return "admin/students";
    }

    @GetMapping("/students/new")
    public String newStudentForm(Model model) {
        model.addAttribute("user", new User());
        return "admin/student-form";
    }

    @PostMapping("/students")
    public String createStudent(@ModelAttribute User user, RedirectAttributes redirectAttributes) {
        user.setRole(User.Role.STUDENT);
        user.setEnabled(true);
        userService.createUser(user);
        redirectAttributes.addFlashAttribute("success", "Étudiant créé avec succès");
        return "redirect:/admin/students";
    }

    @GetMapping("/students/{id}/edit")
    public String editStudentForm(@PathVariable Long id, Model model) {
        User student = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        model.addAttribute("user", student);
        return "admin/student-form";
    }

    @PostMapping("/students/{id}")
    public String updateStudent(@PathVariable Long id, 
                               @ModelAttribute User user,
                               RedirectAttributes redirectAttributes) {
        userService.updateUser(id, user);
        redirectAttributes.addFlashAttribute("success", "Étudiant mis à jour avec succès");
        return "redirect:/admin/students";
    }

    @GetMapping("/students/{id}/delete")
    public String deleteStudent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.deleteUser(id);
        redirectAttributes.addFlashAttribute("success", "Étudiant supprimé avec succès");
        return "redirect:/admin/students";
    }

    // ===== Gestion des inscriptions =====

    @GetMapping("/courses/{courseId}/enrollments")
    public String manageCourseEnrollments(@PathVariable Long courseId, Model model) {
        Course course = courseService.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        List<User> allStudents = userService.findAllStudents();
        
        // Filtrer les étudiants disponibles (non inscrits)
        List<User> enrolledStudents = new java.util.ArrayList<>(course.getEnrolledStudents());
        List<User> availableStudents = allStudents.stream()
                .filter(s -> !enrolledStudents.contains(s))
                .collect(java.util.stream.Collectors.toList());
        
        model.addAttribute("course", course);
        model.addAttribute("allStudents", allStudents);
        model.addAttribute("enrolledStudents", enrolledStudents);
        model.addAttribute("availableStudents", availableStudents);
        
        return "admin/course-enrollments";
    }

    @PostMapping("/courses/{courseId}/enroll/{studentId}")
    public String enrollStudent(@PathVariable Long courseId, 
                               @PathVariable Long studentId,
                               RedirectAttributes redirectAttributes) {
        try {
            courseService.enrollStudent(courseId, studentId);
            redirectAttributes.addFlashAttribute("success", "Étudiant inscrit avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/courses/" + courseId + "/enrollments";
    }

    @PostMapping("/courses/{courseId}/unenroll/{studentId}")
    public String unenrollStudent(@PathVariable Long courseId, 
                                 @PathVariable Long studentId,
                                 RedirectAttributes redirectAttributes) {
        courseService.unenrollStudent(courseId, studentId);
        redirectAttributes.addFlashAttribute("success", "Étudiant désinscrit avec succès");
        return "redirect:/admin/courses/" + courseId + "/enrollments";
    }

    // ===== Gestion des évaluations (Panel Professeur) =====

    @GetMapping("/evaluations")
    public String listEvaluations(Model model) {
        List<Evaluation> evaluations = evaluationService.findAll();
        List<Course> courses = courseService.findAll();
        model.addAttribute("evaluations", evaluations);
        model.addAttribute("courses", courses);
        return "admin/evaluations";
    }

    @GetMapping("/evaluations/new")
    public String newEvaluationForm(Model model) {
        try {
            log.info("Accès au formulaire de création d'évaluation");
            Evaluation evaluation = new Evaluation();
            log.info("Objet Evaluation créé: {}", evaluation);
            List<Course> courses = courseService.findAll();
            log.info("Nombre de cours trouvés: {}", courses.size());
            model.addAttribute("evaluation", evaluation);
            model.addAttribute("courses", courses);
            log.info("Attributs du modèle ajoutés, retour vers evaluation-form");
            return "admin/evaluation-form";
        } catch (Exception e) {
            log.error("Erreur lors de l'accès au formulaire d'évaluation: ", e);
            throw e;
        }
    }

    @PostMapping("/evaluations")
    public String createEvaluation(@ModelAttribute Evaluation evaluation,
                                   @RequestParam Long courseId,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        User creator = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        Evaluation savedEvaluation = evaluationService.createEvaluation(evaluation, courseId, creator);
        redirectAttributes.addFlashAttribute("success", "Évaluation créée avec succès ! Ajoutez maintenant les questions.");
        return "redirect:/admin/evaluations/" + savedEvaluation.getId() + "/questions";
    }

    @GetMapping("/evaluations/{id}/edit")
    public String editEvaluationForm(@PathVariable Long id, Model model) {
        Evaluation evaluation = evaluationService.findById(id)
                .orElseThrow(() -> new RuntimeException("Évaluation non trouvée"));
        model.addAttribute("evaluation", evaluation);
        model.addAttribute("courses", courseService.findAll());
        return "admin/evaluation-form";
    }

    @PostMapping("/evaluations/{id}")
    public String updateEvaluation(@PathVariable Long id,
                                   @ModelAttribute Evaluation evaluation,
                                   RedirectAttributes redirectAttributes) {
        evaluationService.updateEvaluation(id, evaluation);
        redirectAttributes.addFlashAttribute("success", "Évaluation mise à jour avec succès");
        return "redirect:/admin/evaluations";
    }

    @GetMapping("/evaluations/{id}/delete")
    public String deleteEvaluation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        evaluationService.deleteEvaluation(id);
        redirectAttributes.addFlashAttribute("success", "Évaluation supprimée avec succès");
        return "redirect:/admin/evaluations";
    }

    @GetMapping("/evaluations/{id}/toggle")
    public String toggleEvaluation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Evaluation evaluation = evaluationService.findById(id)
                .orElseThrow(() -> new RuntimeException("Évaluation non trouvée"));
        evaluation.setActive(!evaluation.isActive());
        evaluationService.updateEvaluation(id, evaluation);
        String status = evaluation.isActive() ? "activée" : "désactivée";
        redirectAttributes.addFlashAttribute("success", "Évaluation " + status);
        return "redirect:/admin/evaluations";
    }

    // ===== Gestion des questions d'évaluation =====

    @GetMapping("/evaluations/{id}/questions")
    public String manageQuestions(@PathVariable Long id, Model model) {
        Evaluation evaluation = evaluationService.findById(id)
                .orElseThrow(() -> new RuntimeException("Évaluation non trouvée"));
        model.addAttribute("evaluation", evaluation);
        model.addAttribute("questions", evaluation.getQuestions());
        return "admin/evaluation-questions";
    }

    @PostMapping("/evaluations/{id}/questions")
    public String addQuestion(@PathVariable Long id,
                              @RequestParam String questionText,
                              @RequestParam String option1,
                              @RequestParam String option2,
                              @RequestParam String option3,
                              @RequestParam String option4,
                              @RequestParam Integer correctAnswer,
                              @RequestParam(required = false) String explanation,
                              @RequestParam(defaultValue = "1") Integer points,
                              RedirectAttributes redirectAttributes) {
        
        List<String> options = new ArrayList<>();
        options.add(option1);
        options.add(option2);
        options.add(option3);
        options.add(option4);
        
        EvaluationQuestion question = new EvaluationQuestion();
        question.setQuestionText(questionText);
        question.setOptions(options);
        question.setCorrectAnswerIndex(correctAnswer);
        question.setExplanation(explanation);
        question.setPoints(points);
        
        evaluationService.addQuestion(id, question);
        redirectAttributes.addFlashAttribute("success", "Question ajoutée avec succès");
        return "redirect:/admin/evaluations/" + id + "/questions";
    }

    @GetMapping("/evaluations/{evalId}/questions/{questionId}/delete")
    public String deleteQuestion(@PathVariable Long evalId,
                                 @PathVariable Long questionId,
                                 RedirectAttributes redirectAttributes) {
        evaluationService.deleteQuestion(questionId);
        redirectAttributes.addFlashAttribute("success", "Question supprimée avec succès");
        return "redirect:/admin/evaluations/" + evalId + "/questions";
    }

    // ===== Résultats des évaluations =====

    @GetMapping("/evaluations/{id}/results")
    public String viewResults(@PathVariable Long id, Model model) {
        Evaluation evaluation = evaluationService.findById(id)
                .orElseThrow(() -> new RuntimeException("Évaluation non trouvée"));
        
        List<Map<String, Object>> results = evaluationService.getStudentResults(id);
        Map<String, Object> stats = evaluationService.getEvaluationStats(id);
        
        model.addAttribute("evaluation", evaluation);
        model.addAttribute("results", results);
        model.addAttribute("stats", stats);
        
        return "admin/evaluation-results";
    }

    @GetMapping("/evaluations/all-results")
    public String viewAllResults(Model model) {
        List<EvaluationAttempt> allAttempts = evaluationService.getAllCompletedAttempts();
        Map<String, Object> globalStats = evaluationService.getGlobalStats();
        List<Evaluation> evaluations = evaluationService.findAll();
        
        model.addAttribute("attempts", allAttempts);
        model.addAttribute("stats", globalStats);
        model.addAttribute("evaluations", evaluations);
        
        return "admin/all-evaluation-results";
    }
}
