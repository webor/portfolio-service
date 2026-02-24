package com.thanos.portfolio.controller;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.thanos.portfolio.dto.ApplyRebalanceRequest;
import com.thanos.portfolio.dto.PortfolioCreateRequest;
import com.thanos.portfolio.dto.PortfolioResponse;
import com.thanos.portfolio.service.PortfolioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioController {

    private final PortfolioService service;

    public PortfolioController(PortfolioService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PortfolioResponse> createOrUpdate(@RequestBody PortfolioCreateRequest req) {
        return ResponseEntity.ok(service.createOrUpdate(req));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<PortfolioResponse> getByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(service.getByUserId(userId));
    }

    @GetMapping("/rm/{rmId}")
    public ResponseEntity<List<PortfolioResponse>> getByRmId(@PathVariable String rmId) {
        return ResponseEntity.ok(service.getByRmId(rmId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PortfolioResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping("/apply")
    public ResponseEntity<Void> apply(@RequestBody ApplyRebalanceRequest req) {
        try {
            service.applyRebalance(req);
            return ResponseEntity.ok().build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}