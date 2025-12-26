# Plateforme Pédagogique avec IA Générative

## 🎓 Description du Projet

Plateforme pédagogique intelligente et sécurisée basée sur Spring Boot, intégrant des mécanismes avancés d'intelligence artificielle générative (LLM + RAG + Agent IA) pour la génération automatique et adaptative de quiz.

### Fonctionnalités Principales

- ✅ **Gestion des utilisateurs** avec authentification et autorisation (Spring Security)
- ✅ **Rôles différenciés**: Administrateur et Étudiant
- ✅ **Gestion complète des cours**: création, publication, indexation
- ✅ **Système RAG (Retrieval-Augmented Generation)**: indexation intelligente du contenu
- ✅ **IA Agentique**: génération adaptative de quiz selon le niveau de l'étudiant
- ✅ **Quiz QCM générés par IA**: questions pertinentes basées exclusivement sur le contenu du cours
- ✅ **Évaluation intelligente**: recommandations personnalisées par l'IA agentique
- ✅ **Interface utilisateur moderne**: Thymeleaf avec design responsive

---

## 🏗️ Architecture Technique

### Stack Technologique

- **Framework**: Spring Boot 3.2.0
- **Sécurité**: Spring Security avec authentification par formulaire
- **Persistance**: Spring Data JPA + H2 Database
- **Vue**: Thymeleaf avec CSS intégré
- **IA**: Architecture LLM + RAG + Agent IA
- **Build**: Maven

### Structure du Projet

```
src/main/java/com/plateforme/educational/
├── config/
│   └── DataInitializer.java          # Initialisation des données de test
├── controller/
│   ├── MainController.java           # Contrôleur principal (login, dashboard)
│   ├── AdminController.java          # Gestion admin (cours, étudiants)
│   └── StudentController.java        # Interface étudiant (cours, quiz)
├── entity/
│   ├── User.java                     # Utilisateur (Admin/Student)
│   ├── Course.java                   # Cours pédagogique
│   ├── CourseChunk.java              # Fragment de cours pour RAG
│   ├── QuizAttempt.java              # Tentative de quiz
│   └── QuizQuestion.java             # Question de quiz
├── repository/
│   ├── UserRepository.java
│   ├── CourseRepository.java
│   ├── CourseChunkRepository.java
│   ├── QuizAttemptRepository.java
│   └── QuizQuestionRepository.java
├── security/
│   ├── SecurityConfig.java           # Configuration Spring Security
│   └── CustomUserDetailsService.java # Service d'authentification
├── service/
│   ├── UserService.java              # Gestion des utilisateurs
│   ├── CourseService.java            # Gestion des cours
│   ├── RAGService.java               # Système RAG (indexation/recherche)
│   ├── LLMService.java               # Interface LLM pour génération
│   └── AIAgentService.java           # IA Agentique (superviseur intelligent)
└── EducationalPlatformApplication.java
```

---

## 🤖 Architecture IA: LLM + RAG + Agent IA

### 1. RAG (Retrieval-Augmented Generation)

Le système RAG garantit que les quiz sont générés **exclusivement** à partir du contenu du cours:

**Processus d'indexation:**
- Découpage du contenu en chunks de 500 caractères avec chevauchement
- Stockage des fragments dans la base de données
- Génération d'embeddings simplifiés (hash dans cette version)

**Récupération du contexte:**
- Recherche des chunks pertinents par similarité
- Construction du contexte pour le LLM
- Garantie de la fidélité au contenu du cours

### 2. LLM (Large Language Model)

Service de génération de questions QCM:

**Caractéristiques:**
- Génère des questions avec 4 options (1 correcte)
- Fournit des explications pour chaque question
- Respecte strictement le contexte fourni par le RAG
- Format JSON structuré pour l'intégration

**Note:** La version actuelle utilise un LLM simulé pour le développement. Pour la production, configurer:
- OpenAI API (GPT-3.5/GPT-4)
- Ollama (LLM local)
- Autre fournisseur LLM

### 3. IA Agentique (Agent IA)

Le superviseur intelligent qui contrôle tout le processus:

**Responsabilités:**
- **Analyse de l'historique**: évalue les performances passées de l'étudiant
- **Décision adaptative**: détermine le nombre de questions et la difficulté
- **Contrôle qualité**: vérifie que le LLM respecte le contexte RAG
- **Évaluation**: analyse les résultats et fait des recommandations
- **Progression**: ajuste dynamiquement le niveau selon les résultats

**Niveaux de difficulté:**
- BEGINNER: premier quiz ou scores faibles (< 50%)
- INTERMEDIATE: scores entre 70% et 90%
- ADVANCED: excellent niveau (> 90%)

**Algorithme de décision:**
```
Si première tentative -> BEGINNER, 5 questions
Sinon si score moyen >= 90% et progression -> ADVANCED, 8 questions
Sinon si score moyen >= 75% -> INTERMEDIATE, 6 questions
Sinon si score moyen >= 70% -> niveau actuel maintenu
Sinon -> retour BEGINNER
```

---

## 👥 Gestion des Utilisateurs et Sécurité

### Rôles et Autorisations

**ADMIN (Administrateur):**
- Créer, modifier, supprimer des cours
- Publier et indexer les cours
- Gérer les comptes étudiants
- Inscrire/désinscrire des étudiants aux cours
- Accès: `/admin/**`

**STUDENT (Étudiant):**
- Consulter les cours inscrits uniquement
- Lire le contenu pédagogique
- Générer des quiz IA adaptatifs
- Passer les quiz et voir les résultats
- Accès: `/student/**`

### Configuration Spring Security

- Authentification par formulaire (login/password)
- Encodage BCrypt des mots de passe
- Séparation stricte des routes par rôle
- Protection CSRF activée
- Sessions HTTP sécurisées

---

## 📊 Modèle de Données

### Entités Principales

**User**
- Authentification et rôle (ADMIN/STUDENT)
- Relations: cours inscrits, tentatives de quiz

**Course**
- Contenu pédagogique textuel
- États: brouillon, publié, indexé
- Relations: créateur, étudiants inscrits, chunks RAG

**CourseChunk**
- Fragments du cours pour le RAG
- Contenu, position, embedding

**QuizAttempt**
- Tentative de quiz avec score et difficulté
- Relations: étudiant, cours, questions
- Métadonnées: décision IA, recommandation

**QuizQuestion**
- Question QCM avec options et explication
- Réponse correcte et réponse de l'étudiant

---

## 🚀 Installation et Démarrage

### Prérequis

- Java 17 ou supérieur
- Maven 3.6+
- (Optionnel) IDE Java (IntelliJ IDEA, Eclipse, VS Code)

### Installation

1. **Cloner/télécharger le projet**
```bash
cd mini_proj
```

2. **Compiler le projet**
```bash
mvn clean install
```

3. **Lancer l'application**
```bash
mvn spring-boot:run
```

4. **Accéder à l'application**
```
http://localhost:8080
```

### Base de Données H2

**Console H2:**
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:educational_platform`
- Username: `sa`
- Password: (vide)

---

## 🔐 Comptes de Démonstration

### Administrateur
- **Username**: `admin`
- **Password**: `admin123`
- **Rôle**: Gestion complète de la plateforme

### Étudiants
- **Username**: `student` / **Password**: `student123`
- **Username**: `marie` / **Password**: `marie123`
- **Username**: `jean` / **Password**: `jean123`

---

## 📖 Guide d'Utilisation

### Pour l'Administrateur

1. **Se connecter** avec le compte admin
2. **Créer un cours**:
   - Aller dans "Gérer les cours"
   - Cliquer sur "Nouveau cours"
   - Saisir titre, description et contenu pédagogique
   - Enregistrer
3. **Publier le cours**:
   - Cliquer sur "Publier" dans la liste des cours
4. **Indexer le cours pour le RAG**:
   - Cliquer sur "Indexer" (obligatoire pour la génération de quiz)
5. **Inscrire des étudiants**:
   - Cliquer sur "Inscrire" pour un cours
   - Sélectionner les étudiants à inscrire

### Pour l'Étudiant

1. **Se connecter** avec un compte étudiant
2. **Consulter les cours**:
   - Voir la liste des cours inscrits sur le dashboard
   - Cliquer sur un cours pour voir le contenu
3. **Générer un quiz IA**:
   - Sur la page du cours, cliquer sur "Générer un Quiz IA"
   - L'IA agentique analyse votre historique et crée un quiz adapté
4. **Passer le quiz**:
   - Répondre aux questions QCM
   - Soumettre les réponses
5. **Voir les résultats**:
   - Score et détails de correction
   - Explication pour chaque question
   - Recommandation de l'IA agentique pour la suite

---

## 🎯 Workflow Complet

```
Admin crée un cours
    ↓
Admin publie le cours
    ↓
Admin indexe le cours (RAG)
    ↓
Admin inscrit des étudiants
    ↓
Étudiant consulte le cours
    ↓
Étudiant demande un quiz
    ↓
Agent IA analyse l'historique
    ↓
Agent IA décide des paramètres (difficulté, nb questions)
    ↓
RAG récupère le contexte pertinent du cours
    ↓
LLM génère les questions basées sur le contexte
    ↓
Agent IA valide et crée le quiz
    ↓
Étudiant passe le quiz
    ↓
Agent IA évalue et fait une recommandation
    ↓
Étudiant voit les résultats et la recommandation
```

---

## 🔧 Configuration

### Configuration LLM (Production)

**Option 1: OpenAI**
```properties
# application.properties
spring.ai.openai.api-key=sk-your-api-key-here
spring.ai.openai.chat.options.model=gpt-3.5-turbo
spring.ai.openai.chat.options.temperature=0.7
```

**Option 2: Ollama (Local)**
```properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=llama2
```

### Paramètres du RAG

Modifier dans `RAGService.java`:
```java
private static final int CHUNK_SIZE = 500;        // Taille des chunks
private static final int CHUNK_OVERLAP = 100;     // Chevauchement
```

### Seuils de l'IA Agentique

Modifier dans `AIAgentService.java`:
```java
private static final double PASSING_SCORE = 70.0;  // Seuil de réussite
private static final double GOOD_SCORE = 75.0;      // Bon niveau
private static final double EXCELLENT_SCORE = 90.0; // Excellent niveau
```

---

## 🌟 Évolutions Possibles

### Court Terme
- ✅ Intégration d'un vrai LLM (OpenAI/Ollama)
- ✅ Amélioration des embeddings (utiliser un modèle de sentence transformers)
- ✅ Statistiques avancées pour les administrateurs
- ✅ Export des résultats en PDF

### Moyen Terme
- 📄 Support multi-formats: PDF, images, vidéos
- 🎥 Transcription automatique des vidéos
- 🖼️ Analyse d'images avec Computer Vision
- 🏆 Système de badges et certifications
- 📊 Tableaux de bord analytiques avancés

### Long Terme
- 🌐 API REST complète
- 📱 Application mobile (React Native/Flutter)
- 🔄 Synchronisation multi-dispositifs
- 👥 Système de tutorat peer-to-peer
- 🌍 Support multilingue avec traduction IA

---

## 🛠️ Technologies et Bibliothèques

| Catégorie | Technologie | Version |
|-----------|-------------|---------|
| Framework | Spring Boot | 3.2.0 |
| Sécurité | Spring Security | 6.x |
| Persistance | Spring Data JPA | 3.x |
| Base de données | H2 Database | 2.x |
| Vue | Thymeleaf | 3.x |
| Build | Maven | 3.x |
| Java | JDK | 17+ |
| Lombok | Lombok | Latest |
| IA | Spring AI | 0.8.1 |

---

## 📝 Règles de Gestion

1. Seul un administrateur authentifié peut gérer les cours et les étudiants
2. Un étudiant ne peut accéder qu'aux cours auxquels il est inscrit
3. Un cours doit être publié ET indexé pour permettre la génération de quiz
4. Les quiz sont générés exclusivement à partir du contenu du cours (garantie RAG)
5. L'IA agentique adapte automatiquement la difficulté selon les performances
6. Un score >= 70% est requis pour valider un quiz
7. Toutes les tentatives sont sauvegardées pour l'analyse de progression

---

## 🐛 Dépannage

### L'application ne démarre pas
- Vérifier Java 17+ installé: `java -version`
- Vérifier Maven installé: `mvn -version`
- Nettoyer et recompiler: `mvn clean install`

### Erreur de connexion à H2
- Vérifier l'URL JDBC dans application.properties
- Console H2 accessible sur http://localhost:8080/h2-console

### Les quiz ne se génèrent pas
- Vérifier que le cours est **publié** ET **indexé**
- Vérifier que l'étudiant est bien inscrit au cours
- Consulter les logs pour plus de détails

### Problème d'authentification
- Vérifier que les comptes de test sont créés (DataInitializer)
- Vérifier les mots de passe: admin123, student123, etc.

---

## 👨‍💻 Développement

### Lancer en mode développement
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Activer le hot reload
Le module spring-boot-devtools est déjà inclus pour le rechargement automatique.

### Tests
```bash
mvn test
```

---

## 📄 Licence

Projet éducatif - Mini-projet Spring Boot

---

## 📧 Support

Pour toute question ou problème:
- Consulter la documentation ci-dessus
- Vérifier les logs de l'application
- Examiner les messages d'erreur dans la console

---

## ✨ Remerciements

Projet développé dans le cadre d'un mini-projet Spring Boot intégrant:
- Spring Framework
- Spring Security
- Spring Data JPA
- Thymeleaf
- Intelligence Artificielle (LLM + RAG + Agent IA)

**Version:** 1.0.0  
**Date:** Décembre 2025
