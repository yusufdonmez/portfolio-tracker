package com.portfolio.tracker.dto;

import com.portfolio.tracker.model.AssetType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssetSummaryDTO {
    private String symbol;
    private String name;
    private AssetType type;
    private Double quantity;
    private Double averageCost;
    private Double currentPrice;
    private Double marketValue;
    private Double pnl;
    private Double pnlPercentage;
}
