# Architecture Technique - Plateforme Pédagogique IA

## Vue d'ensemble de l'architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         PRESENTATION LAYER                       │
│                         (Thymeleaf Views)                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │  Login   │  │  Admin   │  │ Student  │  │  Quiz    │       │
│  │  Pages   │  │Dashboard │  │Dashboard │  │  Pages   │       │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘       │
└─────────────────────────────────────────────────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────────┐
│                        SECURITY LAYER                            │
│                      (Spring Security)                           │
│  ┌────────────────────────────────────────────────────────┐    │
│  │  Authentication → Authorization → Role-Based Access    │    │
│  │  ADMIN Routes (/admin/**) | STUDENT Routes (/student/**)│   │
│  └────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────────┐
│                      CONTROLLER LAYER                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │    Main      │  │    Admin     │  │   Student    │         │
│  │ Controller   │  │  Controller  │  │  Controller  │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
└─────────────────────────────────────────────────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────────┐
│                       SERVICE LAYER                              │
│  ┌───────────┐  ┌───────────┐  ┌────────────────────────────┐ │
│  │   User    │  │  Course   │  │    AI Services             │ │
│  │  Service  │  │  Service  │  │  ┌──────┐ ┌─────┐ ┌──────┐│ │
│  └───────────┘  └───────────┘  │  │ RAG  │ │ LLM │ │Agent ││ │
│                                 │  └──────┘ └─────┘ └──────┘│ │
│                                 └────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────────┐
│                    PERSISTENCE LAYER                             │
│                    (Spring Data JPA)                             │
│  ┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐            │
│  │ User │  │Course│  │ Quiz │  │Chunk │  │Question│           │
│  │ Repo │  │ Repo │  │ Repo │  │ Repo │  │  Repo  │           │
│  └──────┘  └──────┘  └──────┘  └──────┘  └──────┘            │
└─────────────────────────────────────────────────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────────┐
│                      DATABASE LAYER                              │
│                      (H2 In-Memory)                              │
│  ┌────────────────────────────────────────────────────────┐    │
│  │  users | courses | course_chunks | quiz_attempts       │    │
│  │  quiz_questions | student_courses | quiz_options       │    │
│  └────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

## Architecture IA en détail

### Flux de génération de quiz

```
┌─────────────────────────────────────────────────────────────────┐
│ STUDENT demande un quiz                                         │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ AI AGENT SERVICE                                                │
│ ┌─────────────────────────────────────────────────────────┐   │
│ │ 1. Analyse de l'historique de l'étudiant                │   │
│ │    - Tentatives précédentes                             │   │
│ │    - Scores moyens                                      │   │
│ │    - Progression                                        │   │
│ └─────────────────────────────────────────────────────────┘   │
│                              ↓                                  │
│ ┌─────────────────────────────────────────────────────────┐   │
│ │ 2. Décision des paramètres du quiz                      │   │
│ │    - Niveau de difficulté (BEGINNER/INTERMEDIATE/ADV)  │   │
│ │    - Nombre de questions (5-8)                          │   │
│ │    - Nombre de chunks de contexte à récupérer          │   │
│ └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ RAG SERVICE                                                     │
│ ┌─────────────────────────────────────────────────────────┐   │
│ │ 3. Récupération du contexte                             │   │
│ │    - Recherche des chunks pertinents                    │   │
│ │    - Calcul de similarité                               │   │
│ │    - Construction du contexte agrégé                    │   │
│ └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────���───────────────────────────────────────────────┐
│ LLM SERVICE                                                     │
│ ┌─────────────────────────────────────────────────────────┐   │
│ │ 4. Génération des questions                             │   │
│ │    - Construction du prompt avec contexte               │   │
│ │    - Appel au LLM (OpenAI/Ollama)                       │   │
│ │    - Parsing de la réponse JSON                         │   │
│ │    - Validation du format                               │   │
│ └─────────────────────────────────────────────────────────┘   │
└──────��──────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ AI AGENT SERVICE                                                │
│ ┌─────────────────────────────────────────────────────────┐   │
│ │ 5. Validation et création du quiz                       │   │
│ │    - Vérification de la cohérence                       │   │
│ │    - Création de QuizAttempt                            │   │
│ │    - Enregistrement de la décision de l'agent          │   │
│ └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Quiz présenté à l'étudiant                                      │
└─────────────────────────────────────────────────────────────────┘
```

### Flux d'évaluation

```
┌─────────────────────────────────────────────────────────────────┐
│ STUDENT soumet les réponses                                     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ STUDENT CONTROLLER                                              │
│ ┌─────────────────────────────────────────────────────────┐   │
│ │ 1. Enregistrement des réponses                          │   │
│ │    - Stockage de studentAnswerIndex                     │   │
│ │    - Vérification de chaque réponse                     │   │
│ └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ AI AGENT SERVICE                                                │
│ ┌─────────────────────────────────────────────────────────┐   │
│ │ 2. Évaluation intelligente                              │   │
│ │    - Calcul du score                                    │   │
│ │    - Analyse des erreurs                                │   │
│ │    - Détermination du niveau atteint                    │   │
│ │    - Génération de recommandations personnalisées       │   │
│ │    - Décision: quiz validé ou révision nécessaire       │   │
│ └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Résultats affichés avec recommandation de l'IA                 │
└─────────────────────────────────────────────────────────────────┘
```

## Modèle de domaine

### Diagramme de classes principal

```
┌─────────────────┐
│      User       │
├─────────────────┤
│ + id            │
│ + username      │
│ + password      │
│ + email         │
│ + fullName      │
│ + role          │◄──────┐
│ + enabled       │       │
└─────────────────┘       │
         ↑                │
         │ *              │
         │                │
         │ *              │
┌─────────────────┐       │
│     Course      │       │
├─────────────────┤       │
│ + id            │       │
│ + title         │       │ 1
│ + description   │       │
│ + content       │───────┘ creator
│ + published     │
│ + indexed       │
└─────────────────┘
         │ 1
         │
         │ *
┌─────────────────┐
│  CourseChunk    │
├─────────────────┤
│ + id            │
│ + content       │
│ + chunkIndex    │
│ + embedding     │
└─────────────────┘

┌─────────────────┐       ┌─────────────────┐
│  QuizAttempt    │       │  QuizQuestion   │
├─────────────────┤       ├─────────────────┤
│ + id            │ 1   * │ + id            │
│ + attemptDate   │───────│ + questionText  │
│ + totalQuestions│       │ + options       │
│ + correctAnswers│       │ + correctAnswer │
│ + score         │       │ + studentAnswer │
│ + difficulty    │       │ + explanation   │
│ + passed        │       │ + correct       │
│ + agentDecision │       └─────────────────┘
│ + recommendation│
└─────────────────┘
```

## Patterns et Principes

### Design Patterns Utilisés

1. **MVC (Model-View-Controller)**
   - Séparation claire entre logique métier, présentation et données

2. **Repository Pattern**
   - Abstraction de la couche de persistance via Spring Data JPA

3. **Service Layer Pattern**
   - Encapsulation de la logique métier dans des services

4. **DTO Pattern** (implicite)
   - Transfert de données entre couches (QuizQuestionDTO)

5. **Strategy Pattern** (dans l'IA)
   - Sélection dynamique de la stratégie selon le niveau de l'étudiant

6. **Template Method** (Spring)
   - Utilisation des templates Spring (JpaRepository, etc.)

### Principes SOLID

- **Single Responsibility**: Chaque classe a une responsabilité unique
- **Open/Closed**: Extensions possibles sans modification (ajout de nouveaux types de quiz)
- **Liskov Substitution**: Les interfaces sont bien respectées
- **Interface Segregation**: Interfaces spécifiques (UserRepository, CourseRepository)
- **Dependency Inversion**: Injection de dépendances via constructeur

## Sécurité

### Niveaux de sécurité

```
┌─────────────────────────────────────────────────────────────────┐
│ NIVEAU 1: Spring Security                                       │
│ - Authentification par formulaire                               │
│ - Encodage BCrypt des mots de passe                            │
│ - Protection CSRF                                               │
│ - Sessions HTTP sécurisées                                      │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ NIVEAU 2: Autorisation basée sur les rôles                     │
│ - @PreAuthorize sur les méthodes sensibles                     │
│ - Filtrage des routes par rôle                                 │
│ - Vérification du propriétaire des ressources                  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ NIVEAU 3: Validation métier                                     │
│ - Un étudiant ne peut accéder qu'à ses propres quiz            │
│ - Vérification d'inscription au cours avant génération         │
│ - Validation de l'état du cours (publié, indexé)               │
└─────────────────────────────────────────────────────────────────┘
```

## Performance et Optimisation

### Optimisations implémentées

1. **Lazy Loading**: Relations JPA chargées à la demande
2. **Indexation**: Préparation des données pour recherches rapides (RAG)
3. **Caching** (à implémenter): Spring Cache pour les cours fréquemment consultés
4. **Pagination** (à implémenter): Pour les listes longues

### Points d'amélioration

1. **Cache Redis**: Pour les sessions et les résultats de RAG
2. **Base vectorielle**: Remplacer le système d'embedding simplifié
3. **Async Processing**: Génération de quiz en background
4. **CDN**: Pour les ressources statiques (images, CSS)

## Évolutivité

### Architecture évolutive

```
ACTUEL                    FUTUR
──────                    ─────

H2 In-Memory     →    PostgreSQL + Redis
                      (Production DB + Cache)

Thymeleaf        →    API REST + Frontend SPA
                      (React/Vue/Angular)

LLM Simulé       →    OpenAI/Ollama/HuggingFace
                      (Vrai LLM en production)

Simple Hash      →    Vector Database
                      (Pinecone/Weaviate/Milvus)

Monolithe        →    Microservices
                      (Auth, Courses, Quiz, AI)
```

## Monitoring et Logs

### Niveaux de logs

```
DEBUG: Détails techniques pour le développement
INFO:  Événements importants (création de cours, génération de quiz)
WARN:  Situations anormales mais gérables
ERROR: Erreurs nécessitant une attention
```

### Métriques à surveiller (production)

- Temps de génération de quiz
- Taux de réussite des quiz
- Temps de réponse des endpoints
- Utilisation mémoire/CPU
- Taux d'erreur LLM

## Tests

### Stratégie de tests (à implémenter)

```
┌─────────────────────────────────────────────────────────────────┐
│ Unit Tests                                                      │
│ - Services métier (UserService, CourseService)                 │
│ - Logique IA (AIAgentService, RAGService)                      │
│ - Repositories                                                  │
└─────────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────────┐
│ Integration Tests                                               │
│ - Controllers avec MockMvc                                      │
│ - Sécurité Spring                                              │
│ - Flux complets (création cours → génération quiz → évaluation)│
└─────────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────────┐
│ End-to-End Tests (optionnel)                                    │
│ - Selenium/Playwright pour tests UI                            │
│ - Scénarios utilisateur complets                               │
└─────────────────────────────────────────────────────────────────┘
```

---

**Version:** 1.0.0  
**Date:** Décembre 2025
