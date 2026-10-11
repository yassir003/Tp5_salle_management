package com.example.dao;

import com.example.model.Reservation;
import com.example.model.Salle;
import com.example.util.Page;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SalleDao {

    private final EntityManager entityManager;

    public SalleDao(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void save(Salle salle) {
        entityManager.persist(salle);
    }

    public Salle findById(Long id) {
        return entityManager.find(Salle.class, id);
    }

    public List<Salle> findAll() {
        return entityManager.createQuery("SELECT s FROM Salle s", Salle.class).getResultList();
    }

    /**
     * Recherche les salles disponibles (non réservées) sur un créneau horaire donné en JPQL.
     * Une réservation chevauche l'intervalle si : (:debut < r.fin AND :fin > r.debut).
     */
    public List<Salle> findSallesDisponibles(LocalDateTime debutRecherche, LocalDateTime finRecherche) {
        String jpql = "SELECT s FROM Salle s WHERE s.id NOT IN (" +
                      "SELECT r.salle.id FROM Reservation r WHERE " +
                      "(:debut < r.fin AND :fin > r.debut)" +
                      ")";

        TypedQuery<Salle> query = entityManager.createQuery(jpql, Salle.class);
        query.setParameter("debut", debutRecherche);
        query.setParameter("fin", finRecherche);

        return query.getResultList();
    }

    /**
     * Variante utilisant la NamedQuery déclarée sur l'entité Salle.
     */
    public List<Salle> findSallesDisponiblesViaNamedQuery(LocalDateTime debutRecherche, LocalDateTime finRecherche) {
        TypedQuery<Salle> query = entityManager.createNamedQuery("Salle.findDisponibles", Salle.class);
        query.setParameter("debut", debutRecherche);
        query.setParameter("fin", finRecherche);
        return query.getResultList();
    }

    /**
     * Recherche multi-critères dynamique avec l'API Criteria sans pagination.
     */
    public List<Salle> searchSalles(String nomRecherche, Integer capaciteMin, LocalDateTime debutRecherche, LocalDateTime finRecherche) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Salle> cq = cb.createQuery(Salle.class);
        Root<Salle> salle = cq.from(Salle.class);

        List<Predicate> predicates = buildPredicates(cb, cq, salle, nomRecherche, capaciteMin, debutRecherche, finRecherche);

        cq.select(salle);
        if (!predicates.isEmpty()) {
            cq.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        TypedQuery<Salle> query = entityManager.createQuery(cq);
        return query.getResultList();
    }

    /**
     * Recherche multi-critères dynamique avec pagination côté serveur.
     *
     * @param nomRecherche   Mot-clé recherché dans le nom (optionnel)
     * @param capaciteMin    Capacité d'accueil minimale (optionnel)
     * @param debutRecherche Début de créneau horaire (optionnel, requiert finRecherche)
     * @param finRecherche   Fin de créneau horaire (optionnel, requiert debutRecherche)
     * @param pageNumber     Numéro de la page demandée (1-based)
     * @param pageSize       Nombre d'éléments par page
     * @return Page contenant la liste des salles et les métadonnées de pagination
     */
    public Page<Salle> searchSalles(String nomRecherche, Integer capaciteMin,
                                   LocalDateTime debutRecherche, LocalDateTime finRecherche,
                                   int pageNumber, int pageSize) {
        if (pageNumber < 1) {
            pageNumber = 1;
        }
        if (pageSize < 1) {
            pageSize = 10;
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // 1. Requête principale avec pagination
        CriteriaQuery<Salle> cq = cb.createQuery(Salle.class);
        Root<Salle> salle = cq.from(Salle.class);
        List<Predicate> predicates = buildPredicates(cb, cq, salle, nomRecherche, capaciteMin, debutRecherche, finRecherche);

        cq.select(salle);
        if (!predicates.isEmpty()) {
            cq.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        TypedQuery<Salle> query = entityManager.createQuery(cq);
        query.setFirstResult((pageNumber - 1) * pageSize);
        query.setMaxResults(pageSize);

        List<Salle> sallesPage = query.getResultList();

        // 2. Requête de comptage avec les mêmes prédicats
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Salle> countRoot = countQuery.from(Salle.class);
        List<Predicate> countPredicates = buildPredicates(cb, countQuery, countRoot, nomRecherche, capaciteMin, debutRecherche, finRecherche);

        countQuery.select(cb.count(countRoot));
        if (!countPredicates.isEmpty()) {
            countQuery.where(cb.and(countPredicates.toArray(new Predicate[0])));
        }

        Long totalElements = entityManager.createQuery(countQuery).getSingleResult();

        return new Page<>(sallesPage, pageNumber, pageSize, totalElements);
    }

    /**
     * Récupère toutes les salles de manière paginée.
     */
    public Page<Salle> findAll(int pageNumber, int pageSize) {
        return searchSalles(null, null, null, null, pageNumber, pageSize);
    }

    /**
     * Construit la liste des prédicats selon les critères passés en paramètres.
     * Cette méthode est réutilisable pour la requête principale et la requête de comptage.
     */
    private List<Predicate> buildPredicates(CriteriaBuilder cb,
                                            CriteriaQuery<?> cq,
                                            Root<Salle> salle,
                                            String nomRecherche,
                                            Integer capaciteMin,
                                            LocalDateTime debutRecherche,
                                            LocalDateTime finRecherche) {
        List<Predicate> predicates = new ArrayList<>();

        // Critère nom (LIKE insensible à la casse)
        if (nomRecherche != null && !nomRecherche.trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(salle.get("nom")), "%" + nomRecherche.trim().toLowerCase() + "%"));
        }

        // Critère capacité minimale (salle.capacite >= capaciteMin)
        if (capaciteMin != null) {
            predicates.add(cb.ge(salle.get("capacite"), capaciteMin));
        }

        // Critère disponibilité (exclure salles réservées sur le créneau)
        if (debutRecherche != null && finRecherche != null) {
            Subquery<Long> subquery = cq.subquery(Long.class);
            Root<Reservation> reservation = subquery.from(Reservation.class);
            subquery.select(reservation.get("salle").get("id"));

            Predicate chevauchement = cb.and(
                cb.lessThan(reservation.get("debut"), finRecherche),
                cb.greaterThan(reservation.get("fin"), debutRecherche)
            );
            subquery.where(chevauchement);

            predicates.add(cb.not(salle.get("id").in(subquery)));
        }

        return predicates;
    }
}
