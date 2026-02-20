package com.thanos.portfolio.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "portfolio_rebalance_applied",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_portfolio_rebalance_applied",
                columnNames = {"portfolio_id", "rebalance_id"}
        )
)
@Getter @Setter @NoArgsConstructor
public class PortfolioRebalanceApplied {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portfolio_id", nullable = false)
    private Long portfolioId;

    @Column(name = "rebalance_id", nullable = false, length = 200)
    private String rebalanceId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(columnDefinition = "TEXT")
    private String tradesJson;

    public PortfolioRebalanceApplied(Long portfolioId, String rebalanceId, String tradesJson) {
        this.portfolioId = portfolioId;
        this.rebalanceId = rebalanceId;
        this.tradesJson = tradesJson;
    }
}