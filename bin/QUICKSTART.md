# Guide de Démarrage Rapide

## 🚀 Lancer le projet en 3 minutes

### Étape 1: Vérifier les prérequis

```bash
# Vérifier Java (version 17 ou supérieure requise)
java -version

# Vérifier Maven
mvn -version
```

Si Java ou Maven ne sont pas installés:
- **Java 17**: https://adoptium.net/
- **Maven**: https://maven.apache.org/download.cgi

### Étape 2: Compiler le projet

```bash
# Depuis le répertoire mini_proj
mvn clean install
```

Cette commande va:
- Télécharger toutes les dépendances nécessaires
- Compiler le code source
- Créer le fichier JAR exécutable

### Étape 3: Lancer l'application

```bash
mvn spring-boot:run
```

Attendez le message: `Started EducationalPlatformApplication in X seconds`

### Étape 4: Accéder à l'application

Ouvrez votre navigateur et allez sur: **http://localhost:8080**

---

## 👤 Comptes de test

### Administrateur
- Username: `admin`
- Password: `admin123`

### Étudiants
- Username: `student` / Password: `student123`
- Username: `marie` / Password: `marie123`
- Username: `jean` / Password: `jean123`

---

## 📖 Scénario de démonstration complet

### 🎯 Pour l'administrateur

1. **Se connecter** avec `admin / admin123`

2. **Voir le tableau de bord**
   - 2 cours sont déjà créés
   - 3 étudiants sont inscrits

3. **Gérer les cours**
   - Cliquer sur "Gérer les cours"
   - Cours "Introduction à Java" est publié ET indexé ✅
   - Cours "Bases de données SQL" est publié ET indexé ✅

4. **Créer un nouveau cours (optionnel)**
   - Cliquer sur "Nouveau cours"
   - Saisir le titre, description et contenu
   - Enregistrer → Publier → Indexer

5. **Gérer les inscriptions**
   - Dans la liste des cours, cliquer sur "Inscrire" pour un cours
   - Ajouter ou retirer des étudiants

### 🎓 Pour l'étudiant

1. **Se connecter** avec `student / student123`

2. **Voir le tableau de bord**
   - 2 cours inscrits apparaissent
   - Statistiques personnelles

3. **Accéder à un cours**
   - Cliquer sur "Introduction à Java"
   - Lire le contenu du cours
   - Voir le bouton "Générer un Quiz IA" 🤖

4. **Générer un quiz avec l'IA**
   - Cliquer sur "Générer un Quiz IA"
   - L'IA agentique analyse votre profil
   - Un quiz adapté est créé (5 questions pour un débutant)

5. **Passer le quiz**
   - Répondre aux questions QCM
   - Cliquer sur "Soumettre le Quiz"

6. **Voir les résultats**
   - Score affiché avec animation
   - Correction détaillée de chaque question
   - Explications fournies par l'IA
   - **Recommandation personnalisée de l'IA agentique** 🤖
   - Décision de l'agent IA visible

7. **Refaire un quiz**
   - Retourner au cours
   - Cliquer à nouveau sur "Générer un Quiz IA"
   - L'IA adapte la difficulté selon votre premier score ! 🎯

---

## 🤖 Tester l'IA Agentique

### Scénario: Progression adaptative

**Premier quiz (score faible < 70%):**
```
Agent IA décide:
- Niveau: BEGINNER
- Questions: 5
- Recommandation: "Révisez le cours..."
```

**Deuxième quiz (score moyen 70-90%):**
```
Agent IA décide:
- Niveau: INTERMEDIATE (augmenté!)
- Questions: 6
- Recommandation: "Continuez vos efforts..."
```

**Troisième quiz (score excellent > 90%):**
```
Agent IA décide:
- Niveau: ADVANCED (maximum!)
- Questions: 8
- Recommandation: "Excellent ! Cours validé !"
```

### Observer l'IA en action

Sur la page de résultats, vous verrez:

1. **Recommandation de l'IA** (encadré bleu)
   - Analyse personnalisée de votre performance
   - Conseil pour la suite

2. **Décision de l'Agent IA** (encadré technique)
   - Tentatives précédentes
   - Score moyen
   - Niveau choisi
   - Justification

---

## 🔍 Explorer la base de données

### Accéder à la console H2

1. Aller sur: **http://localhost:8080/h2-console**

2. Configuration:
   - JDBC URL: `jdbc:h2:mem:educational_platform`
   - Username: `sa`
   - Password: (laisser vide)

3. Cliquer sur "Connect"

### Requêtes utiles

```sql
-- Voir tous les utilisateurs
SELECT * FROM USERS;

-- Voir tous les cours
SELECT * FROM COURSES;

-- Voir les tentatives de quiz
SELECT * FROM QUIZ_ATTEMPTS ORDER BY ATTEMPT_DATE DESC;

-- Voir les chunks RAG d'un cours
SELECT * FROM COURSE_CHUNKS WHERE COURSE_ID = 1;

-- Statistiques d'un étudiant
SELECT 
    u.FULL_NAME,
    c.TITLE,
    qa.SCORE,
    qa.DIFFICULTY,
    qa.PASSED
FROM QUIZ_ATTEMPTS qa
JOIN USERS u ON qa.STUDENT_ID = u.ID
JOIN COURSES c ON qa.COURSE_ID = c.ID
WHERE u.USERNAME = 'student'
ORDER BY qa.ATTEMPT_DATE DESC;
```

---

## 🎨 Fonctionnalités à tester

### ✅ Sécurité
- [ ] Essayer d'accéder à `/admin/courses` sans être connecté → Redirige vers login
- [ ] Se connecter comme étudiant et tenter `/admin/dashboard` → Accès refusé
- [ ] Se connecter comme admin et accéder à `/student/dashboard` → Accès refusé

### ✅ Gestion des cours
- [ ] Créer un nouveau cours
- [ ] Modifier un cours existant
- [ ] Publier un cours
- [ ] Indexer un cours (RAG)
- [ ] Supprimer un cours

### ✅ Gestion des étudiants
- [ ] Créer un nouvel étudiant
- [ ] Modifier un étudiant
- [ ] Inscrire un étudiant à un cours
- [ ] Désinscrire un étudiant

### ✅ Système IA
- [ ] Générer un quiz (premier essai → BEGINNER)
- [ ] Obtenir un score élevé → Refaire → Quiz plus difficile
- [ ] Obtenir un score faible → Refaire → Quiz maintenu au niveau débutant
- [ ] Observer les recommandations de l'IA
- [ ] Vérifier les explications des questions

### ✅ Interface
- [ ] Toutes les pages s'affichent correctement
- [ ] Les alertes de succès/erreur fonctionnent
- [ ] Navigation fluide entre les pages
- [ ] Déconnexion fonctionne

---

## 🐛 Résolution de problèmes

### Problème: Le port 8080 est déjà utilisé

**Solution:**
```bash
# Changer le port dans application.properties
server.port=8081
```

### Problème: Erreur "java: release version 17 not supported"

**Solution:**
Votre JDK est trop ancien. Installez Java 17 ou supérieur.

### Problème: Maven ne trouve pas les dépendances

**Solution:**
```bash
# Nettoyer le cache Maven et réessayer
mvn clean
mvn dependency:purge-local-repository
mvn install
```

### Problème: Les quiz ne se génèrent pas

**Vérifications:**
1. Le cours est-il **publié** ? (badge vert "Publié")
2. Le cours est-il **indexé** ? (badge bleu "Indexé")
3. L'étudiant est-il **inscrit** au cours ?

### Problème: Erreur de base de données

**Solution:**
H2 étant en mémoire, les données sont perdues à chaque redémarrage.
C'est normal ! Les données de test sont recréées automatiquement.

---

## 📊 Métriques et logs

### Voir les logs en temps réel

Les logs s'affichent dans la console où vous avez lancé `mvn spring-boot:run`

**Logs importants à surveiller:**
```
INFO - Starting indexation for course: Introduction à Java
INFO - Indexed 15 chunks for course: Introduction à Java
INFO - AI Agent: Generating adaptive quiz for student student on course Introduction à Java
INFO - AI Agent Decision: First attempt - Starting with BEGINNER level
```

### Activer les logs détaillés

Dans `application.properties`:
```properties
logging.level.com.plateforme=DEBUG
logging.level.org.springframework.security=TRACE
```

---

## 🎓 Aller plus loin

### Intégrer un vrai LLM

**Option 1: OpenAI**
```properties
# Décommenter dans application.properties
spring.ai.openai.api-key=sk-votre-clé-api
spring.ai.openai.chat.options.model=gpt-3.5-turbo
```

**Option 2: Ollama (local)**
```bash
# Installer Ollama
curl https://ollama.ai/install.sh | sh

# Télécharger un modèle
ollama pull llama2

# Dans application.properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=llama2
```

### Ajouter des cours personnalisés

1. Se connecter comme admin
2. Créer un cours avec votre contenu
3. Publier → Indexer
4. Inscrire des étudiants
5. Les étudiants peuvent générer des quiz sur votre contenu !

### Tester différents profils d'étudiants

Créez plusieurs comptes étudiants et testez:
- Un étudiant débutant (scores faibles)
- Un étudiant moyen (scores 70-80%)
- Un excellent étudiant (scores > 90%)

Observez comment l'IA adapte les quiz différemment pour chacun !

---

## 📞 Support

Si vous rencontrez des problèmes:

1. **Vérifiez les logs** dans la console
2. **Consultez la documentation** dans README.md
3. **Vérifiez l'architecture** dans ARCHITECTURE.md
4. **Testez la console H2** pour voir l'état de la base

---

## 🎉 Félicitations !

Vous avez maintenant une plateforme pédagogique complète avec:
- ✅ Authentification sécurisée
- ✅ Gestion de cours
- ✅ Système RAG fonctionnel
- ✅ IA agentique adaptative
- ✅ Interface utilisateur moderne

**Profitez de votre plateforme intelligente ! 🚀**
