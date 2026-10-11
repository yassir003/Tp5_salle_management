package com.example;

import com.example.dao.ReservationDao;
import com.example.dao.SalleDao;
import com.example.model.Reservation;
import com.example.model.Salle;
import com.example.repository.SalleRepository;
import com.example.repository.SalleRepositoryImpl;
import com.example.service.SalleService;
import com.example.service.SalleServiceImpl;
import com.example.util.Page;
import com.example.util.PaginationResult;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class App {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("SallePU");
        EntityManager em = emf.createEntityManager();

        try {
            // ==================================================================
            // 0. INITIALISATION DES DONNÉES EN BASE
            // ==================================================================
            initialiserDonnees(em);

            SalleDao salleDao = new SalleDao(em);
            SalleRepository salleRepository = new SalleRepositoryImpl(em);

            // Service avec injection manuelle de l'EntityManager
            SalleService salleService = new SalleServiceImpl();
            ((SalleServiceImpl) salleService).setEntityManager(em);

            LocalDate today = LocalDate.now();
            LocalDateTime debutFiltre = LocalDateTime.of(today, LocalTime.of(10, 0));
            LocalDateTime finFiltre = LocalDateTime.of(today, LocalTime.of(11, 30));

            // ==================================================================
            // 1. TESTS DE DISPONIBILITÉ PAR REQUÊTE JPQL (SalleDao)
            // ==================================================================
            System.out.println("======================================================================");
            System.out.println("=== 1. TESTS DE DISPONIBILITÉ PAR JPQL ===");
            System.out.println("======================================================================");

            testerDisponibilite(salleDao, today, LocalTime.of(10, 0), LocalTime.of(11, 30),
                    "Cas 1 : Créneau 10:00-11:30 (Einstein occupée -> 5 salles libres)");

            testerDisponibilite(salleDao, today, LocalTime.of(12, 0), LocalTime.of(14, 0),
                    "Cas 2 : Créneau 12:00-14:00 (Pause -> 6 salles libres)");

            testerDisponibilite(salleDao, today, LocalTime.of(15, 0), LocalTime.of(16, 0),
                    "Cas 3 : Créneau 15:00-16:00 (Turing occupée -> 5 salles libres)");

            testerDisponibilite(salleDao, today, LocalTime.of(8, 0), LocalTime.of(19, 0),
                    "Cas 4 : Journée complète (Einstein et Turing occupées -> 4 salles libres)");

            // ==================================================================
            // 2. TESTS DE RECHERCHE MULTI-CRITÈRES DYNAMIQUE AVEC L'API CRITERIA
            // ==================================================================
            System.out.println("\n======================================================================");
            System.out.println("=== 2. TESTS DE RECHERCHE MULTI-CRITÈRES AVEC L'API CRITERIA ===");
            System.out.println("======================================================================");

            testerCriteria(salleDao, null, null, null, null,
                    "Critères : Aucun filtre (doit renvoyer toutes les salles)");

            testerCriteria(salleDao, "ein", null, null, null,
                    "Critères : nom contenant 'ein' (insensible à la casse)");

            testerCriteria(salleDao, null, 40, null, null,
                    "Critères : capacité >= 40 (Einstein: 50, Pascal: 45, Newton: 60)");

            testerCriteria(salleDao, "Salle", 30, debutFiltre, finFiltre,
                    "Critères combinés : nom 'Salle' + capacité >= 30 + créneau 10:00-11:30 (Einstein exclue)");

            testerCriteria(salleDao, null, 100, null, null,
                    "Critères : capacité >= 100 (aucun résultat)");

            // ==================================================================
            // 3. TESTS DU SYSTÈME DE PAGINATION CÔTÉ SERVEUR (Page<T>)
            // ==================================================================
            System.out.println("\n======================================================================");
            System.out.println("=== 3. TESTS DU SYSTÈME DE PAGINATION CÔTÉ SERVEUR (Page<T>) ===");
            System.out.println("======================================================================");

            int pageSize = 2;
            Page<Salle> page1 = salleDao.findAll(1, pageSize);
            afficherPage(page1, "Page 1 / " + page1.getTotalPages() + " (Toutes les salles, taille = " + pageSize + ")");

            Page<Salle> page2 = salleDao.findAll(2, pageSize);
            afficherPage(page2, "Page 2 / " + page2.getTotalPages() + " (Toutes les salles, taille = " + pageSize + ")");

            Page<Salle> page3 = salleDao.findAll(3, pageSize);
            afficherPage(page3, "Page 3 / " + page3.getTotalPages() + " (Toutes les salles, taille = " + pageSize + ")");

            // Pagination avec filtres combinés (capacité >= 35, créneau 10:00-11:30)
            Page<Salle> filterPage1 = salleDao.searchSalles(null, 35, debutFiltre, finFiltre, 1, 2);
            afficherPage(filterPage1, "Filtre [cap >= 35, créneau 10h-11h30] - Page 1");

            Page<Salle> filterPage2 = salleDao.searchSalles(null, 35, debutFiltre, finFiltre, 2, 2);
            afficherPage(filterPage2, "Filtre [cap >= 35, créneau 10h-11h30] - Page 2");

            // ==================================================================
            // 4. TESTS DU REPOSITORY JPA (SalleRepository & SalleRepositoryImpl)
            // ==================================================================
            System.out.println("\n======================================================================");
            System.out.println("=== 4. TESTS DU REPOSITORY JPA (SalleRepository) ===");
            System.out.println("======================================================================");

            List<Salle> repoDispo = salleRepository.findAvailableRooms(debutFiltre, finFiltre);
            System.out.println("Repository.findAvailableRooms (10h-11h30) -> " + repoDispo.size() + " salles disponibles :");
            for (Salle s : repoDispo) {
                System.out.println("  -> " + s.getNom());
            }

            long totalRoomsCap35 = salleRepository.countRooms(null, 35, debutFiltre, finFiltre);
            List<Salle> repoPagedRooms = salleRepository.searchRooms(null, 35, debutFiltre, finFiltre, 1, 2);

            System.out.println("\nRepository.countRooms (cap >= 35, 10h-11h30) : " + totalRoomsCap35);
            System.out.println("Repository.searchRooms (page 1, size 2) : " + repoPagedRooms.size() + " salles retournées :");
            for (Salle s : repoPagedRooms) {
                System.out.println("  -> " + s.getNom() + " (Capacité : " + s.getCapacite() + ")");
            }

            // ==================================================================
            // 5. TESTS DE LA COUCHE SERVICE MÉTIER (SalleService & SalleServiceImpl)
            // ==================================================================
            System.out.println("\n======================================================================");
            System.out.println("=== 5. TESTS DE LA COUCHE SERVICE MÉTIER (SalleService) ===");
            System.out.println("======================================================================");

            // Test avec créneau spécifique (exemple du TP : 2024-06-01 de 09:00 à 11:00)
            LocalDateTime debutTp = LocalDateTime.of(2024, 6, 1, 9, 0);
            LocalDateTime finTp = LocalDateTime.of(2024, 6, 1, 11, 0);

            List<Salle> sallesDisponiblesTp = salleService.getAvailableRooms(debutTp, finTp);
            System.out.println("Salles disponibles entre " + debutTp + " et " + finTp + " (Einstein occupée 09h30-12h) :");
            for (Salle salle : sallesDisponiblesTp) {
                System.out.println(" - " + salle.getNom() + " (Capacité: " + salle.getCapacite() + ")");
            }

            // Test searchRooms & countRooms via le service
            long totalCap45 = salleService.countRooms(null, 45, null, null);
            List<Salle> roomsCap45 = salleService.searchRooms(null, 45, null, null, 1, 10);
            System.out.println("\nService.countRooms (cap >= 45) -> " + totalCap45);
            System.out.println("Service.searchRooms (cap >= 45) ->");
            for (Salle s : roomsCap45) {
                System.out.println("  -> " + s.getNom() + " (Capacité : " + s.getCapacite() + ")");
            }

            // Test règle métier : début > fin
            System.out.print("\nTest règle métier (validation début > fin) : ");
            try {
                salleService.getAvailableRooms(
                        LocalDateTime.of(today, LocalTime.of(16, 0)),
                        LocalDateTime.of(today, LocalTime.of(10, 0))
                );
                System.out.println("ÉCHEC (l'exception n'a pas été levée)");
            } catch (IllegalArgumentException ex) {
                System.out.println("SUCCÈS -> Exception métier levée : " + ex.getMessage());
            }

            // ==================================================================
            // 6. TESTS DE LA CLASSE UTILITAIRE PaginationResult<T>
            // ==================================================================
            System.out.println("\n======================================================================");
            System.out.println("=== 6. TESTS DE LA CLASSE UTILITAIRE PaginationResult<T> ===");
            System.out.println("======================================================================");

            PaginationResult<Salle> pResult = salleService.searchRoomsPaginated(null, 30, null, null, 1, 2);
            System.out.println("PaginationResult obtenu (page 1, taille 2, cap >= 30) :");
            System.out.println(" - Page courante  : " + pResult.getCurrentPage());
            System.out.println(" - Taille page    : " + pResult.getPageSize());
            System.out.println(" - Total pages    : " + pResult.getTotalPages());
            System.out.println(" - Total éléments : " + pResult.getTotalItems());
            System.out.println(" - A précédente   : " + pResult.hasPrevious());
            System.out.println(" - A suivante     : " + pResult.hasNext());
            System.out.println(" - Éléments (" + pResult.getItems().size() + ") :");
            for (Salle s : pResult.getItems()) {
                System.out.println("   * [ID " + s.getId() + "] " + s.getNom() + " (Capacité : " + s.getCapacite() + ")");
            }

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            em.close();
            emf.close();
        }
    }

    private static void initialiserDonnees(EntityManager em) {
        em.getTransaction().begin();

        SalleDao salleDao = new SalleDao(em);
        ReservationDao reservationDao = new ReservationDao(em);

        // 1. Création des 6 salles
        Salle einstein = new Salle("Salle Einstein", 50);
        Salle turing = new Salle("Salle Turing", 30);
        Salle lovelace = new Salle("Salle Lovelace", 20);
        Salle pascal = new Salle("Salle Pascal", 45);
        Salle newton = new Salle("Salle Newton", 60);
        Salle curie = new Salle("Salle Curie", 35);

        salleDao.save(einstein);
        salleDao.save(turing);
        salleDao.save(lovelace);
        salleDao.save(pascal);
        salleDao.save(newton);
        salleDao.save(curie);

        // 2. Création des réservations
        LocalDate today = LocalDate.now();

        // Einstein réservée aujourd'hui de 09:00 à 12:00
        Reservation resEinsteinToday = new Reservation(
                einstein,
                LocalDateTime.of(today, LocalTime.of(9, 0)),
                LocalDateTime.of(today, LocalTime.of(12, 0))
        );

        // Turing réservée aujourd'hui de 14:00 à 18:00
        Reservation resTuringToday = new Reservation(
                turing,
                LocalDateTime.of(today, LocalTime.of(14, 0)),
                LocalDateTime.of(today, LocalTime.of(18, 0))
        );

        // Einstein réservée le 2024-06-01 de 09:30 à 12:00 (pour tester la date spécifique du TP)
        Reservation resEinsteinTp = new Reservation(
                einstein,
                LocalDateTime.of(2024, 6, 1, 9, 30),
                LocalDateTime.of(2024, 6, 1, 12, 0)
        );

        reservationDao.save(resEinsteinToday);
        reservationDao.save(resTuringToday);
        reservationDao.save(resEinsteinTp);

        em.getTransaction().commit();

        System.out.println("=== DONNÉES INITIALISÉES AVEC SUCCÈS ===");
        System.out.println("6 Salles enregistrées : Einstein (50), Turing (30), Lovelace (20), Pascal (45), Newton (60), Curie (35)");
        System.out.println("Réservations enregistrées :");
        System.out.println(" - Einstein : Aujourd'hui 09:00 -> 12:00 & 2024-06-01 09:30 -> 12:00");
        System.out.println(" - Turing   : Aujourd'hui 14:00 -> 18:00");
        System.out.println();
    }

    private static void testerDisponibilite(SalleDao salleDao, LocalDate date, LocalTime debut, LocalTime fin, String description) {
        LocalDateTime dtDebut = LocalDateTime.of(date, debut);
        LocalDateTime dtFin = LocalDateTime.of(date, fin);

        System.out.println("----------------------------------------------------------------------");
        System.out.println("Test JPQL : " + description);
        System.out.println("Créneau recherché : " + debut.format(TIME_FMT) + " à " + fin.format(TIME_FMT));

        List<Salle> disponibles = salleDao.findSallesDisponibles(dtDebut, dtFin);

        System.out.println("Salles disponibles trouvées (" + disponibles.size() + ") :");
        for (Salle salle : disponibles) {
            System.out.println("  -> " + salle.getNom() + " (Capacité : " + salle.getCapacite() + ")");
        }
    }

    private static void testerCriteria(SalleDao salleDao, String nom, Integer capaciteMin,
                                       LocalDateTime debut, LocalDateTime fin, String description) {
        System.out.println("----------------------------------------------------------------------");
        System.out.println("Test Criteria : " + description);
        List<Salle> resultats = salleDao.searchSalles(nom, capaciteMin, debut, fin);
        System.out.println("Résultat (" + resultats.size() + " salle(s)) :");
        for (Salle salle : resultats) {
            System.out.println("  -> " + salle.getNom() + " (Capacité : " + salle.getCapacite() + ")");
        }
    }

    private static void afficherPage(Page<Salle> page, String titre) {
        System.out.println("----------------------------------------------------------------------");
        System.out.println("Pagination : " + titre);
        System.out.println("Métadonnées : Page " + page.getPageNumber() + " / " + page.getTotalPages()
                + " | Éléments sur cette page : " + page.getContent().size()
                + " | Total éléments : " + page.getTotalElements()
                + " | A précédente : " + page.hasPrevious()
                + " | A suivante : " + page.hasNext());
        for (Salle salle : page.getContent()) {
            System.out.println("  -> [ID " + salle.getId() + "] " + salle.getNom() + " (Capacité : " + salle.getCapacite() + ")");
        }
    }
}
