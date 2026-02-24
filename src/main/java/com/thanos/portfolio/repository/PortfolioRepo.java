package com.thanos.portfolio.repository;

import com.thanos.portfolio.entities.Portfolio;
import feign.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepo extends JpaRepository<Portfolio, Long> {
    Optional<Portfolio> findByUserId(String userId);
    Optional<List<Portfolio>> findByRmId(String rmId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Portfolio p where p.id = :id")
    Optional<Portfolio> findByIdForUpdate(@Param("id") Long id);
}
