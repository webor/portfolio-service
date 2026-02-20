package com.thanos.portfolio.dto;

import com.thanos.portfolio.model.ExecutedTrade;
import com.thanos.portfolio.model.PriceRow;

public record ApplyRebalanceRequest(
        String rebalanceId,
        Long portfolioId,
        java.util.List<ExecutedTrade> executedTrades,
        java.util.List<PriceRow> priceFrame
) {}

