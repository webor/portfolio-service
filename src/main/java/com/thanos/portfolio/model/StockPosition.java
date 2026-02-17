package com.thanos.portfolio.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record StockPosition(
        String ticker,
        String name,
        Integer quantity,
        BigDecimal avgPrice,
        BigDecimal percentageOfPortfolio,
        BigDecimal totalAmount,
        @JsonIgnore
        Instant positionDate
) {}