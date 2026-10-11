package com.example.repository;

import com.example.model.Salle;
import java.time.LocalDateTime;
import java.util.List;

public interface SalleRepository {

    /**
     * Recherche les salles non réservées sur un créneau horaire donné (JPQL).
     */
    List<Salle> findAvailableRooms(LocalDateTime debut, LocalDateTime fin);

    /**
     * Recherche dynamique multi-critères avec pagination (API Criteria).
     */
    List<Salle> searchRooms(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin, int page, int size);

    /**
     * Calcule le nombre total de salles correspondant aux critères de recherche (API Criteria).
     */
    long countRooms(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin);

    /**
     * Enregistre ou met à jour une salle.
     */
    void save(Salle salle);

    /**
     * Recherche une salle par son identifiant.
     */
    Salle findById(Long id);

    /**
     * Récupère la liste de toutes les salles.
     */
    List<Salle> findAll();
}
