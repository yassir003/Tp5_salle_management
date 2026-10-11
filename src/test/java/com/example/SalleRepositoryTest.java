package com.example;

import com.example.model.Reservation;
import com.example.model.Salle;
import com.example.repository.SalleRepository;
import com.example.repository.SalleRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SalleRepositoryTest extends BaseJpaTest {

    private SalleRepository repository;
    private Salle salle1;
    private Salle salle2;

    @BeforeEach
    public void init() {
        repository = new SalleRepositoryImpl(em);

        salle1 = new Salle("Salle Bleue", 30);
        salle2 = new Salle("Salle Verte", 50);

        repository.save(salle1);
        repository.save(salle2);

        // Réservation sur salle1 de 09:00 à 11:00
        Reservation res = new Reservation(
                salle1,
                LocalDateTime.of(2026, 6, 1, 9, 0),
                LocalDateTime.of(2026, 6, 1, 11, 0)
        );
        em.persist(res);
        em.flush();
    }

    @Test
    @DisplayName("Repository : findAvailableRooms doit exclure les réservations qui se chevauchent")
    public void testFindAvailableRooms() {
        LocalDateTime debut = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 6, 1, 12, 0);

        List<Salle> dispo = repository.findAvailableRooms(debut, fin);

        assertEquals(1, dispo.size());
        assertEquals("Salle Verte", dispo.get(0).getNom());
    }

    @Test
    @DisplayName("Repository : countRooms et searchRooms paginés")
    public void testSearchRoomsAndCount() {
        long count = repository.countRooms(null, 20, null, null);
        assertEquals(2, count);

        List<Salle> page1 = repository.searchRooms(null, 20, null, null, 1, 1);
        assertEquals(1, page1.size());
    }

    @Test
    @DisplayName("Repository : CRUD basique (findById, findAll)")
    public void testCrud() {
        Salle found = repository.findById(salle1.getId());
        assertNotNull(found);
        assertEquals("Salle Bleue", found.getNom());

        List<Salle> all = repository.findAll();
        assertEquals(2, all.size());
    }
}
