package com.financetracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transfer Model Class
 */
public class Transfer {
    private UUID transferId;
    private UUID userId;
    private UUID fromAccountId;
    private UUID toAccountId;
    private BigDecimal amount;
    private LocalDate transferDate;
    private String description;
    private String transferType;
    private LocalDateTime createdAt;
    
    // Transient fields for display
    private String fromAccountName;
    private String toAccountName;
    
    // Constructors
    public Transfer() {
        this.transferId = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.transferType = "INTERNAL";
    }
    
    public Transfer(UUID fromAccountId, UUID toAccountId, BigDecimal amount, LocalDate transferDate) {
        this();
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.transferDate = transferDate;
    }
    
    // Getters and Setters
    public UUID getTransferId() {
        return transferId;
    }
    
    public void setTransferId(UUID transferId) {
        this.transferId = transferId;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public UUID getFromAccountId() {
        return fromAccountId;
    }
    
    public void setFromAccountId(UUID fromAccountId) {
        this.fromAccountId = fromAccountId;
    }
    
    public UUID getToAccountId() {
        return toAccountId;
    }
    
    public void setToAccountId(UUID toAccountId) {
        this.toAccountId = toAccountId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public LocalDate getTransferDate() {
        return transferDate;
    }
    
    public void setTransferDate(LocalDate transferDate) {
        this.transferDate = transferDate;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getTransferType() {
        return transferType;
    }
    
    public void setTransferType(String transferType) {
        this.transferType = transferType;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getFromAccountName() {
        return fromAccountName;
    }
    
    public void setFromAccountName(String fromAccountName) {
        this.fromAccountName = fromAccountName;
    }
    
    public String getToAccountName() {
        return toAccountName;
    }
    
    public void setToAccountName(String toAccountName) {
        this.toAccountName = toAccountName;
    }
    
    @Override
    public String toString() {
        return "Transfer{" +
                "fromAccount=" + fromAccountName +
                ", toAccount=" + toAccountName +
                ", amount=" + amount +
                ", date=" + transferDate +
                '}';
    }
}
