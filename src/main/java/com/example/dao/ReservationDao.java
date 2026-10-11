package com.example.dao;

import com.example.model.Reservation;

import javax.persistence.EntityManager;
import java.util.List;

public class ReservationDao {

    private final EntityManager entityManager;

    public ReservationDao(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void save(Reservation reservation) {
        entityManager.persist(reservation);
    }

    public Reservation findById(Long id) {
        return entityManager.find(Reservation.class, id);
    }

    public List<Reservation> findAll() {
        return entityManager.createQuery("SELECT r FROM Reservation r", Reservation.class).getResultList();
    }
}
