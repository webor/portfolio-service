package com.thanos.portfolio.entities;

import com.thanos.portfolio.converter.PortfolioDetailsConverter;
import com.thanos.portfolio.converter.RMDetailsConverter;
import com.thanos.portfolio.converter.TargetStateConverter;
import com.thanos.portfolio.converter.UserDetailsConverter;
import com.thanos.portfolio.model.Category;
import com.thanos.portfolio.model.RMDetails;
import com.thanos.portfolio.model.StockPosition;
import com.thanos.portfolio.model.UserDetails;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "portfolios")
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "free_cash", nullable = false, precision = 18, scale = 2)
    private BigDecimal freeCash = BigDecimal.ZERO;

    @Column(name = "cooldown_days", nullable = false)
    private Integer cooldownDays = 3;

    @Column(name = "drift_threshold_abs", nullable = false, precision = 10, scale = 4)
    private BigDecimal driftThresholdAbs = new BigDecimal("0.05"); // 5%

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_mode", nullable = false, length = 20)
    private TriggerMode triggerMode = TriggerMode.MANUAL;


    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "rm_id")
    private String rmId;

    @Column(name = "created_on", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdOn;

    @Column(name = "updated_on", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedOn;

    @Column(name = "user_details", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = UserDetailsConverter.class)
    private UserDetails userDetails;

    @Column(name = "rm_details", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = RMDetailsConverter.class)
    private RMDetails rmDetails;

    @Column(name = "portfolio", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = PortfolioDetailsConverter.class)
    private Map<Category, List<StockPosition>> portfolio;

    @Column(name = "target_state", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = TargetStateConverter.class)
    private Map<Category, BigDecimal> targetState;

}
