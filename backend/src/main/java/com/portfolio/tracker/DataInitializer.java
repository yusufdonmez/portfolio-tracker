package com.portfolio.tracker;

import com.portfolio.tracker.service.CsvImportService;
import com.portfolio.tracker.repository.AssetRepository;
import com.portfolio.tracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CsvImportService csvImportService;
    private final AssetRepository assetRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Clearing existing data for a fresh import...");
        transactionRepository.deleteAll();
        assetRepository.deleteAll();

        String[] files = {"may-4-akbank.csv", "may-4-ibkr.csv", "may-4-midas.csv"};
        String dataPath = "/data/"; // Mounted in Docker

        for (String fileName : files) {
            File file = new File(dataPath + fileName);
            if (file.exists()) {
                log.info("Importing initial data from: {}", fileName);
                String[] parts = fileName.split("-");
                if (parts.length < 3) continue;
                String platform = parts[2].replace(".csv", "");
                try {
                    csvImportService.importCsv(file.getAbsolutePath(), platform);
                } catch (Exception e) {
                    log.error("Failed to import {}: {}", fileName, e.getMessage());
                }
            } else {
                log.warn("Initial data file not found: {}", file.getAbsolutePath());
            }
        }
    }
}
