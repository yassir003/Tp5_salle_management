package com.example;

import com.example.util.PaginationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PaginationResultTest {

    @Test
    @DisplayName("Devrait calculer correctement le nombre total de pages")
    public void testCalculTotalPages() {
        PaginationResult<String> r1 = new PaginationResult<>(List.of("A", "B"), 1, 2, 5);
        assertEquals(3, r1.getTotalPages()); // ceil(5 / 2) = 3

        PaginationResult<String> r2 = new PaginationResult<>(List.of("A", "B"), 1, 2, 4);
        assertEquals(2, r2.getTotalPages()); // ceil(4 / 2) = 2

        PaginationResult<String> r3 = new PaginationResult<>(List.of(), 1, 10, 0);
        assertEquals(0, r3.getTotalPages());
    }

    @Test
    @DisplayName("Devrait vérifier les drapeaux de navigation (hasPrevious, hasNext, isFirst, isLast)")
    public void testFlagsNavigation() {
        // Première page sur 3
        PaginationResult<String> firstPage = new PaginationResult<>(List.of("A"), 1, 1, 3);
        assertTrue(firstPage.isFirst());
        assertFalse(firstPage.isLast());
        assertFalse(firstPage.hasPrevious());
        assertTrue(firstPage.hasNext());

        // Deuxième page sur 3
        PaginationResult<String> middlePage = new PaginationResult<>(List.of("B"), 2, 1, 3);
        assertFalse(middlePage.isFirst());
        assertFalse(middlePage.isLast());
        assertTrue(middlePage.hasPrevious());
        assertTrue(middlePage.hasNext());

        // Dernière page sur 3
        PaginationResult<String> lastPage = new PaginationResult<>(List.of("C"), 3, 1, 3);
        assertFalse(lastPage.isFirst());
        assertTrue(lastPage.isLast());
        assertTrue(lastPage.hasPrevious());
        assertFalse(lastPage.hasNext());
    }
}
