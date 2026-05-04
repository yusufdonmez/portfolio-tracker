package com.portfolio.tracker.controller;

import com.portfolio.tracker.service.CsvImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ImportController {

    private final CsvImportService csvImportService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> importFiles(@RequestParam("files") MultipartFile[] files) {
        log.info("Received {} files for import", files.length);
        int totalImported = 0;
        int totalProcessed = 0;
        
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            
            try {
                // Save file temporarily
                Path tempDir = Files.createTempDirectory("portfolio_import");
                File tempFile = new File(tempDir.toFile(), file.getOriginalFilename());
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    fos.write(file.getBytes());
                }

                // Detect platform
                String platform = detectPlatform(file.getOriginalFilename());
                
                // Import
                csvImportService.importCsv(tempFile.getAbsolutePath(), platform);
                
                totalProcessed++;
                // Clean up
                Files.deleteIfExists(tempFile.toPath());
                Files.deleteIfExists(tempDir);
                
            } catch (IOException e) {
                log.error("Failed to process uploaded file {}: {}", file.getOriginalFilename(), e.getMessage());
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Processed " + totalProcessed + " files.");
        return ResponseEntity.ok(response);
    }

    private String detectPlatform(String filename) {
        filename = filename.toLowerCase();
        if (filename.contains("ibkr")) return "ibkr";
        if (filename.contains("midas")) return "midas";
        if (filename.contains("akbank")) return "akbank";
        
        String[] parts = filename.split("-");
        if (parts.length >= 3) {
            return parts[2].replace(".csv", "");
        }
        return "generic";
    }
}
