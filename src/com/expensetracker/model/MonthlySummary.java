package com.expensetracker.model;

import java.math.BigDecimal;

/**
 * Summary DTO for aggregated monthly cashflow (Income, Expense, Net Savings).
 */
public class MonthlySummary {

    private String monthKey;    // e.g. "2026-10"
    private String monthLabel;  // e.g. "Oct 2026"
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal netSavings;
    private double savingsRate; // Percentage e.g. 65.4%

    public MonthlySummary() {
        this.income = BigDecimal.ZERO;
        this.expense = BigDecimal.ZERO;
        this.netSavings = BigDecimal.ZERO;
    }

    public MonthlySummary(String monthKey, String monthLabel, BigDecimal income, BigDecimal expense,
                          BigDecimal netSavings, double savingsRate) {
        this.monthKey = monthKey;
        this.monthLabel = monthLabel;
        this.income = income;
        this.expense = expense;
        this.netSavings = netSavings;
        this.savingsRate = savingsRate;
    }

    public String getMonthKey() {
        return monthKey;
    }

    public void setMonthKey(String monthKey) {
        this.monthKey = monthKey;
    }

    public String getMonthLabel() {
        return monthLabel;
    }

    public void setMonthLabel(String monthLabel) {
        this.monthLabel = monthLabel;
    }

    public BigDecimal getIncome() {
        return income;
    }

    public void setIncome(BigDecimal income) {
        this.income = income;
    }

    public BigDecimal getExpense() {
        return expense;
    }

    public void setExpense(BigDecimal expense) {
        this.expense = expense;
    }

    public BigDecimal getNetSavings() {
        return netSavings;
    }

    public void setNetSavings(BigDecimal netSavings) {
        this.netSavings = netSavings;
    }

    public double getSavingsRate() {
        return savingsRate;
    }

    public void setSavingsRate(double savingsRate) {
        this.savingsRate = savingsRate;
    }
}
