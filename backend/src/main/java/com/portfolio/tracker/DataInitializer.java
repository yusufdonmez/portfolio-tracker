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
import java.util.Arrays;

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
        log.info("Checking for initial data in /data/imported/...");
        
        // We no longer deleteAll() here to support persistent history with deduplication
        
        String dataPath = "/data/imported/";
        File dir = new File(dataPath);
        
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("Import directory not found: {}", dataPath);
            return;
        }

        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".csv"));
        if (files == null || files.length == 0) {
            log.info("No CSV files found in {}", dataPath);
            return;
        }

        // Sort files to ensure consistent import order if needed
        Arrays.sort(files);

        for (File file : files) {
            log.info("Processing initial data from: {}", file.getName());
            
            // Try to detect platform from filename: e.g., "may-4-akbank.csv" -> "akbank"
            String platform = "generic";
            String[] parts = file.getName().split("-");
            if (parts.length >= 3) {
                platform = parts[2].replace(".csv", "");
            } else if (file.getName().toLowerCase().contains("ibkr")) {
                platform = "ibkr";
            } else if (file.getName().toLowerCase().contains("midas")) {
                platform = "midas";
            } else if (file.getName().toLowerCase().contains("akbank")) {
                platform = "akbank";
            }

            try {
                csvImportService.importCsv(file.getAbsolutePath(), platform);
            } catch (Exception e) {
                log.error("Failed to import {}: {}", file.getName(), e.getMessage());
            }
        }
    }
}
