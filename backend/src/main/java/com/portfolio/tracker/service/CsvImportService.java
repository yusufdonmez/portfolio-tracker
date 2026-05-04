package com.portfolio.tracker.service;

import com.portfolio.tracker.model.*;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class CsvImportService {

    private final AssetRepository assetRepository;
    private final TransactionRepository transactionRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Pattern OPTION_PATTERN = Pattern.compile("^([A-Z]+)([0-9]{6})([CP])([0-9]+)$");

    @Transactional
    public void importCsv(String filePath, String platform) throws Exception {
        try (Reader reader = new FileReader(filePath);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

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
                if (quantity == null) continue; // Skip rows without quantity
                
                Double price = parseDouble(record.get("Purchase Price"));
                if (price == null && txType != TransactionType.DEPOSIT && txType != TransactionType.WITHDRAWAL) continue;

                Double commission = parseDouble(record.get("Commission"));
                String tradeDateStr = record.get("Trade Date");
                
                LocalDateTime tradeDate = tradeDateStr != null && !tradeDateStr.isEmpty() 
                    ? LocalDate.parse(tradeDateStr, DATE_FORMATTER).atStartOfDay() 
                    : LocalDateTime.now();

                Transaction transaction = Transaction.builder()
                        .asset(asset)
                        .type(txType)
                        .quantity(quantity)
                        .price(price)
                        .fee(commission != null ? commission : 0.0)
                        .transactionDate(tradeDate)
                        .platform(platform)
                        .notes(record.get("Comment"))
                        .build();

                transactionRepository.save(transaction);
            }
        }
    }

    private Asset getOrCreateAsset(String symbol) {
        return assetRepository.findBySymbol(symbol).orElseGet(() -> {
            AssetType type = AssetType.STOCK;
            OptionType optionType = OptionType.NONE;
            Double strike = null;
            LocalDate expiry = null;

            if (symbol.equals("$$CASH_TX")) {
                type = AssetType.CASH;
            } else if (symbol.equals("GLD") || symbol.equals("XAU")) {
                type = AssetType.COMMODITY;
            } else {
                Matcher matcher = OPTION_PATTERN.matcher(symbol);
                if (matcher.matches()) {
                    type = AssetType.OPTION;
                    optionType = matcher.group(3).equals("C") ? OptionType.CALL : OptionType.PUT;
                    // Simplified parsing for expiry and strike
                    String expiryStr = matcher.group(2);
                    expiry = LocalDate.parse("20" + expiryStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
                    strike = Double.parseDouble(matcher.group(4)) / 1000.0; // Assuming strike format
                }
            }

            Asset asset = Asset.builder()
                    .symbol(symbol)
                    .type(type)
                    .optionType(optionType)
                    .strikePrice(strike)
                    .expirationDate(expiry)
                    .currency("USD") // Default
                    .build();
            return assetRepository.save(asset);
        });
    }

    private TransactionType parseTransactionType(String type) {
        if (type == null) return TransactionType.BUY;
        return switch (type.toUpperCase()) {
            case "SELL" -> TransactionType.SELL;
            case "DEPOSIT" -> TransactionType.DEPOSIT;
            case "WITHDRAWAL" -> TransactionType.WITHDRAWAL;
            case "DIVIDEND" -> TransactionType.DIVIDEND;
            default -> TransactionType.BUY;
        };
    }

    private Double parseDouble(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
