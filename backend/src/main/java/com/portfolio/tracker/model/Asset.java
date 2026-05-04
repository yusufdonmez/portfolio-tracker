package com.portfolio.tracker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "assets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String symbol;

    private String name;

    @Enumerated(EnumType.STRING)
    private AssetType type;

    private String sector;

    // Option specific fields
    private Double strikePrice;
    private LocalDate expirationDate;
    
    @Enumerated(EnumType.STRING)
    private OptionType optionType;

    private String currency; // e.g., USD, TRY

    private Double currentPrice;
}
