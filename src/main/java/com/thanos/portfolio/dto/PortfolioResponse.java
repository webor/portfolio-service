package com.thanos.portfolio.dto;

import com.thanos.portfolio.entities.TriggerMode;
import com.thanos.portfolio.model.Category;
import com.thanos.portfolio.model.RMDetails;
import com.thanos.portfolio.model.StockPosition;
import com.thanos.portfolio.model.UserDetails;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record PortfolioResponse(
        Long portfolioId,
        UserDetails userDetails,
        RMDetails rmDetails,
        Map<Category, List<StockPosition>> portfolio,
        Map<Category, BigDecimal> targetState,
        BigDecimal portfolioValue,
        LocalDateTime updatedOn,
        LocalDateTime createdOn,
        TriggerMode triggerMode,
        BigDecimal freeCash,
        BigDecimal driftThresholdAbs,
        Integer cooldownDays
) {}
