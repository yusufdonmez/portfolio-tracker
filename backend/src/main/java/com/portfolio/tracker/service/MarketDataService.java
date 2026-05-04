package com.portfolio.tracker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yahoofinance.YahooFinance;
import yahoofinance.Stock;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class MarketDataService {

    public Map<String, Double> getPrices(Set<String> symbols) {
        Map<String, Double> prices = new HashMap<>();
        try {
            // YahooFinance library handles batch requests
            String[] symbolArray = symbols.toArray(new String[0]);
            Map<String, Stock> stocks = YahooFinance.get(symbolArray);
            
            for (String symbol : symbols) {
                Stock stock = stocks.get(symbol);
                if (stock != null && stock.getQuote() != null) {
                    prices.put(symbol, stock.getQuote().getPrice().doubleValue());
                }
            }
        } catch (IOException e) {
            log.error("Failed to fetch prices from Yahoo Finance: {}", e.getMessage());
        }
        return prices;
    }
}
