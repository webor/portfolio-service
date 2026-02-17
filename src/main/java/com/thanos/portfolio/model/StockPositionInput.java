package com.thanos.portfolio.model;

import java.math.BigDecimal;

public record StockPositionInput(
        String ticker,
        String name,
        Integer quantity,     // integer shares only
        BigDecimal avgPrice   // cost basis
) {}

