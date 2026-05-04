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
        
        // Fetch live prices for all assets except cash
        Set<String> symbols = allAssets.stream()
                .filter(a -> a.getType() != AssetType.CASH)
                .map(Asset::getSymbol)
                .collect(Collectors.toSet());
        
        Map<String, Double> livePrices = marketDataService.getPrices(symbols);
        
        List<AssetSummaryDTO> assetSummaries = new ArrayList<>();
        Double totalValue = 0.0;
        Double totalPnL = 0.0;
        Double totalCash = 0.0;

        for (Asset asset : allAssets) {
            List<Transaction> txs = transactionRepository.findByAssetId(asset.getId());
            if (txs.isEmpty()) continue;

            txs.sort(Comparator.comparing(Transaction::getTransactionDate));

            if (asset.getType() != AssetType.CASH) {
                Double livePrice = livePrices.get(asset.getSymbol());
                if (livePrice != null) {
                    asset.setCurrentPrice(livePrice);
                }
            }

            AssetSummaryDTO summary = calculateAssetSummary(asset, txs);
            
            if (asset.getType() == AssetType.CASH) {
                totalCash += summary.getQuantity(); // Just the quantity of $$CASH_TX
            } else if (summary.getQuantity() > 0.0001) {
                assetSummaries.add(summary);
                totalValue += summary.getMarketValue();
                totalPnL += summary.getPnl();
            }
        }

        assetSummaries.sort(Comparator.comparing(AssetSummaryDTO::getMarketValue).reversed());

        Map<AssetType, Double> typeAllocation = new EnumMap<>(AssetType.class);
        Map<String, Double> top10Allocation = new LinkedHashMap<>();
        double othersValue = 0;

        for (int i = 0; i < assetSummaries.size(); i++) {
            AssetSummaryDTO assetSummary = assetSummaries.get(i);
            typeAllocation.merge(assetSummary.getType(), assetSummary.getMarketValue(), Double::sum);
            
            if (i < 10) {
                top10Allocation.put(assetSummary.getSymbol(), assetSummary.getMarketValue());
            } else {
                othersValue += assetSummary.getMarketValue();
            }
        }
        if (othersValue > 0) {
            top10Allocation.put("Others", othersValue);
        }

        String topRisk = "None";
        if (!assetSummaries.isEmpty()) {
            AssetSummaryDTO mostConcentrated = assetSummaries.get(0);
            double totalValForRisk = totalValue > 0 ? totalValue : 1.0;
            double weight = mostConcentrated.getMarketValue() / totalValForRisk;
            topRisk = mostConcentrated.getSymbol() + " (" + String.format("%.1f", weight * 100) + "%)";
        }

        return PortfolioSummaryDTO.builder()
                .totalValue(totalValue + totalCash)
                .totalPnL(totalPnL)
                .totalCash(totalCash)
                .dailyChange(0.0)
                .allocation(typeAllocation)
                .top10Allocation(top10Allocation)
                .assets(assetSummaries)
                .topRisk(topRisk)
                .build();
    }

    private AssetSummaryDTO calculateAssetSummary(Asset asset, List<Transaction> txs) {
        double totalQuantity = 0;
        double totalCost = 0;
        double multiplier = (asset.getType() == AssetType.OPTION) ? 100.0 : 1.0;
        
        for (Transaction tx : txs) {
            Double q = tx.getQuantity();
            Double p = tx.getPrice();
            if (q == null) continue;
            
            if (tx.getType() == TransactionType.BUY || tx.getType() == TransactionType.DEPOSIT) {
                totalQuantity += q;
                if (asset.getType() != AssetType.CASH) {
                    totalCost += (q * (p != null ? p : 0.0) * multiplier) + (tx.getFee() != null ? tx.getFee() : 0.0);
                }
            } else if (tx.getType() == TransactionType.SELL || tx.getType() == TransactionType.WITHDRAWAL) {
                if (totalQuantity > 0) {
                    double avgCost = totalCost / totalQuantity;
                    totalQuantity -= q;
                    totalCost -= q * avgCost;
                }
            }
        }

        Double currentPrice = asset.getCurrentPrice();
        if (currentPrice == null) {
            currentPrice = (asset.getType() == AssetType.CASH) ? 1.0 : 0.0;
        }
        
        double marketValue = totalQuantity * currentPrice * multiplier;
        double pnl = marketValue - totalCost;
        double pnlPercentage = totalCost > 0 ? (pnl / totalCost) * 100 : 0;

        return AssetSummaryDTO.builder()
                .symbol(asset.getSymbol())
                .name(asset.getName())
                .type(asset.getType())
                .quantity(totalQuantity)
                .averageCost(totalQuantity > 0 ? totalCost / totalQuantity : 0)
                .currentPrice(currentPrice)
                .marketValue(marketValue)
                .pnl(pnl)
                .pnlPercentage(pnlPercentage)
                .build();
    }
}
