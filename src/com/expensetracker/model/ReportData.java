package com.expensetracker.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO encapsulating comprehensive financial analytics and reports.
 */
public class ReportData {

    private List<MonthlySummary> monthlyTrends;
    private List<CategorySummary> expenseCategories;
    private List<CategorySummary> incomeCategories;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netSavings;
    private double overallSavingsRate;
    private BigDecimal averageMonthlyExpense;
    private String topExpenseCategory;
    private BigDecimal topExpenseCategoryAmount;
    private String topIncomeCategory;
    private BigDecimal topIncomeCategoryAmount;

    public ReportData() {
        this.monthlyTrends = new ArrayList<>();
        this.expenseCategories = new ArrayList<>();
        this.incomeCategories = new ArrayList<>();
        this.totalIncome = BigDecimal.ZERO;
        this.totalExpense = BigDecimal.ZERO;
        this.netSavings = BigDecimal.ZERO;
        this.averageMonthlyExpense = BigDecimal.ZERO;
        this.topExpenseCategoryAmount = BigDecimal.ZERO;
        this.topIncomeCategoryAmount = BigDecimal.ZERO;
    }

    public List<MonthlySummary> getMonthlyTrends() {
        return monthlyTrends;
    }

    public void setMonthlyTrends(List<MonthlySummary> monthlyTrends) {
        this.monthlyTrends = monthlyTrends;
    }

    public List<CategorySummary> getExpenseCategories() {
        return expenseCategories;
    }

    public void setExpenseCategories(List<CategorySummary> expenseCategories) {
        this.expenseCategories = expenseCategories;
    }

    public List<CategorySummary> getIncomeCategories() {
        return incomeCategories;
    }

    public void setIncomeCategories(List<CategorySummary> incomeCategories) {
        this.incomeCategories = incomeCategories;
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

    public BigDecimal getNetSavings() {
        return netSavings;
    }

    public void setNetSavings(BigDecimal netSavings) {
        this.netSavings = netSavings;
    }

    public double getOverallSavingsRate() {
        return overallSavingsRate;
    }

    public void setOverallSavingsRate(double overallSavingsRate) {
        this.overallSavingsRate = overallSavingsRate;
    }

    public BigDecimal getAverageMonthlyExpense() {
        return averageMonthlyExpense;
    }

    public void setAverageMonthlyExpense(BigDecimal averageMonthlyExpense) {
        this.averageMonthlyExpense = averageMonthlyExpense;
    }

    public String getTopExpenseCategory() {
        return topExpenseCategory;
    }

    public void setTopExpenseCategory(String topExpenseCategory) {
        this.topExpenseCategory = topExpenseCategory;
    }

    public BigDecimal getTopExpenseCategoryAmount() {
        return topExpenseCategoryAmount;
    }

    public void setTopExpenseCategoryAmount(BigDecimal topExpenseCategoryAmount) {
        this.topExpenseCategoryAmount = topExpenseCategoryAmount;
    }

    public String getTopIncomeCategory() {
        return topIncomeCategory;
    }

    public void setTopIncomeCategory(String topIncomeCategory) {
        this.topIncomeCategory = topIncomeCategory;
    }

    public BigDecimal getTopIncomeCategoryAmount() {
        return topIncomeCategoryAmount;
    }

    public void setTopIncomeCategoryAmount(BigDecimal topIncomeCategoryAmount) {
        this.topIncomeCategoryAmount = topIncomeCategoryAmount;
    }
}
