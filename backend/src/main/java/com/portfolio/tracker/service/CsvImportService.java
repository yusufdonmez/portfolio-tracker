package com.portfolio.tracker.service;

import com.portfolio.tracker.model.Asset;
import com.portfolio.tracker.model.AssetType;
import com.portfolio.tracker.model.Transaction;
import com.portfolio.tracker.model.TransactionType;
import com.portfolio.tracker.repository.AssetRepository;
import com.portfolio.tracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileReader;
import java.io.Reader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CsvImportService {

    private final AssetRepository assetRepository;
    private final TransactionRepository transactionRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Transactional
    public void importCsv(String filePath, String platform) {
        log.info("Starting CSV import for platform: {} from file: {}", platform, filePath);
        try (Reader reader = new FileReader(filePath);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            int importedCount = 0;
            int skippedCount = 0;

            for (CSVRecord record : csvParser) {
                String symbol = record.get("Symbol");
                if (symbol == null || symbol.isEmpty()) continue;

                String typeStr = record.get("Transaction Type");
                TransactionType txType = parseTransactionType(typeStr);

                Asset asset = getOrCreateAsset(symbol);
                
                // Update current price if available
                Double currentPrice = parseDouble(record.get("Current Price"));
                if (currentPrice != null) {
                    asset.setCurrentPrice(currentPrice);
                    assetRepository.save(asset);
                }

                Double quantity = parseDouble(record.get("Quantity"));
                if (quantity == null) continue;
                
                Double price = parseDouble(record.get("Purchase Price"));
                if (price == null && txType != TransactionType.DEPOSIT && txType != TransactionType.WITHDRAWAL) continue;

                Double commission = parseDouble(record.get("Commission"));
                String tradeDateStr = record.get("Trade Date");
                
                LocalDateTime tradeDate = tradeDateStr != null && !tradeDateStr.isEmpty() 
                    ? LocalDate.parse(tradeDateStr, DATE_FORMATTER).atStartOfDay() 
                    : LocalDateTime.now();

                // Generate Unique Identifier for deduplication
                String uniqueId = generateUniqueId(tradeDateStr, symbol, quantity, price, txType, platform);
                
                if (transactionRepository.findByUniqueIdentifier(uniqueId).isPresent()) {
                    skippedCount++;
                    continue;
                }

                Transaction transaction = Transaction.builder()
                        .asset(asset)
                        .type(txType)
                        .quantity(quantity)
                        .price(price)
                        .fee(commission != null ? commission : 0.0)
                        .transactionDate(tradeDate)
                        .platform(platform)
                        .uniqueIdentifier(uniqueId)
                        .build();

                transactionRepository.save(transaction);
                importedCount++;
            }
            log.info("Import finished for {}. Imported: {}, Skipped (duplicates): {}", platform, importedCount, skippedCount);
        } catch (Exception e) {
            log.error("Error importing CSV: {}", e.getMessage());
            throw new RuntimeException("CSV import failed", e);
        }
    }

    private String generateUniqueId(String date, String symbol, Double qty, Double price, TransactionType type, String platform) {
        return String.format("%s_%s_%s_%s_%s_%s", 
            date != null ? date : "00000000", 
            symbol, 
            qty != null ? qty : 0.0, 
            price != null ? price : 0.0, 
            type, 
            platform.toLowerCase());
    }

    private Asset getOrCreateAsset(String symbol) {
        Optional<Asset> existing = assetRepository.findBySymbol(symbol);
        if (existing.isPresent()) return existing.get();

        AssetType type = AssetType.STOCK;
        if (symbol.equals("$$CASH_TX")) {
            type = AssetType.CASH;
        } else if (symbol.equals("BTC") || symbol.equals("ETH") || symbol.equals("SOL")) {
            type = AssetType.CRYPTO;
        } else if (symbol.equals("GLD") || symbol.equals("SLV")) {
            type = AssetType.COMMODITY;
        } else if (symbol.matches(".*[0-9]{6}[CP][0-9]{8}.*")) {
            type = AssetType.OPTION;
        }

        Asset asset = Asset.builder()
                .symbol(symbol)
                .name(symbol) // Default to symbol
                .type(type)
                .currency("USD") // Default
                .build();

        return assetRepository.save(asset);
    }

    private TransactionType parseTransactionType(String typeStr) {
        if (typeStr == null) return TransactionType.BUY;
        typeStr = typeStr.toUpperCase();
        if (typeStr.contains("SELL")) return TransactionType.SELL;
        if (typeStr.contains("BUY")) return TransactionType.BUY;
        if (typeStr.contains("DEPOSIT")) return TransactionType.DEPOSIT;
        if (typeStr.contains("WITHDRAW")) return TransactionType.WITHDRAWAL;
        return TransactionType.BUY;
    }

    private Double parseDouble(String value) {
        if (value == null || value.isEmpty() || value.equals("-")) return null;
        try {
            return Double.parseDouble(value.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
