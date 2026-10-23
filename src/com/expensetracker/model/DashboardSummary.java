package com.expensetracker.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Top-level DTO representing the comprehensive dashboard state.
 */
public class DashboardSummary {

    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netBalance;
    private BigDecimal thisMonthIncome;
    private BigDecimal thisMonthExpense;
    private double savingsRate;
    private int totalTransactions;
    private List<CategorySummary> categoryBreakdown;
    private List<Transaction> recentTransactions;
    private boolean databaseConnected;
    private String databaseSource;

    public DashboardSummary() {
        this.totalIncome = BigDecimal.ZERO;
        this.totalExpense = BigDecimal.ZERO;
        this.netBalance = BigDecimal.ZERO;
        this.thisMonthIncome = BigDecimal.ZERO;
        this.thisMonthExpense = BigDecimal.ZERO;
        this.categoryBreakdown = new ArrayList<>();
        this.recentTransactions = new ArrayList<>();
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = totalExpense;
    }

    public BigDecimal getNetBalance() {
        return netBalance;
    }

    public void setNetBalance(BigDecimal netBalance) {
        this.netBalance = netBalance;
    }

    public BigDecimal getThisMonthIncome() {
        return thisMonthIncome;
    }

    public void setThisMonthIncome(BigDecimal thisMonthIncome) {
        this.thisMonthIncome = thisMonthIncome;
    }

    public BigDecimal getThisMonthExpense() {
        return thisMonthExpense;
    }

    public void setThisMonthExpense(BigDecimal thisMonthExpense) {
        this.thisMonthExpense = thisMonthExpense;
    }

    public double getSavingsRate() {
        return savingsRate;
    }

    public void setSavingsRate(double savingsRate) {
        this.savingsRate = savingsRate;
    }

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(int totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public List<CategorySummary> getCategoryBreakdown() {
        return categoryBreakdown;
    }

    public void setCategoryBreakdown(List<CategorySummary> categoryBreakdown) {
        this.categoryBreakdown = categoryBreakdown;
    }

    public List<Transaction> getRecentTransactions() {
        return recentTransactions;
    }

    public void setRecentTransactions(List<Transaction> recentTransactions) {
        this.recentTransactions = recentTransactions;
    }

    public boolean isDatabaseConnected() {
        return databaseConnected;
    }

    public void setDatabaseConnected(boolean databaseConnected) {
        this.databaseConnected = databaseConnected;
    }

    public String getDatabaseSource() {
        return databaseSource;
    }

    public void setDatabaseSource(String databaseSource) {
        this.databaseSource = databaseSource;
    }
}
