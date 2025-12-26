package com.plateforme.educational.config;

import com.plateforme.educational.entity.Course;
import com.plateforme.educational.entity.User;
import com.plateforme.educational.repository.CourseRepository;
import com.plateforme.educational.repository.UserRepository;
import com.plateforme.educational.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseService courseService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting data initialization...");

        // Créer les utilisateurs de démonstration
        createUsers();
        
        // Créer les cours de démonstration
        createCourses();

        log.info("Data initialization completed!");
    }

    private void createUsers() {
        if (userRepository.count() > 0) {
            log.info("Users already exist, skipping user creation");
            return;
        }

        // Créer un administrateur
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setEmail("admin@plateforme.com");
        admin.setFullName("Administrateur Principal");
        admin.setRole(User.Role.ADMIN);
        admin.setEnabled(true);
        userRepository.save(admin);
        log.info("Created admin user: admin / admin123");

        // Créer des étudiants
        User student1 = new User();
        student1.setUsername("student");
        student1.setPassword(passwordEncoder.encode("student123"));
        student1.setEmail("etudiant@plateforme.com");
        student1.setFullName("Étudiant Test");
        student1.setRole(User.Role.STUDENT);
        student1.setEnabled(true);
        userRepository.save(student1);
        log.info("Created student user: student / student123");

        User student2 = new User();
        student2.setUsername("marie");
        student2.setPassword(passwordEncoder.encode("marie123"));
        student2.setEmail("marie@plateforme.com");
        student2.setFullName("Marie Dupont");
        student2.setRole(User.Role.STUDENT);
        student2.setEnabled(true);
        userRepository.save(student2);
        log.info("Created student user: marie / marie123");

        User student3 = new User();
        student3.setUsername("jean");
        student3.setPassword(passwordEncoder.encode("jean123"));
        student3.setEmail("jean@plateforme.com");
        student3.setFullName("Jean Martin");
        student3.setRole(User.Role.STUDENT);
        student3.setEnabled(true);
        userRepository.save(student3);
        log.info("Created student user: jean / jean123");
    }

    private void createCourses() {
        if (courseRepository.count() > 0) {
            log.info("Courses already exist, skipping course creation");
            return;
        }

        User admin = userRepository.findByUsername("admin").orElseThrow();
        User student1 = userRepository.findByUsername("student").orElseThrow();
        User student2 = userRepository.findByUsername("marie").orElseThrow();

        // Cours 1: Introduction à Java (VERSION ULTRA-MINIMALE)
        Course javaCourse = new Course();
        javaCourse.setTitle("Introduction à Java");
        javaCourse.setDescription("Apprenez les bases de la programmation Java");
        javaCourse.setContent("""
                Java est un langage orienté objet créé en 1995.
                Il est portable et robuste.
                On utilise des classes et des objets.
                Les types primitifs sont: int, double, boolean, char.
                Les structures de contrôle sont: if, for, while.
                
                Java est un langage de programmation orienté objet créé par James Gosling chez Sun Microsystems en 1995.
                C'est l'un des langages les plus populaires au monde, utilisé pour développer des applications web, 
                mobiles (Android), desktop et des systèmes d'entreprise.
                
                Caractéristiques principales de Java:
                
                1. Portabilité: Java suit le principe "Write Once, Run Anywhere" (WORA). Le code Java est compilé 
                en bytecode qui peut s'exécuter sur n'importe quelle machine virtuelle Java (JVM), indépendamment 
                de l'architecture matérielle ou du système d'exploitation.
                
                2. Orienté Objet: Java est basé sur les concepts de la programmation orientée objet (POO). 
                Tout en Java est un objet, à l'exception des types primitifs. Les quatre piliers de la POO en Java sont:
                   - Encapsulation: regroupement des données et des méthodes dans une classe
                   - Héritage: capacité d'une classe à hériter des propriétés d'une autre classe
                   - Polymorphisme: capacité d'un objet à prendre plusieurs formes
                   - Abstraction: masquage des détails d'implémentation complexes
                
                3. Gestion automatique de la mémoire: Java dispose d'un garbage collector qui gère automatiquement 
                l'allocation et la libération de la mémoire, réduisant ainsi les risques de fuites mémoire.
                
                4. Sécurité: Java a été conçu avec la sécurité à l'esprit. Le bytecode est vérifié avant l'exécution 
                pour détecter les violations de sécurité. Java dispose également d'un gestionnaire de sécurité qui 
                contrôle l'accès aux ressources système.
                
                5. Multi-threading: Java supporte nativement la programmation concurrente grâce aux threads, 
                permettant l'exécution simultanée de plusieurs parties d'un programme.
                
                Structure de base d'un programme Java:
                
                Un programme Java simple commence toujours par une classe contenant une méthode main():
                
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                
                Variables et types de données:
                
                Java est un langage fortement typé. Les types primitifs incluent:
                - byte, short, int, long (entiers)
                - float, double (nombres à virgule flottante)
                - boolean (vrai/faux)
                - char (caractère)
                
                Les variables doivent être déclarées avec leur type:
                int age = 25;
                String nom = "Alice";
                boolean estEtudiant = true;
                
                Structures de contrôle:
                
                Java dispose des structures de contrôle classiques:
                - if/else pour les conditions
                - switch pour les sélections multiples
                - for, while, do-while pour les boucles
                
                Concepts avancés:
                
                Collections: Java fournit le Framework Collections qui inclut des structures de données comme 
                ArrayList, HashMap, HashSet, etc.
                
                Exceptions: Java utilise un système robuste de gestion des exceptions avec try-catch-finally 
                pour gérer les erreurs.
                
                Streams et Lambda: Depuis Java 8, le langage supporte la programmation fonctionnelle avec 
                les expressions lambda et l'API Stream pour le traitement de collections de manière déclarative.
                """);
        javaCourse.setPublished(true);
        javaCourse.setCreator(admin);
        courseRepository.save(javaCourse);
        
        // Inscrire des étudiants
        courseService.enrollStudent(javaCourse.getId(), student1.getId());
        courseService.enrollStudent(javaCourse.getId(), student2.getId());
        
        // Indexation désactivée au démarrage pour économiser la mémoire
        // Indexez manuellement depuis l'interface Admin après le démarrage
        // courseService.indexCourse(javaCourse.getId());
        log.info("Created Java course (indexation manuelle requise)");

        // Cours 2: Base de données SQL (VERSION ULTRA-MINIMALE)
        Course sqlCourse = new Course();
        sqlCourse.setTitle("Bases de données SQL");
        sqlCourse.setDescription("Maîtrisez SQL et les bases de données relationnelles");
        sqlCourse.setContent("""
                SQL permet de gérer des bases de données.
                Les commandes principales sont SELECT, INSERT, UPDATE et DELETE.
                On utilise WHERE pour filtrer les résultats.
                
                Qu'est-ce qu'une base de données relationnelle?
                
                Une base de données relationnelle organise les données en tables (relations) composées de lignes 
                (enregistrements) et de colonnes (attributs). Les tables peuvent être liées entre elles par des 
                relations, d'où le terme "relationnel".
                
                Concepts fondamentaux:
                
                1. Tables: Une table représente une entité (par exemple, Clients, Produits, Commandes). 
                Chaque table a un schéma défini avec des colonnes de types spécifiques.
                
                2. Clés primaires: Chaque table doit avoir une clé primaire (PRIMARY KEY) qui identifie 
                de manière unique chaque enregistrement. Exemple: id_client dans une table Clients.
                
                3. Clés étrangères: Les clés étrangères (FOREIGN KEY) créent des relations entre tables. 
                Par exemple, une table Commandes peut avoir une clé étrangère id_client qui référence 
                la clé primaire de la table Clients.
                
                4. Contraintes: Les contraintes garantissent l'intégrité des données:
                   - NOT NULL: la colonne ne peut pas contenir de valeur nulle
                   - UNIQUE: toutes les valeurs de la colonne doivent être uniques
                   - CHECK: vérifie qu'une condition est respectée
                   - DEFAULT: définit une valeur par défaut
                
                Langage SQL - Les différentes catégories:
                
                DDL (Data Definition Language) - Définition des structures:
                - CREATE: créer une base, table, vue, index
                - ALTER: modifier la structure d'une table
                - DROP: supprimer une base ou table
                - TRUNCATE: vider une table
                
                Exemple de création de table:
                CREATE TABLE Clients (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    nom VARCHAR(100) NOT NULL,
                    email VARCHAR(100) UNIQUE,
                    date_inscription DATE DEFAULT CURRENT_DATE
                );
                
                DML (Data Manipulation Language) - Manipulation des données:
                - SELECT: interroger et récupérer des données
                - INSERT: ajouter de nouveaux enregistrements
                - UPDATE: modifier des enregistrements existants
                - DELETE: supprimer des enregistrements
                
                Requête SELECT - La base:
                SELECT colonne1, colonne2 FROM table WHERE condition;
                
                Exemples pratiques:
                SELECT * FROM Clients;  -- Tous les clients
                SELECT nom, email FROM Clients WHERE id = 5;  -- Client spécifique
                SELECT * FROM Produits WHERE prix > 100 ORDER BY prix DESC;  -- Produits triés
                
                Jointures (JOINS):
                
                Les jointures permettent de combiner des données de plusieurs tables:
                
                - INNER JOIN: retourne les enregistrements ayant une correspondance dans les deux tables
                - LEFT JOIN: retourne tous les enregistrements de la table gauche et les correspondances de droite
                - RIGHT JOIN: inverse du LEFT JOIN
                - FULL OUTER JOIN: retourne tous les enregistrements des deux tables
                
                Exemple de jointure:
                SELECT Clients.nom, Commandes.montant
                FROM Clients
                INNER JOIN Commandes ON Clients.id = Commandes.id_client;
                
                Agrégations et fonctions:
                
                SQL offre des fonctions d'agrégation pour analyser les données:
                - COUNT(): compte le nombre d'enregistrements
                - SUM(): somme des valeurs
                - AVG(): moyenne
                - MAX(), MIN(): valeurs maximale et minimale
                - GROUP BY: regroupe les résultats
                - HAVING: filtre les groupes (équivalent de WHERE pour les agrégations)
                
                Exemple d'agrégation:
                SELECT categorie, COUNT(*) as nb_produits, AVG(prix) as prix_moyen
                FROM Produits
                GROUP BY categorie
                HAVING COUNT(*) > 5;
                
                Sous-requêtes:
                
                Une sous-requête est une requête imbriquée dans une autre requête:
                SELECT nom FROM Clients
                WHERE id IN (SELECT id_client FROM Commandes WHERE montant > 1000);
                
                Index et performance:
                
                Les index améliorent les performances des requêtes SELECT en créant une structure de données 
                optimisée pour la recherche. Cependant, ils ralentissent les INSERT, UPDATE et DELETE.
                
                CREATE INDEX idx_nom ON Clients(nom);
                
                Transactions et ACID:
                
                Les transactions garantissent l'intégrité des données avec les propriétés ACID:
                - Atomicité: tout ou rien
                - Cohérence: les données restent cohérentes
                - Isolation: les transactions sont isolées les unes des autres
                - Durabilité: les changements sont permanents
                
                BEGIN TRANSACTION;
                UPDATE Comptes SET solde = solde - 100 WHERE id = 1;
                UPDATE Comptes SET solde = solde + 100 WHERE id = 2;
                COMMIT;
                """);
        sqlCourse.setPublished(true);
        sqlCourse.setCreator(admin);
        courseRepository.save(sqlCourse);
        
        courseService.enrollStudent(sqlCourse.getId(), student1.getId());
        
        // Indexation désactivée au démarrage pour économiser la mémoire
        // Indexez manuellement depuis l'interface Admin après le démarrage
        // courseService.indexCourse(sqlCourse.getId());
        log.info("Created SQL course (indexation manuelle requise)");

        // Cours 3: Intelligence Artificielle (non indexé)
        Course aiCourse = new Course();
        aiCourse.setTitle("Introduction à l'Intelligence Artificielle");
        aiCourse.setDescription("Découvrez les concepts fondamentaux de l'IA et du Machine Learning");
        aiCourse.setContent("""
                Introduction à l'Intelligence Artificielle
                
                L'Intelligence Artificielle (IA) est une branche de l'informatique qui vise à créer des systèmes 
                capables d'effectuer des tâches nécessitant normalement l'intelligence humaine.
                
                Domaines de l'IA:
                - Machine Learning (Apprentissage automatique)
                - Deep Learning (Apprentissage profond)
                - Natural Language Processing (Traitement du langage naturel)
                - Computer Vision (Vision par ordinateur)
                - Robotique
                
                Ce cours sera bientôt indexé pour la génération de quiz.
                """);
        aiCourse.setPublished(false);
        aiCourse.setCreator(admin);
        courseRepository.save(aiCourse);
        log.info("Created AI course (not published)");
    }
}
