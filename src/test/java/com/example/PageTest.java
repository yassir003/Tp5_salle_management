package com.example;

import com.example.util.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PageTest {

    @Test
    @DisplayName("Page : Devrait calculer correctement le nombre total de pages et les états de navigation")
    public void testPageProperties() {
        Page<String> p1 = new Page<>(List.of("A", "B"), 1, 2, 5);
        assertEquals(1, p1.getPageNumber());
        assertEquals(2, p1.getPageSize());
        assertEquals(5, p1.getTotalElements());
        assertEquals(3, p1.getTotalPages());
        assertEquals(2, p1.getContent().size());
        assertTrue(p1.isFirst());
        assertFalse(p1.isLast());
        assertFalse(p1.hasPrevious());
        assertTrue(p1.hasNext());

        Page<String> pLast = new Page<>(List.of("E"), 3, 2, 5);
        assertEquals(3, pLast.getPageNumber());
        assertFalse(pLast.isFirst());
        assertTrue(pLast.isLast());
        assertTrue(pLast.hasPrevious());
        assertFalse(pLast.hasNext());
    }
}
