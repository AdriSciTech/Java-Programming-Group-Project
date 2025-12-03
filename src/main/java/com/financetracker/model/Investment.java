package com.financetracker.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Investment Model Class
 */
public class Investment {
    private UUID investmentId;
    private UUID userId;
    private UUID accountId;
    private String investmentName;
    private String investmentType;
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal purchasePrice;
    private BigDecimal currentPrice;
    private LocalDate purchaseDate;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public Investment() {
        this.investmentId = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.quantity = BigDecimal.ZERO;
        this.purchasePrice = BigDecimal.ZERO;
        this.currentPrice = BigDecimal.ZERO;
    }
    
    public Investment(String investmentName, String investmentType, BigDecimal quantity, BigDecimal purchasePrice) {
        this();
        this.investmentName = investmentName;
        this.investmentType = investmentType;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
        this.currentPrice = purchasePrice;
    }
    
    // Getters and Setters
    public UUID getInvestmentId() {
        return investmentId;
    }
    
    public void setInvestmentId(UUID investmentId) {
        this.investmentId = investmentId;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public UUID getAccountId() {
        return accountId;
    }
    
    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }
    
    public String getInvestmentName() {
        return investmentName;
    }
    
    public void setInvestmentName(String investmentName) {
        this.investmentName = investmentName;
    }
    
    public String getInvestmentType() {
        return investmentType;
    }
    
    public void setInvestmentType(String investmentType) {
        this.investmentType = investmentType;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    public BigDecimal getQuantity() {
        return quantity;
    }
    
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
    
    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }
    
    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }
    
    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }
    
    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }
    
    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }
    
    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * Calculate total value (current price * quantity)
     */
    public BigDecimal getTotalValue() {
        if (currentPrice != null && quantity != null) {
            return currentPrice.multiply(quantity);
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Calculate ROI: (current_price - purchase_price) * quantity
     */
    public BigDecimal getROI() {
        if (currentPrice != null && purchasePrice != null && quantity != null) {
            BigDecimal priceDiff = currentPrice.subtract(purchasePrice);
            return priceDiff.multiply(quantity);
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Calculate ROI percentage
     */
    public BigDecimal getROIPercentage() {
        if (purchasePrice != null && purchasePrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal priceDiff = currentPrice != null ? currentPrice.subtract(purchasePrice) : BigDecimal.ZERO;
            return priceDiff.divide(purchasePrice, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        return BigDecimal.ZERO;
    }
    
    @Override
    public String toString() {
        return "Investment{" +
                "investmentName='" + investmentName + '\'' +
                ", symbol='" + symbol + '\'' +
                ", quantity=" + quantity +
                ", currentPrice=" + currentPrice +
                ", roi=" + getROI() +
                '}';
    }
}
