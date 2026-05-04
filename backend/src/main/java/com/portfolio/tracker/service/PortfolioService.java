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

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final AssetRepository assetRepository;
    private final TransactionRepository transactionRepository;

    public PortfolioSummaryDTO getSummary() {
        List<Asset> allAssets = assetRepository.findAll();
        List<AssetSummaryDTO> assetSummaries = new ArrayList<>();
        Double totalValue = 0.0;
        Double totalPnL = 0.0;

        for (Asset asset : allAssets) {
            List<Transaction> txs = transactionRepository.findByAssetId(asset.getId());
            if (txs.isEmpty()) continue;

            // Sort transactions by date to ensure correct balance calculation
            txs.sort(Comparator.comparing(Transaction::getTransactionDate));

            AssetSummaryDTO summary = calculateAssetSummary(asset, txs);
            if (summary.getQuantity() > 0 || summary.getType() == AssetType.CASH) {
                assetSummaries.add(summary);
                totalValue += summary.getMarketValue();
                totalPnL += summary.getPnl();
            }
        }

        // Sort assets by market value descending (Risk/Exposure)
        assetSummaries.sort(Comparator.comparing(AssetSummaryDTO::getMarketValue).reversed());

        // Calculate Top 5 vs Others for Allocation
        Map<AssetType, Double> typeAllocation = new EnumMap<>(AssetType.class);
        Map<String, Double> top5Allocation = new LinkedHashMap<>();
        double othersValue = 0;

        for (int i = 0; i < assetSummaries.size(); i++) {
            AssetSummaryDTO asset = assetSummaries.get(i);
            typeAllocation.merge(asset.getType(), asset.getMarketValue(), Double::sum);
            
            if (i < 5) {
                top5Allocation.put(asset.getSymbol(), asset.getMarketValue());
            } else {
                othersValue += asset.getMarketValue();
            }
        }
        if (othersValue > 0) {
            top5Allocation.put("Others", othersValue);
        }

        String topRisk = "None";
        if (!assetSummaries.isEmpty()) {
            AssetSummaryDTO mostConcentrated = assetSummaries.get(0);
            double totalValForRisk = totalValue > 0 ? totalValue : 1.0;
            double weight = mostConcentrated.getMarketValue() / totalValForRisk;
            topRisk = mostConcentrated.getSymbol() + " (" + String.format("%.1f", weight * 100) + "%)";
        }

        return PortfolioSummaryDTO.builder()
                .totalValue(totalValue)
                .totalPnL(totalPnL)
                .dailyChange(0.0)
                .allocation(typeAllocation)
                .top5Allocation(top5Allocation)
                .assets(assetSummaries)
                .topRisk(topRisk)
                .build();
    }

    private AssetSummaryDTO calculateAssetSummary(Asset asset, List<Transaction> txs) {
        double totalQuantity = 0;
        double totalCost = 0;
        
        for (Transaction tx : txs) {
            Double q = tx.getQuantity();
            Double p = tx.getPrice();
            if (q == null) continue;
            
            if (tx.getType() == TransactionType.BUY || tx.getType() == TransactionType.DEPOSIT) {
                totalQuantity += q;
                if (asset.getType() != AssetType.CASH) {
                    totalCost += q * (p != null ? p : 0.0) + (tx.getFee() != null ? tx.getFee() : 0.0);
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
        
        double marketValue = totalQuantity * currentPrice;
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
