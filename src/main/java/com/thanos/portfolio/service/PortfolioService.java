package com.thanos.portfolio.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thanos.portfolio.dto.*;
import com.thanos.portfolio.entities.PortfolioRebalanceApplied;
import com.thanos.portfolio.entities.Side;
import com.thanos.portfolio.entities.TriggerMode;
import com.thanos.portfolio.model.*;
import com.thanos.portfolio.entities.Portfolio;
import com.thanos.portfolio.repository.PortfolioRebalanceAppliedRepo;
import com.thanos.portfolio.repository.PortfolioRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PortfolioService {

    private final PortfolioRebalanceAppliedRepo appliedRepo;
    private final PortfolioRepo repo;
    private final ObjectMapper objectMapper =  new ObjectMapper();

    public PortfolioService(PortfolioRebalanceAppliedRepo appliedRepo, PortfolioRepo repo) {
        this.appliedRepo = appliedRepo;
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

    @Transactional
    public void applyRebalance(ApplyRebalanceRequest req) throws JsonProcessingException {
        if (req.rebalanceId() == null || req.rebalanceId().isBlank()) {
            throw new IllegalArgumentException("rebalanceId is required");
        }
        if (req.portfolioId() == null) {
            throw new IllegalArgumentException("portfolioId is required");
        }
        if (req.executedTrades() == null || req.executedTrades().isEmpty()) {
            return;
        }
        if (req.priceFrame() == null || req.priceFrame().isEmpty()) {
            throw new IllegalArgumentException("priceFrame is required to update cash + totals");
        }

        // ✅ idempotency first (fast path)
        if (appliedRepo.existsByPortfolioIdAndRebalanceId(req.portfolioId(), req.rebalanceId())) {
            return;
        }

        // ✅ lock portfolio row to prevent concurrent updates
        Portfolio entity = repo.findByIdForUpdate(req.portfolioId())
                .orElseThrow(() -> new NoSuchElementException("Portfolio not found for id=" + req.portfolioId()));

        // Re-check idempotency inside lock to avoid race
        if (appliedRepo.existsByPortfolioIdAndRebalanceId(req.portfolioId(), req.rebalanceId())) {
            return;
        }

        // Build price lookup
        Map<String, PriceRow> priceBySymbol = req.priceFrame().stream()
                .collect(Collectors.toMap(
                        r -> r.symbol().toUpperCase(),
                        r -> r,
                        (a, b) -> a
                ));

        // Load holdings into mutable map
        Map<Category, List<StockPosition>> holdings = new LinkedHashMap<>();
        Map<Category, List<StockPosition>> current = fromEntityPortfolio(entity.getPortfolio());

        for (Category c : Category.values()) {
            List<StockPosition> list = current.getOrDefault(c, List.of());
            holdings.put(c, new ArrayList<>(list));
        }

        BigDecimal freeCash = entity.getFreeCash() == null ? BigDecimal.ZERO : entity.getFreeCash();

        // Apply trades
        for (ExecutedTrade t : req.executedTrades()) {
            if (t == null) continue;
            String sym = t.ticker() == null ? null : t.ticker().toUpperCase();
            if (sym == null || sym.isBlank()) continue;
            if (t.qty() <= 0) continue;

            PriceRow pr = priceBySymbol.get(sym);
            if (pr == null || pr.price() == null) {
                throw new IllegalArgumentException("Missing price for symbol=" + sym + " in priceFrame");
            }
            BigDecimal px = pr.price();

            Category cat = Category.fromWire(pr.category()); // ✅ use priceFrame category as source of truth
            List<StockPosition> list = holdings.get(cat);

            int idx = indexOfTicker(list, sym);

            if (t.side() == Side.BUY) {
                BigDecimal cost = px.multiply(BigDecimal.valueOf(t.qty()));

                // Optional strict cash check
                // if (freeCash.compareTo(cost) < 0) throw new IllegalStateException("Insufficient cash for BUY " + sym);

                freeCash = freeCash.subtract(cost);

                if (idx == -1) {
                    list.add(new StockPosition(
                            sym,
                            pr.name() != null ? pr.name() : sym,
                            t.qty(),
                            px,                 // avgPrice for new position
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            Instant.now()
                    ));
                } else {
                    StockPosition cur = list.get(idx);
                    int oldQty = cur.quantity() == null ? 0 : cur.quantity();
                    BigDecimal oldAvg = cur.avgPrice() == null ? BigDecimal.ZERO : cur.avgPrice();

                    int newQty = oldQty + t.qty();
                    BigDecimal newAvg = oldAvg.multiply(BigDecimal.valueOf(oldQty))
                            .add(px.multiply(BigDecimal.valueOf(t.qty())))
                            .divide(BigDecimal.valueOf(newQty), 8, RoundingMode.HALF_UP);

                    list.set(idx, new StockPosition(
                            cur.ticker(),
                            cur.name(),
                            newQty,
                            newAvg,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            Instant.now()
                    ));
                }

            } else if (t.side() == Side.SELL) {
                if (idx == -1) {
                    throw new IllegalStateException("Cannot SELL; no holding for " + sym + " in category " + cat);
                }

                StockPosition cur = list.get(idx);
                int oldQty = cur.quantity() == null ? 0 : cur.quantity();
                if (oldQty < t.qty()) {
                    throw new IllegalStateException("Cannot SELL " + t.qty() + " of " + sym + "; only " + oldQty + " available");
                }

                // ✅ credit cash
                BigDecimal proceeds = px.multiply(BigDecimal.valueOf(t.qty()));
                freeCash = freeCash.add(proceeds);

                int newQty = oldQty - t.qty();
                if (newQty == 0) {
                    list.remove(idx);
                } else {
                    list.set(idx, new StockPosition(
                            cur.ticker(),
                            cur.name(),
                            newQty,
                            cur.avgPrice(),     // avgPrice unchanged on sell
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            Instant.now()
                    ));
                }
            }
        }

        // Recompute totals + percentages using latest prices
        Map<Category, List<StockPosition>> updated = recomputeTotalsAndPercentages(holdings, priceBySymbol);

        entity.setPortfolio(toEntityPortfolio(updated));
        entity.setFreeCash(freeCash);

        repo.save(entity);
        String tradesJson = objectMapper.writeValueAsString(req.executedTrades());
        // record idempotency
        appliedRepo.save(new PortfolioRebalanceApplied(entity.getId(), req.rebalanceId(), tradesJson));
    }

    // ---------------- helpers ----------------

    private int indexOfTicker(List<StockPosition> list, String sym) {
        for (int i = 0; i < list.size(); i++) {
            StockPosition sp = list.get(i);
            if (sp.ticker() != null && sp.ticker().equalsIgnoreCase(sym)) return i;
        }
        return -1;
    }

    private Map<Category, List<StockPosition>> recomputeTotalsAndPercentages(
            Map<Category, List<StockPosition>> holdings,
            Map<String, PriceRow> priceBySymbol
    ) {
        BigDecimal totalValue = BigDecimal.ZERO;

        // First pass: compute totalValue
        for (var e : holdings.entrySet()) {
            for (StockPosition sp : e.getValue()) {
                PriceRow pr = priceBySymbol.get(sp.ticker().toUpperCase());
                if (pr == null || pr.price() == null) continue;
                totalValue = totalValue.add(pr.price().multiply(BigDecimal.valueOf(sp.quantity())));
            }
        }

        if (totalValue.compareTo(BigDecimal.ZERO) <= 0) {
            return holdings;
        }

        Map<Category, List<StockPosition>> out = new LinkedHashMap<>();

        for (var e : holdings.entrySet()) {
            Category cat = e.getKey();

            BigDecimal finalTotalValue = totalValue;
            List<StockPosition> updated = e.getValue().stream().map(sp -> {
                PriceRow pr = priceBySymbol.get(sp.ticker().toUpperCase());
                if (pr == null || pr.price() == null) return sp;

                BigDecimal totalAmt = pr.price()
                        .multiply(BigDecimal.valueOf(sp.quantity()))
                        .setScale(2, RoundingMode.HALF_UP);

                BigDecimal pct = totalAmt
                        .divide(finalTotalValue, 8, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(4, RoundingMode.HALF_UP);

                return new StockPosition(
                        sp.ticker(),
                        sp.name(),
                        sp.quantity(),
                        sp.avgPrice(),
                        pct,
                        totalAmt,
                        Instant.now()
                );
            }).toList();

            out.put(cat, updated);
        }

        return out;
    }

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