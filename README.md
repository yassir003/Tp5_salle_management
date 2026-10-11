# TP5 : Requêtes avancées avec JPA - Salles disponibles, recherche multi-critères et pagination

Projet d'application Java EE / JPA démontrant la gestion des entités relationnelles, les requêtes JPQL avancées de détection de chevauchement temporel, la recherche dynamique multi-critères avec l'API Criteria, la pagination côté serveur et l'organisation en couches (DAO, Repository EJB, Service Métier).

---

## 🏛 Aperçu de l'Architecture

Le projet respecte une séparation stricte des responsabilités (Separation of Concerns) :

```text
src/
├── main/
│   ├── java/com/example/
│   │   ├── model/                  # Entités JPA (Salle, Reservation)
│   │   ├── dao/                    # Objets d'accès aux données (SalleDao, ReservationDao)
│   │   ├── repository/             # Interface & Implémentation EJB (SalleRepository, SalleRepositoryImpl)
│   │   ├── service/                # Couche métier EJB & CDI (SalleService, SalleServiceImpl)
│   │   ├── util/                   # Classes utilitaires de pagination (Page, PaginationResult)
│   │   └── App.java                # Point d'entrée d'exécution Java SE
│   └── resources/
│       └── META-INF/
│           └── persistence.xml     # Configuration de l'unité de persistance JPA (SallePU)
└── test/
    └── java/com/example/           # Tests unitaires & d'intégration (19 tests)
        ├── BaseJpaTest.java
        ├── SalleDaoTest.java
        ├── SalleRepositoryTest.java
        ├── SalleServiceTest.java
        ├── PaginationResultTest.java
        └── PageTest.java
```

---

## 🛠 Technologies & Dépendances

- **Langage :** Java 21
- **Persistance :** JPA 2.2 (`javax.persistence-api`)
- **Fournisseur ORM :** Hibernate Core 5.4.30.Final
- **Base de données :** H2 Database 1.4.200 (in-memory `jdbc:h2:mem:test`)
- **Composants d'Entreprise :** 
  - EJB 3.2.2 (`javax.ejb-api` avec `@Stateless`)
  - CDI JSR-330 (`javax.inject` avec `@Inject`)
- **Tests :** 
  - JUnit 5 Jupiter 5.10.2
  - Mockito 5.11.0 (`mockito-core`, `mockito-junit-jupiter`)
- **Build Tool :** Apache Maven 3

---

## 🗄 Modèle de Données (Entités JPA)

### 1. `Salle` ([Salle.java](src/main/java/com/example/model/Salle.java))
Représente une salle de cours ou de réunion.
- `id` : `Long`, clé primaire auto-générée (`IDENTITY`).
- `nom` : `String`, obligatoire (`nullable = false`), unique (`unique = true`).
- `capacite` : `int`, capacité d'accueil minimale.
- `reservations` : `Set<Reservation>`, relation bidirectionnelle `@OneToMany(mappedBy = "salle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)`.

### 2. `Reservation` ([Reservation.java](src/main/java/com/example/model/Reservation.java))
Représente une réservation sur un créneau horaire.
- `id` : `Long`, clé primaire auto-générée (`IDENTITY`).
- `salle` : `Salle`, relation `@ManyToOne(fetch = FetchType.LAZY)` avec clé étrangère non nulle `salle_id`.
- `debut` : `LocalDateTime`, date et heure de début.
- `fin` : `LocalDateTime`, date et heure de fin.

---

## 🔍 Requêtes Avancées

### 1. Détection de chevauchement en JPQL

Une réservation $R$ intersecte un créneau horaire recherché $[\text{début}, \text{fin}]$ si et seulement si :
$$\text{début} < R.\text{fin} \quad \text{ET} \quad \text{fin} > R.\text{debut}$$

Requête JPQL pour exclure les salles déjà réservées :
```sql
SELECT s FROM Salle s WHERE s.id NOT IN (
    SELECT r.salle.id FROM Reservation r WHERE (:debut < r.fin AND :fin > r.debut)
)
```
Cette requête est également enregistrée sous forme de requête nommée sur l'entité : `@NamedQuery(name = "Salle.findDisponibles", ...)`.

---

### 2. Recherche dynamique avec l'API Criteria

L'API Criteria permet d'ajouter des filtres conditionnels sans concaténation de chaînes JPQL :
- **Nom :** recherche insensible à la casse avec `LOWER()` et `LIKE %motif%`.
- **Capacité minimale :** `cb.ge(salle.get("capacite"), capaciteMin)`.
- **Disponibilité :** exclusion via sous-requête Criteria `cq.subquery(Long.class)`.

```java
CriteriaBuilder cb = entityManager.getCriteriaBuilder();
CriteriaQuery<Salle> cq = cb.createQuery(Salle.class);
Root<Salle> salle = cq.from(Salle.class);

List<Predicate> predicates = new ArrayList<>();
// Composition conditionnelle des filtres...
cq.select(salle).where(cb.and(predicates.toArray(new Predicate[0])));
```

---

### 3. Pagination côté serveur & `PaginationResult`

La pagination limite la taille des transferts et optimise les performances de la base de données :
- `query.setFirstResult((page - 1) * size);` $\rightarrow$ Clause SQL `OFFSET`.
- `query.setMaxResults(size);` $\rightarrow$ Clause SQL `LIMIT`.
- **Requête de comptage :** Exécution d'un `CriteriaQuery<Long>` avec `cb.count(root)` appliquant **les mêmes prédicats** que la requête principale pour garantir des métadonnées cohérentes.

Les résultats sont encapsulés dans la classe générique [PaginationResult.java](src/main/java/com/example/util/PaginationResult.java) :
- `items` : Liste des éléments de la page courante.
- `currentPage` : Numéro de la page (1-based).
- `pageSize` : Nombre d'éléments demandés par page.
- `totalPages` : Nombre total de pages ($\lceil \text{totalItems} / \text{pageSize} \rceil$).
- `totalItems` : Nombre total d'éléments répondant aux critères.
- Méthodes d'état : `hasPrevious()`, `hasNext()`, `isFirst()`, `isLast()`.

---

## 🧩 Organisation en Couches

1. **`SalleDao` ([SalleDao.java](src/main/java/com/example/dao/SalleDao.java)) :**  
   Accès direct via `EntityManager` (requêtes JPQL, NamedQueries et Criteria).
2. **`SalleRepository` ([SalleRepository.java](src/main/java/com/example/repository/SalleRepository.java)) :**  
   Interface d'abstraction de données et son implémentation `@Stateless` [SalleRepositoryImpl.java](src/main/java/com/example/repository/SalleRepositoryImpl.java).
3. **`SalleService` ([SalleService.java](src/main/java/com/example/service/SalleService.java)) :**  
   Couche service métier `@Stateless` injectant le Repository via `@Inject`, intégrant les validations métier :
   - Vérification de la chronologie des dates (`debut.isBefore(fin)`).
   - Validation des capacités minimales positives.
   - Normalisation des paramètres de pagination.

---

## 🧪 Tests & Validation

La suite de tests unitaires et d'intégration couvre l'intégralité du projet (**19 tests réussis**) :

- **[BaseJpaTest.java](src/test/java/com/example/BaseJpaTest.java) :** Fixture JPA démarrant `SallePU` et isolant chaque test dans une transaction avec rollback automatique.
- **[SalleDaoTest.java](src/test/java/com/example/SalleDaoTest.java) :** Validation des requêtes JPQL, NamedQuery, Criteria API et pagination.
- **[SalleRepositoryTest.java](src/test/java/com/example/SalleRepositoryTest.java) :** Tests d'intégration de l'implémentation du repository.
- **[SalleServiceTest.java](src/test/java/com/example/SalleServiceTest.java) :** Tests unitaires isolés avec Mockito pour les règles métier et la délégation.
- **[PaginationResultTest.java](src/test/java/com/example/PaginationResultTest.java) & [PageTest.java](src/test/java/com/example/PageTest.java) :** Tests des calculs de pagination et indicateurs de navigation.

---

## 🚀 Guide d'Exécution

### 1. Prérequis
- Java 21+ installé et configuré (`JAVA_HOME`).
- Apache Maven 3.8+ installé.

### 2. Compilation
```bash
mvn clean compile
```

### 3. Lancer les Tests Unitaires & d'Intégration
```bash
mvn test
```

### 4. Lancer l'Application de Démonstration ([App.java](src/main/java/com/example/App.java))
```bash
mvn exec:java "-Dexec.mainClass=com.example.App"
```

Cette classe principale initialise 6 salles en mémoire H2, insère des réservations types et exécute automatiquement les 6 étapes de tests dans la console.
