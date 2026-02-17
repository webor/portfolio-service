package com.thanos.portfolio.controller;
import com.thanos.portfolio.dto.PortfolioCreateRequest;
import com.thanos.portfolio.dto.PortfolioResponse;
import com.thanos.portfolio.service.PortfolioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portfolio")
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

    @GetMapping("/{id}")
    public ResponseEntity<PortfolioResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }
}