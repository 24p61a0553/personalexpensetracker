package com.expensetracker.model;

import java.math.BigDecimal;

/**
 * Summary DTO for aggregated category statistics (charts and breakdown tables).
 */
public class CategorySummary {

    private String category;
    private String type;
    private BigDecimal totalAmount;
    private int transactionCount;
    private double percentage;
    private String color;
    private String icon;

    public CategorySummary() {
    }

    public CategorySummary(String category, String type, BigDecimal totalAmount, int transactionCount,
                           double percentage, String color, String icon) {
        this.category = category;
        this.type = type;
        this.totalAmount = totalAmount;
        this.transactionCount = transactionCount;
        this.percentage = percentage;
        this.color = color;
        this.icon = icon;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}
