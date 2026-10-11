package com.example.service;

import com.example.model.Salle;
import com.example.repository.SalleRepository;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Stateless
public class SalleServiceImpl implements SalleService {

    @Inject
    private SalleRepository salleRepository;

    public SalleServiceImpl() {
    }

    public SalleServiceImpl(SalleRepository salleRepository) {
        this.salleRepository = salleRepository;
    }

    public SalleServiceImpl(EntityManager entityManager) {
        this.salleRepository = new com.example.repository.SalleRepositoryImpl(entityManager);
    }

    public void setSalleRepository(SalleRepository salleRepository) {
        this.salleRepository = salleRepository;
    }

    public void setEntityManager(EntityManager entityManager) {
        if (this.salleRepository == null) {
            this.salleRepository = new com.example.repository.SalleRepositoryImpl(entityManager);
        } else if (this.salleRepository instanceof com.example.repository.SalleRepositoryImpl) {
            ((com.example.repository.SalleRepositoryImpl) this.salleRepository).setEntityManager(entityManager);
        }
    }

    @Override
    public List<Salle> getAvailableRooms(LocalDateTime debut, LocalDateTime fin) {
        // Règle métier : validation de la cohérence de l'intervalle temporel
        validerIntervalle(debut, fin);
        if (debut == null || fin == null) {
            return Collections.emptyList();
        }
        return salleRepository.findAvailableRooms(debut, fin);
    }

    @Override
    public List<Salle> searchRooms(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin, int page, int size) {
        // Règles métier : validation des arguments
        if (debut != null && fin != null) {
            validerIntervalle(debut, fin);
        }
        if (capaciteMin != null && capaciteMin < 0) {
            throw new IllegalArgumentException("La capacité minimale ne peut pas être négative.");
        }
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 10;
        }

        return salleRepository.searchRooms(nom, capaciteMin, debut, fin, page, size);
    }

    @Override
    public long countRooms(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin) {
        if (debut != null && fin != null) {
            validerIntervalle(debut, fin);
        }
        if (capaciteMin != null && capaciteMin < 0) {
            throw new IllegalArgumentException("La capacité minimale ne peut pas être négative.");
        }

        return salleRepository.countRooms(nom, capaciteMin, debut, fin);
    }

    @Override
    public com.example.util.PaginationResult<Salle> searchRoomsPaginated(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin, int page, int size) {
        List<Salle> items = searchRooms(nom, capaciteMin, debut, fin, page, size);
        long totalItems = countRooms(nom, capaciteMin, debut, fin);
        return new com.example.util.PaginationResult<>(items, page, size, totalItems);
    }

    private void validerIntervalle(LocalDateTime debut, LocalDateTime fin) {
        if (debut != null && fin != null && !debut.isBefore(fin)) {
            throw new IllegalArgumentException("La date de début (" + debut + ") doit être antérieure à la date de fin (" + fin + ").");
        }
    }
}
