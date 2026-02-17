package com.thanos.portfolio.service;

import com.thanos.portfolio.dto.*;
import com.thanos.portfolio.entities.TriggerMode;
import com.thanos.portfolio.model.*;
import com.thanos.portfolio.entities.Portfolio;
import com.thanos.portfolio.repository.PortfolioRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PortfolioService {

    private final PortfolioRepo repo;

    public PortfolioService(PortfolioRepo repo) {
        this.repo = repo;
    }

    public PortfolioResponse getById(Long id) {
        Portfolio e = repo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Portfolio not found for id=" + id));

        UserDetails user = new UserDetails(
                e.getUserDetails().userId(),
                e.getUserDetails().firstName(),
                e.getUserDetails().lastName(),
                e.getUserDetails().email(),
                e.getUserDetails().phoneNumber()
        );

        RMDetails rm = new RMDetails(
                e.getRmDetails().rmId(),
                e.getRmDetails().firstName(),
                e.getRmDetails().lastName(),
                e.getRmDetails().email(),
                e.getRmDetails().phoneNumber()
        );

        Map<Category, List<StockPosition>> portfolio = fromEntityPortfolio(e.getPortfolio());

        BigDecimal portfolioValue = sumPortfolioValue(portfolio);

        return new PortfolioResponse(
                e.getId(),
                user,
                rm,
                portfolio,
                e.getTargetState(), // Map<Category, BigDecimal>
                portfolioValue,
                e.getUpdatedOn(),
                e.getCreatedOn(),
                e.getTriggerMode(),
                e.getFreeCash(),
                e.getDriftThresholdAbs(),
                e.getCooldownDays()
        );
    }

    @Transactional
    public PortfolioResponse createOrUpdate(PortfolioCreateRequest req) {
        String userId = req.userDetails().userId();
        BigDecimal freeCash = req.freeCash() != null ? req.freeCash() : BigDecimal.ZERO;
        Integer cooldownDays = req.cooldownDays() != null ? req.cooldownDays() : 3;
        BigDecimal driftThresholdAbs = req.driftThresholdAbs() != null ? req.driftThresholdAbs() : new BigDecimal("0.05");
        TriggerMode triggerMode = req.triggerMode() != null ? TriggerMode.valueOf(req.triggerMode()) : TriggerMode.MANUAL;
        // 1) Compute totals per position and total portfolio value
        Map<Category, List<StockPosition>> computed = compute(req.portfolio());
        BigDecimal portfolioValue = sumPortfolioValue(computed);

        // 2) Apply percentageOfPortfolio
        Map<Category, List<StockPosition>> withPct = applyPercentages(computed, portfolioValue);

        // 3) Persist (upsert)
        Portfolio entity = repo.findByUserId(userId).orElseGet(Portfolio::new);
        entity.setFreeCash(freeCash);
        entity.setCooldownDays(cooldownDays);
        entity.setDriftThresholdAbs(driftThresholdAbs);
        entity.setTriggerMode(triggerMode);
        entity.setUserId(userId);
        entity.setRmId(req.rmDetails().rmId());

        entity.setUserDetails(new UserDetails(
                req.userDetails().userId(),
                req.userDetails().firstName(),
                req.userDetails().lastName(),
                req.userDetails().email(),
                req.userDetails().phoneNumber()
        ));

        entity.setRmDetails(new RMDetails(
                req.rmDetails().rmId(),
                req.rmDetails().firstName(),
                req.rmDetails().lastName(),
                req.rmDetails().email(),
                req.rmDetails().phoneNumber()
        ));

        // Save computed portfolio into entity StockPosition (JSON)
        entity.setPortfolio(toEntityPortfolio(withPct));
        entity.setTargetState(req.targetState()); // Map<Category, BigDecimal>

        Portfolio saved = repo.save(entity);

        return new PortfolioResponse(
                saved.getId(),
                req.userDetails(),
                req.rmDetails(),
                withPct,
                req.targetState(),
                portfolioValue,
                saved.getUpdatedOn(),
                saved.getCreatedOn(),
                saved.getTriggerMode(),
                saved.getFreeCash(),
                saved.getDriftThresholdAbs(),
                saved.getCooldownDays()
        );
    }

    @Transactional(readOnly = true)
    public PortfolioResponse getByUserId(String userId) {
        Portfolio e = repo.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("Portfolio not found for userId=" + userId));

        UserDetails user = new UserDetails(
                e.getUserDetails().userId(),
                e.getUserDetails().firstName(),
                e.getUserDetails().lastName(),
                e.getUserDetails().email(),
                e.getUserDetails().phoneNumber()
        );

        RMDetails rm = new RMDetails(
                e.getRmDetails().rmId(),
                e.getRmDetails().firstName(),
                e.getRmDetails().lastName(),
                e.getRmDetails().email(),
                e.getRmDetails().phoneNumber()
        );

        Map<Category, List<StockPosition>> portfolio = fromEntityPortfolio(e.getPortfolio());

        BigDecimal portfolioValue = sumPortfolioValue(portfolio);

        return new PortfolioResponse(
                e.getId(),
                user,
                rm,
                portfolio,
                e.getTargetState(), // Map<Category, BigDecimal>
                portfolioValue,
                e.getUpdatedOn(),
                e.getCreatedOn(),
                e.getTriggerMode(),
                e.getFreeCash(),
                e.getDriftThresholdAbs(),
                e.getCooldownDays()
        );
    }

    // ---------------- helpers ----------------

    private Map<Category, List<StockPosition>> compute(Map<Category, List<StockPositionInput>> input) {
        Map<Category, List<StockPosition>> out = new LinkedHashMap<>();

        for (var e : input.entrySet()) {
            Category category = e.getKey();
            List<StockPositionInput> positions = e.getValue() == null ? List.of() : e.getValue();

            List<StockPosition> computed = positions.stream()
                    .map(p -> {
                        BigDecimal total = p.avgPrice().multiply(BigDecimal.valueOf(p.quantity()));
                        return new StockPosition(
                                p.ticker(),
                                p.name(),
                                p.quantity(),
                                p.avgPrice(),
                                BigDecimal.ZERO,   // percentageOfPortfolio computed later
                                total,
                                Instant.now()
                        );
                    })
                    .toList();

            out.put(category, computed);
        }

        return out;
    }

    private BigDecimal sumPortfolioValue(Map<Category, List<StockPosition>> portfolio) {
        return portfolio.values().stream()
                .flatMap(List::stream)
                .map(StockPosition::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<Category, List<StockPosition>> applyPercentages(
            Map<Category, List<StockPosition>> portfolio,
            BigDecimal portfolioValue
    ) {
        if (portfolioValue.compareTo(BigDecimal.ZERO) <= 0) return portfolio;

        Map<Category, List<StockPosition>> out = new LinkedHashMap<>();

        for (var e : portfolio.entrySet()) {
            Category category = e.getKey();

            List<StockPosition> updated = e.getValue().stream()
                    .map(p -> {
                        BigDecimal pct = p.totalAmount()
                                .divide(portfolioValue, 8, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(4, RoundingMode.HALF_UP);

                        return new StockPosition(
                                p.ticker(),
                                p.name(),
                                p.quantity(),
                                p.avgPrice(),
                                pct,
                                p.totalAmount(),
                                Instant.now()
                        );
                    })
                    .toList();

            out.put(category, updated);
        }

        return out;
    }

    private Map<Category, List<StockPosition>> toEntityPortfolio(
            Map<Category, List<StockPosition>> apiPortfolio
    ) {
        // At the moment this is identity mapping, but keeping the method for future enrichment (names/quotes)
        Map<Category, List<StockPosition>> out = new LinkedHashMap<>();
        for (var e : apiPortfolio.entrySet()) {
            out.put(e.getKey(), e.getValue());
        }
        return out;
    }

    private Map<Category, List<StockPosition>> fromEntityPortfolio(
            Map<Category, List<StockPosition>> entityPortfolio
    ) {
        if (entityPortfolio == null) return Map.of();

        // Identity mapping; you can also return entityPortfolio directly if you prefer
        Map<Category, List<StockPosition>> out = new LinkedHashMap<>();
        for (var e : entityPortfolio.entrySet()) {
            out.put(e.getKey(), e.getValue());
        }
        return out;
    }
}