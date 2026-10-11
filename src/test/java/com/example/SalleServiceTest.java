package com.example;

import com.example.model.Salle;
import com.example.repository.SalleRepository;
import com.example.service.SalleServiceImpl;
import com.example.util.PaginationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SalleServiceTest {

    private SalleRepository salleRepository;
    private SalleServiceImpl salleService;

    @org.junit.jupiter.api.BeforeEach
    public void setUp() {
        salleRepository = mock(SalleRepository.class);
        salleService = new SalleServiceImpl(salleRepository);
    }

    @Test
    @DisplayName("Service : Devrait lever IllegalArgumentException quand début > fin")
    public void testGetAvailableRooms_DateInvalide() {
        LocalDateTime debut = LocalDateTime.of(2026, 6, 1, 14, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 6, 1, 10, 0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            salleService.getAvailableRooms(debut, fin);
        });

        assertTrue(ex.getMessage().contains("antérieure"));
        verifyNoInteractions(salleRepository);
    }

    @Test
    @DisplayName("Service : Devrait lever IllegalArgumentException quand capacité minimale négative")
    public void testSearchRooms_CapaciteNegative() {
        assertThrows(IllegalArgumentException.class, () -> {
            salleService.searchRooms(null, -5, null, null, 1, 10);
        });

        verifyNoInteractions(salleRepository);
    }

    @Test
    @DisplayName("Service : getAvailableRooms délègue correctement au Repository")
    public void testGetAvailableRooms_Delegation() {
        LocalDateTime debut = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 6, 1, 12, 0);

        Salle s1 = new Salle("Salle 1", 30);
        when(salleRepository.findAvailableRooms(debut, fin)).thenReturn(List.of(s1));

        List<Salle> result = salleService.getAvailableRooms(debut, fin);

        assertEquals(1, result.size());
        assertEquals("Salle 1", result.get(0).getNom());
        verify(salleRepository, times(1)).findAvailableRooms(debut, fin);
    }

    @Test
    @DisplayName("Service : Normalise les valeurs de page et size si inférieures à 1")
    public void testSearchRooms_NormalisationPageSize() {
        salleService.searchRooms("Test", 20, null, null, 0, -5);

        // Doit appeler searchRooms avec page = 1 et size = 10
        verify(salleRepository, times(1)).searchRooms("Test", 20, null, null, 1, 10);
    }

    @Test
    @DisplayName("Service : searchRoomsPaginated retourne un PaginationResult complet")
    public void testSearchRoomsPaginated() {
        Salle s1 = new Salle("Salle A", 30);
        Salle s2 = new Salle("Salle B", 40);

        when(salleRepository.searchRooms(null, 25, null, null, 1, 2))
                .thenReturn(Arrays.asList(s1, s2));
        when(salleRepository.countRooms(null, 25, null, null))
                .thenReturn(5L);

        PaginationResult<Salle> result = salleService.searchRoomsPaginated(null, 25, null, null, 1, 2);

        assertNotNull(result);
        assertEquals(1, result.getCurrentPage());
        assertEquals(2, result.getPageSize());
        assertEquals(5L, result.getTotalItems());
        assertEquals(3, result.getTotalPages()); // ceil(5 / 2) = 3
        assertEquals(2, result.getItems().size());
        assertFalse(result.hasPrevious());
        assertTrue(result.hasNext());
    }
}
