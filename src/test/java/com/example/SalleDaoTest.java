package com.example;

import com.example.dao.ReservationDao;
import com.example.dao.SalleDao;
import com.example.model.Reservation;
import com.example.model.Salle;
import com.example.util.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SalleDaoTest extends BaseJpaTest {

    private SalleDao salleDao;
    private ReservationDao reservationDao;

    private Salle salleA;
    private Salle salleB;
    private Salle salleC;

    @BeforeEach
    public void initData() {
        salleDao = new SalleDao(em);
        reservationDao = new ReservationDao(em);

        salleA = new Salle("Salle Alpha", 40);
        salleB = new Salle("Salle Beta", 25);
        salleC = new Salle("Salle Gamma", 60);

        salleDao.save(salleA);
        salleDao.save(salleB);
        salleDao.save(salleC);

        // Réservation sur salleA : 10:00 -> 12:00
        Reservation resA = new Reservation(
                salleA,
                LocalDateTime.of(2026, 5, 10, 10, 0),
                LocalDateTime.of(2026, 5, 10, 12, 0)
        );
        reservationDao.save(resA);

        // Réservation sur salleB : 14:00 -> 16:00
        Reservation resB = new Reservation(
                salleB,
                LocalDateTime.of(2026, 5, 10, 14, 0),
                LocalDateTime.of(2026, 5, 10, 16, 0)
        );
        reservationDao.save(resB);

        em.flush();
    }

    @Test
    @DisplayName("Devrait sauvegarder et retrouver une salle par son ID")
    public void testSaveAndFindById() {
        Salle found = salleDao.findById(salleA.getId());
        assertNotNull(found);
        assertEquals("Salle Alpha", found.getNom());
        assertEquals(40, found.getCapacite());
    }

    @Test
    @DisplayName("JPQL : Devrait exclure les salles ayant une réservation chevauchante")
    public void testFindSallesDisponibles_ExclutChevauchement() {
        // Créneau 10:30 -> 11:30 chevauche salleA (10:00-12:00)
        LocalDateTime debut = LocalDateTime.of(2026, 5, 10, 10, 30);
        LocalDateTime fin = LocalDateTime.of(2026, 5, 10, 11, 30);

        List<Salle> disponibles = salleDao.findSallesDisponibles(debut, fin);

        assertEquals(2, disponibles.size());
        assertTrue(disponibles.stream().noneMatch(s -> s.getId().equals(salleA.getId())));
        assertTrue(disponibles.stream().anyMatch(s -> s.getId().equals(salleB.getId())));
        assertTrue(disponibles.stream().anyMatch(s -> s.getId().equals(salleC.getId())));
    }

    @Test
    @DisplayName("JPQL : Devrait inclure toutes les salles lorsqu'il n'y a aucun chevauchement")
    public void testFindSallesDisponibles_AucunChevauchement() {
        // Créneau 12:00 -> 14:00 (pause entre salleA et salleB)
        LocalDateTime debut = LocalDateTime.of(2026, 5, 10, 12, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 5, 10, 14, 0);

        List<Salle> disponibles = salleDao.findSallesDisponibles(debut, fin);

        assertEquals(3, disponibles.size());
    }

    @Test
    @DisplayName("NamedQuery : Devrait fonctionner à l'identique de la requête JPQL dynamique")
    public void testFindSallesDisponiblesViaNamedQuery() {
        LocalDateTime debut = LocalDateTime.of(2026, 5, 10, 10, 30);
        LocalDateTime fin = LocalDateTime.of(2026, 5, 10, 11, 30);

        List<Salle> disponibles = salleDao.findSallesDisponiblesViaNamedQuery(debut, fin);

        assertEquals(2, disponibles.size());
        assertTrue(disponibles.stream().noneMatch(s -> s.getId().equals(salleA.getId())));
    }

    @Test
    @DisplayName("Criteria : Filtrage dynamique par nom insensible à la casse")
    public void testSearchSalles_ParNom() {
        List<Salle> resultats = salleDao.searchSalles("alpha", null, null, null);
        assertEquals(1, resultats.size());
        assertEquals("Salle Alpha", resultats.get(0).getNom());
    }

    @Test
    @DisplayName("Criteria : Filtrage par capacité minimale")
    public void testSearchSalles_ParCapaciteMin() {
        List<Salle> resultats = salleDao.searchSalles(null, 40, null, null);
        assertEquals(2, resultats.size()); // salleA (40) et salleC (60)
        assertTrue(resultats.stream().allMatch(s -> s.getCapacite() >= 40));
    }

    @Test
    @DisplayName("Criteria : Filtrage combiné (nom + capacité + créneau horaire)")
    public void testSearchSalles_Combine() {
        // Nom contient 'Salle', capacité >= 30, créneau 10:30-11:30 (salleA occupée)
        // salleA a 40 mais occupée -> exclue
        // salleB a 25 < 30 -> exclue
        // salleC a 60 et disponible -> seule retenue
        LocalDateTime debut = LocalDateTime.of(2026, 5, 10, 10, 30);
        LocalDateTime fin = LocalDateTime.of(2026, 5, 10, 11, 30);

        List<Salle> resultats = salleDao.searchSalles("Salle", 30, debut, fin);

        assertEquals(1, resultats.size());
        assertEquals("Salle Gamma", resultats.get(0).getNom());
    }

    @Test
    @DisplayName("Pagination Criteria : Devrait respecter pageSize, offset et calculer le total")
    public void testSearchSalles_Pagination() {
        Page<Salle> page1 = salleDao.findAll(1, 2);
        assertEquals(1, page1.getPageNumber());
        assertEquals(2, page1.getPageSize());
        assertEquals(2, page1.getContent().size());
        assertEquals(3, page1.getTotalElements());
        assertEquals(2, page1.getTotalPages());
        assertFalse(page1.hasPrevious());
        assertTrue(page1.hasNext());

        Page<Salle> page2 = salleDao.findAll(2, 2);
        assertEquals(2, page2.getPageNumber());
        assertEquals(1, page2.getContent().size());
        assertTrue(page2.hasPrevious());
        assertFalse(page2.hasNext());
    }
}
