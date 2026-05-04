package com.portfolio.tracker.controller;

import com.portfolio.tracker.dto.PortfolioSummaryDTO;
import com.portfolio.tracker.service.CsvImportService;
import com.portfolio.tracker.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final CsvImportService csvImportService;

    @GetMapping("/summary")
    public ResponseEntity<PortfolioSummaryDTO> getSummary() {
        return ResponseEntity.ok(portfolioService.getSummary());
    }

    @PostMapping("/import")
    public ResponseEntity<String> importData(@RequestParam String filePath, @RequestParam String platform) {
        try {
            csvImportService.importCsv(filePath, platform);
            return ResponseEntity.ok("Import successful");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Import failed: " + e.getMessage());
        }
    }
}
