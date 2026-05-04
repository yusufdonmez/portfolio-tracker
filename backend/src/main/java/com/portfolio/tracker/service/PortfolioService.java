package com.portfolio.tracker.service;

import com.portfolio.tracker.dto.AssetSummaryDTO;
import com.portfolio.tracker.dto.PortfolioSummaryDTO;
import com.portfolio.tracker.model.Asset;
import com.portfolio.tracker.model.AssetType;
import com.portfolio.tracker.model.Transaction;
import com.portfolio.tracker.model.TransactionType;
import com.portfolio.tracker.repository.AssetRepository;
import com.portfolio.tracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final AssetRepository assetRepository;
    private final TransactionRepository transactionRepository;
    private final MarketDataService marketDataService;

    public PortfolioSummaryDTO getSummary() {
        List<Asset> allAssets = assetRepository.findAll();
        List<Transaction> allTransactions = transactionRepository.findAll();
        
        // Sort ALL transactions by date for a global cash and position flow
        allTransactions.sort(Comparator.comparing(Transaction::getTransactionDate));

        Map<String, Double> currentQuantities = new HashMap<>();
        Map<String, Double> totalCosts = new HashMap<>();
        double globalCash = 0.0;

        for (Transaction tx : allTransactions) {
            Asset asset = tx.getAsset();
            String symbol = asset.getSymbol();
            double qty = tx.getQuantity() != null ? tx.getQuantity() : 0.0;
            double price = tx.getPrice() != null ? tx.getPrice() : 0.0;
            double fee = tx.getFee() != null ? tx.getFee() : 0.0;
            double multiplier = (asset.getType() == AssetType.OPTION) ? 100.0 : 1.0;

            if (asset.getType() == AssetType.CASH) {
                if (tx.getType() == TransactionType.DEPOSIT || tx.getType() == TransactionType.BUY) {
                    globalCash += qty;
                } else if (tx.getType() == TransactionType.WITHDRAWAL || tx.getType() == TransactionType.SELL) {
                    globalCash -= qty;
                }
            } else {
                // Stock, Crypto, Option logic
                double transactionValue = (qty * price * multiplier);
                
                if (tx.getType() == TransactionType.BUY) {
                    double costIncrease = transactionValue + fee;
                    currentQuantities.put(symbol, currentQuantities.getOrDefault(symbol, 0.0) + qty);
                    totalCosts.put(symbol, totalCosts.getOrDefault(symbol, 0.0) + costIncrease);
                    globalCash -= costIncrease; // Spend cash
                } else if (tx.getType() == TransactionType.SELL) {
                    double sellProceeds = transactionValue - fee;
                    double currentQty = currentQuantities.getOrDefault(symbol, 0.0);
                    if (currentQty > 0) {
                        double avgCost = totalCosts.getOrDefault(symbol, 0.0) / currentQty;
                        currentQuantities.put(symbol, currentQty - qty);
                        totalCosts.put(symbol, totalCosts.get(symbol) - (qty * avgCost));
                    }
                    globalCash += sellProceeds; // Receive cash
                }
            }
        }

        // Fetch live prices for market value calculation
        Set<String> symbolsForPricing = allAssets.stream()
                .filter(a -> a.getType() != AssetType.CASH)
                .map(Asset::getSymbol)
                .collect(Collectors.toSet());
        Map<String, Double> livePrices = marketDataService.getPrices(symbolsForPricing);

        List<AssetSummaryDTO> assetSummaries = new ArrayList<>();
        double totalMarketValue = 0.0;
        double totalPnL = 0.0;

        for (Asset asset : allAssets) {
            if (asset.getType() == AssetType.CASH) continue;
            
            String symbol = asset.getSymbol();
            double qty = currentQuantities.getOrDefault(symbol, 0.0);
            if (qty <= 0.0001) continue;

            double cost = totalCosts.getOrDefault(symbol, 0.0);
            Double price = livePrices.get(symbol);
            if (price == null) price = asset.getCurrentPrice() != null ? asset.getCurrentPrice() : 0.0;
            
            double multiplier = (asset.getType() == AssetType.OPTION) ? 100.0 : 1.0;
            double marketValue = qty * price * multiplier;
            double pnl = marketValue - cost;

            assetSummaries.add(AssetSummaryDTO.builder()
                    .symbol(symbol)
                    .name(asset.getName())
                    .type(asset.getType())
                    .quantity(qty)
                    .averageCost(cost / qty)
                    .currentPrice(price)
                    .marketValue(marketValue)
                    .pnl(pnl)
                    .pnlPercentage(cost > 0 ? (pnl / cost) * 100 : 0)
                    .build());
            
            totalMarketValue += marketValue;
            totalPnL += pnl;
        }

        assetSummaries.sort(Comparator.comparing(AssetSummaryDTO::getMarketValue).reversed());

        Map<AssetType, Double> typeAllocation = new EnumMap<>(AssetType.class);
        Map<String, Double> top10Allocation = new LinkedHashMap<>();
        double othersValue = 0;

        for (int i = 0; i < assetSummaries.size(); i++) {
            AssetSummaryDTO summary = assetSummaries.get(i);
            typeAllocation.merge(summary.getType(), summary.getMarketValue(), Double::sum);
            if (i < 10) {
                top10Allocation.put(summary.getSymbol(), summary.getMarketValue());
            } else {
                othersValue += summary.getMarketValue();
            }
        }
        if (othersValue > 0) top10Allocation.put("Others", othersValue);

        String topRisk = "None";
        if (!assetSummaries.isEmpty()) {
            AssetSummaryDTO mostConcentrated = assetSummaries.get(0);
            double totalWeightRef = (totalMarketValue + globalCash) > 0 ? (totalMarketValue + globalCash) : 1.0;
            double weight = mostConcentrated.getMarketValue() / totalWeightRef;
            topRisk = mostConcentrated.getSymbol() + " (" + String.format("%.1f", weight * 100) + "%)";
        }

        return PortfolioSummaryDTO.builder()
                .totalValue(totalMarketValue + globalCash)
                .totalPnL(totalPnL)
                .totalCash(globalCash)
                .dailyChange(0.0)
                .allocation(typeAllocation)
                .top10Allocation(top10Allocation)
                .assets(assetSummaries)
                .topRisk(topRisk)
                .build();
    }
}
