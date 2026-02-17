package com.thanos.portfolio.repository;

import com.thanos.portfolio.entities.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PortfolioRepo extends JpaRepository<Portfolio, Long> {
    Optional<Portfolio> findByUserId(String userId);
    Optional<Portfolio> findByRmId(String rmId);
}
