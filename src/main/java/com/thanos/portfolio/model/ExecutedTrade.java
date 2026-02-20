package com.thanos.portfolio.model;

import com.thanos.portfolio.entities.Side;

public record ExecutedTrade(String ticker, Side side, int qty, String reason) {}
