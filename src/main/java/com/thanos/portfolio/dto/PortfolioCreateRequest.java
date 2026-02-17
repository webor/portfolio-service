package com.thanos.portfolio.dto;

import com.thanos.portfolio.model.Category;
import com.thanos.portfolio.model.RMDetails;
import com.thanos.portfolio.model.StockPositionInput;
import com.thanos.portfolio.model.UserDetails;
import com.thanos.portfolio.validator.ValidTargetStateSum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record PortfolioCreateRequest(
        @NotNull @Valid
        UserDetails userDetails,

        @NotNull @Valid
        RMDetails rmDetails,

        @NotNull @NotEmpty
        Map<Category, @Valid List<StockPositionInput>> portfolio,

        @NotNull @Valid
        @ValidTargetStateSum
        Map<Category, BigDecimal> targetState, // category -> % (0-100)

        BigDecimal freeCash,
        BigDecimal driftThresholdAbs,
        Integer cooldownDays,
        String triggerMode
) {}