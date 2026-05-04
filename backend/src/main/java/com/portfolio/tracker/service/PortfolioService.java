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

    public PortfolioSummaryDTO getSummary() {
        List<Asset> allAssets = assetRepository.findAll();
        List<AssetSummaryDTO> assetSummaries = new ArrayList<>();
        Map<AssetType, Double> allocation = new EnumMap<>(AssetType.class);
        Double totalValue = 0.0;
        Double totalPnL = 0.0;

        for (Asset asset : allAssets) {
            List<Transaction> txs = transactionRepository.findByAssetId(asset.getId());
            if (txs.isEmpty()) continue;

            AssetSummaryDTO summary = calculateAssetSummary(asset, txs);
            if (summary.getQuantity() > 0 || summary.getType() == AssetType.CASH) {
                assetSummaries.add(summary);
                totalValue += summary.getMarketValue();
                totalPnL += summary.getPnl();
                
                allocation.merge(asset.getType(), summary.getMarketValue(), Double::sum);
            }
        }

        String topRisk = "None";
        double maxWeight = 0;
        for (AssetSummaryDTO asset : assetSummaries) {
            double weight = asset.getMarketValue() / totalValue;
            if (weight > maxWeight) {
                maxWeight = weight;
                topRisk = asset.getSymbol() + " (" + String.format("%.1f", weight * 100) + "%)";
            }
        }

        return PortfolioSummaryDTO.builder()
                .totalValue(totalValue)
                .totalPnL(totalPnL)
                .dailyChange(0.0) // Placeholder
                .allocation(allocation)
                .assets(assetSummaries)
                .topRisk(topRisk)
                .build();
    }

    private AssetSummaryDTO calculateAssetSummary(Asset asset, List<Transaction> txs) {
        double totalQuantity = 0;
        double totalCost = 0;
        
        // Simple FIFO or Average Cost - using Average Cost here
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
