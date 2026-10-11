package com.example.repository;

import com.example.model.Reservation;
import com.example.model.Salle;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Stateless
public class SalleRepositoryImpl implements SalleRepository {

    @PersistenceContext(unitName = "SallePU")
    private EntityManager entityManager;

    public SalleRepositoryImpl() {
    }

    public SalleRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<Salle> findAvailableRooms(LocalDateTime debut, LocalDateTime fin) {
        String jpql = "SELECT s FROM Salle s WHERE s.id NOT IN (" +
                      "SELECT r.salle.id FROM Reservation r WHERE (:debut < r.fin AND :fin > r.debut))";
        return entityManager.createQuery(jpql, Salle.class)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getResultList();
    }

    @Override
    public List<Salle> searchRooms(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Salle> cq = cb.createQuery(Salle.class);
        Root<Salle> salle = cq.from(Salle.class);

        List<Predicate> predicates = buildPredicates(cb, cq, salle, nom, capaciteMin, debut, fin);

        cq.select(salle);
        if (!predicates.isEmpty()) {
            cq.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        TypedQuery<Salle> query = entityManager.createQuery(cq);
        query.setFirstResult((page - 1) * size);
        query.setMaxResults(size);

        return query.getResultList();
    }

    @Override
    public long countRooms(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Salle> countRoot = countQuery.from(Salle.class);

        List<Predicate> countPredicates = buildPredicates(cb, countQuery, countRoot, nom, capaciteMin, debut, fin);

        countQuery.select(cb.count(countRoot));
        if (!countPredicates.isEmpty()) {
            countQuery.where(cb.and(countPredicates.toArray(new Predicate[0])));
        }

        return entityManager.createQuery(countQuery).getSingleResult();
    }

    @Override
    public void save(Salle salle) {
        if (salle.getId() == null) {
            entityManager.persist(salle);
        } else {
            entityManager.merge(salle);
        }
    }

    @Override
    public Salle findById(Long id) {
        return entityManager.find(Salle.class, id);
    }

    @Override
    public List<Salle> findAll() {
        return entityManager.createQuery("SELECT s FROM Salle s", Salle.class).getResultList();
    }

    /**
     * Construit les prédicats de filtrage réutilisables pour les requêtes SELECT et COUNT.
     */
    private List<Predicate> buildPredicates(CriteriaBuilder cb,
                                            CriteriaQuery<?> cq,
                                            Root<Salle> salle,
                                            String nom,
                                            Integer capaciteMin,
                                            LocalDateTime debut,
                                            LocalDateTime fin) {
        List<Predicate> predicates = new ArrayList<>();

        // Critère nom (recherche LIKE insensible à la casse)
        if (nom != null && !nom.trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(salle.get("nom")), "%" + nom.trim().toLowerCase() + "%"));
        }

        // Critère capacité minimale
        if (capaciteMin != null) {
            predicates.add(cb.ge(salle.get("capacite"), capaciteMin));
        }

        // Critère créneau disponible (exclusion par sous-requête)
        if (debut != null && fin != null) {
            Subquery<Long> subquery = cq.subquery(Long.class);
            Root<Reservation> reservation = subquery.from(Reservation.class);
            subquery.select(reservation.get("salle").get("id"));

            Predicate chevauchement = cb.and(
                cb.lessThan(reservation.get("debut"), fin),
                cb.greaterThan(reservation.get("fin"), debut)
            );
            subquery.where(chevauchement);

            predicates.add(cb.not(salle.get("id").in(subquery)));
        }

        return predicates;
    }
}
