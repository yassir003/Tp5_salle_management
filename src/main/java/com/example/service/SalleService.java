package com.example.service;

import com.example.model.Salle;
import java.time.LocalDateTime;
import java.util.List;

public interface SalleService {

    /**
     * Récupère les salles disponibles pour un créneau horaire donné.
     *
     * @param debut Début du créneau
     * @param fin   Fin du créneau
     * @return Liste des salles disponibles
     */
    List<Salle> getAvailableRooms(LocalDateTime debut, LocalDateTime fin);

    /**
     * Recherche les salles selon plusieurs critères avec pagination.
     *
     * @param nom         Nom ou motif recherché
     * @param capaciteMin Capacité minimale requise
     * @param debut       Début du créneau
     * @param fin         Fin du créneau
     * @param page        Numéro de la page (1-based)
     * @param size        Nombre d'éléments par page
     * @return Liste des salles de la page demandée
     */
    List<Salle> searchRooms(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin, int page, int size);

    /**
     * Compte le nombre total de salles correspondant aux critères.
     *
     * @param nom         Nom ou motif recherché
     * @param capaciteMin Capacité minimale requise
     * @param debut       Début du créneau
     * @param fin         Fin du créneau
     * @return Nombre total d'éléments trouvés
     */
    long countRooms(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin);

    /**
     * Recherche les salles selon plusieurs critères et retourne un conteneur PaginationResult complet.
     *
     * @param nom         Nom ou motif recherché
     * @param capaciteMin Capacité minimale requise
     * @param debut       Début du créneau
     * @param fin         Fin du créneau
     * @param page        Numéro de la page (1-based)
     * @param size        Nombre d'éléments par page
     * @return Résultat paginé complet avec métadonnées
     */
    com.example.util.PaginationResult<Salle> searchRoomsPaginated(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin, int page, int size);
}
