package com.thanos.portfolio.repository;

import com.thanos.portfolio.entities.PortfolioRebalanceApplied;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioRebalanceAppliedRepo extends JpaRepository<PortfolioRebalanceApplied, Long> {
    boolean existsByPortfolioIdAndRebalanceId(Long portfolioId, String rebalanceId);
}
