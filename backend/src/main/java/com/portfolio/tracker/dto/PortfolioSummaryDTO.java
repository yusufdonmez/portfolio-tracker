package com.portfolio.tracker.dto;

import com.portfolio.tracker.model.AssetType;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class PortfolioSummaryDTO {
    private Double totalValue;
    private Double totalPnL;
    private Double totalCash;
    private Double dailyChange;
    private Map<AssetType, Double> allocation;
    private Map<String, Double> top10Allocation;
    private List<AssetSummaryDTO> assets;
    private String topRisk;
}
