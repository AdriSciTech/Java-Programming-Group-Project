package com.financetracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Bill and Subscription Model Class
 */
public class Bill {
    private UUID billId;
    private UUID userId;
    private UUID categoryId;
    private String name;
    private BigDecimal amount;
    private String billingCycle;
    private int dueDay;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isActive;
    private int reminderDays;
    private LocalDate lastPaymentDate;
    private LocalDate nextPaymentDate;
    private String description;
    private String vendor;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Transient field for display
    private String categoryName;
    
    // Constructors
    public Bill() {
        this.billId = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isActive = true;
        this.reminderDays = 3;
    }
    
    public Bill(String name, BigDecimal amount, String billingCycle, int dueDay) {
        this();
        this.name = name;
        this.amount = amount;
        this.billingCycle = billingCycle;
        this.dueDay = dueDay;
    }
    
    // Getters and Setters
    public UUID getBillId() {
        return billId;
    }
    
    public void setBillId(UUID billId) {
        this.billId = billId;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public UUID getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getBillingCycle() {
        return billingCycle;
    }
    
    public void setBillingCycle(String billingCycle) {
        this.billingCycle = billingCycle;
    }
    
    public int getDueDay() {
        return dueDay;
    }
    
    public void setDueDay(int dueDay) {
        this.dueDay = dueDay;
    }
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public int getReminderDays() {
        return reminderDays;
    }
    
    public void setReminderDays(int reminderDays) {
        this.reminderDays = reminderDays;
    }
    
    public LocalDate getLastPaymentDate() {
        return lastPaymentDate;
    }
    
    public void setLastPaymentDate(LocalDate lastPaymentDate) {
        this.lastPaymentDate = lastPaymentDate;
    }
    
    public LocalDate getNextPaymentDate() {
        return nextPaymentDate;
    }
    
    public void setNextPaymentDate(LocalDate nextPaymentDate) {
        this.nextPaymentDate = nextPaymentDate;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getVendor() {
        return vendor;
    }
    
    public void setVendor(String vendor) {
        this.vendor = vendor;
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
    
    public String getCategoryName() {
        return categoryName;
    }
    
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
    
    @Override
    public String toString() {
        return "Bill{" +
                "name='" + name + '\'' +
                ", amount=" + amount +
                ", billingCycle='" + billingCycle + '\'' +
                '}';
    }
}
