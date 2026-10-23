package com.expensetracker.util;

import com.expensetracker.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Thread-safe fallback store used when MySQL database is unreachable or offline.
 * Enables zero-downtime evaluation, testing, and smooth onboarding.
 */
public class DemoDataStore {

    private static final Map<Integer, Transaction> transactions = new ConcurrentHashMap<>();
    private static final Map<String, Category> categories = new ConcurrentHashMap<>();
    private static final AtomicInteger idCounter = new AtomicInteger(100);
    private static volatile String personName = "Srinivas";

    public static UserProfile getProfile() {
        return new UserProfile(1, personName);
    }

    public static boolean saveProfile(String name) {
        if (name != null && !name.trim().isEmpty()) {
            personName = name.trim();
            for (Transaction t : transactions.values()) {
                t.setPersonName(personName);
            }
            return true;
        }
        return false;
    }

    static {
        seedCategories();
        seedTransactions();
    }

    private static void seedCategories() {
        addCat(new Category(1, "Food & Dining", "EXPENSE", "fa-utensils", "#f59e0b", "Groceries, cafes, food delivery"));
        addCat(new Category(2, "Rent & Housing", "EXPENSE", "fa-home", "#ef4444", "Apartment rent, property tax"));
        addCat(new Category(3, "Utilities", "EXPENSE", "fa-bolt", "#3b82f6", "Electricity, water, gas, broadband"));
        addCat(new Category(4, "Transportation", "EXPENSE", "fa-car", "#8b5cf6", "Fuel, public transit, cab fares"));
        addCat(new Category(5, "Entertainment", "EXPENSE", "fa-film", "#ec4899", "Streaming subscriptions, cinema, games"));
        addCat(new Category(6, "Shopping", "EXPENSE", "fa-shopping-bag", "#14b8a6", "Clothing, electronics, goods"));
        addCat(new Category(7, "Healthcare", "EXPENSE", "fa-heartbeat", "#06b6d4", "Medical, pharmacy, gym, fitness"));
        addCat(new Category(8, "Education", "EXPENSE", "fa-graduation-cap", "#6366f1", "Tuition, textbooks, courses"));
        addCat(new Category(9, "Miscellaneous", "EXPENSE", "fa-ellipsis-h", "#64748b", "Other personal expenditures"));

        addCat(new Category(10, "Salary", "INCOME", "fa-money-bill-wave", "#10b981", "Primary job monthly wage"));
        addCat(new Category(11, "Freelance", "INCOME", "fa-laptop-code", "#06b6d4", "Client consulting and contracts"));
        addCat(new Category(12, "Investments", "INCOME", "fa-chart-line", "#3b82f6", "Dividends, capital gains"));
        addCat(new Category(13, "Business", "INCOME", "fa-briefcase", "#8b5cf6", "Side-hustle revenue"));
        addCat(new Category(14, "Gifts & Grants", "INCOME", "fa-gift", "#ec4899", "Allowances, gifts, grants"));
        addCat(new Category(15, "Other Income", "INCOME", "fa-wallet", "#10b981", "Rebates, cashbacks, refunds"));
    }

    private static void addCat(Category c) {
        categories.put(c.getName().toLowerCase(), c);
    }

    private static void seedTransactions() {
        // Current month
        addTx(1, "Monthly Primary Salary", new BigDecimal("4850.00"), "INCOME", "Salary", "2026-10-01", "Bank Transfer", "Direct corporate payroll deposit");
        addTx(2, "Freelance Web App Development", new BigDecimal("950.00"), "INCOME", "Freelance", "2026-10-08", "PayPal", "Client website UI redesign contract");
        addTx(3, "Quarterly Stock Dividends", new BigDecimal("145.50"), "INCOME", "Investments", "2026-10-12", "Direct Deposit", "Index fund dividend payout");
        addTx(4, "Mobile App Consulting Session", new BigDecimal("300.00"), "INCOME", "Business", "2026-10-18", "Bank Transfer", "Tech architecture advisory session");
        addTx(5, "Credit Card Cashback Reward", new BigDecimal("42.80"), "INCOME", "Other Income", "2026-10-21", "Credit Card", "Monthly cash reward rebate");

        // Previous months income
        addTx(6, "Monthly Primary Salary (Sept)", new BigDecimal("4850.00"), "INCOME", "Salary", "2026-09-01", "Bank Transfer", "Payroll deposit September");
        addTx(7, "Freelance Dashboard API Project", new BigDecimal("800.00"), "INCOME", "Freelance", "2026-09-15", "PayPal", "REST API microservice integration");
        addTx(8, "Monthly Primary Salary (Aug)", new BigDecimal("4850.00"), "INCOME", "Salary", "2026-08-01", "Bank Transfer", "Payroll deposit August");
        addTx(9, "Stock Portfolio Dividend", new BigDecimal("120.00"), "INCOME", "Investments", "2026-08-14", "Direct Deposit", "August dividend distribution");

        // Current month expenses
        addTx(10, "Monthly Apartment Rent", new BigDecimal("1250.00"), "EXPENSE", "Rent & Housing", "2026-10-02", "Bank Transfer", "October residential lease payment");
        addTx(11, "Whole Foods Grocery Mart", new BigDecimal("194.30"), "EXPENSE", "Food & Dining", "2026-10-04", "Debit Card", "Weekly household groceries");
        addTx(12, "High-Speed Fiber Internet", new BigDecimal("65.00"), "EXPENSE", "Utilities", "2026-10-06", "Credit Card", "1 Gbps broadband subscription");
        addTx(13, "Gas Station Fuel Refill", new BigDecimal("54.00"), "EXPENSE", "Transportation", "2026-10-07", "Credit Card", "Vehicle unleaded fuel refill");
        addTx(14, "Dining at Italian Bistro", new BigDecimal("78.50"), "EXPENSE", "Food & Dining", "2026-10-10", "Credit Card", "Dinner with college friends");
        addTx(15, "Electric & Municipal Water Utility", new BigDecimal("118.25"), "EXPENSE", "Utilities", "2026-10-11", "Debit Card", "City electricity and water bill");
        addTx(16, "Gym Membership Monthly Fee", new BigDecimal("45.00"), "EXPENSE", "Healthcare", "2026-10-13", "Credit Card", "Fitness center monthly membership");
        addTx(17, "Pharmacy & Health Vitamins", new BigDecimal("32.60"), "EXPENSE", "Healthcare", "2026-10-15", "Debit Card", "Daily multivitamin supplements");
        addTx(18, "Streaming Video & Music Services", new BigDecimal("28.98"), "EXPENSE", "Entertainment", "2026-10-16", "Credit Card", "Netflix & Spotify subscriptions");
        addTx(19, "Weekend Farmers Market", new BigDecimal("62.40"), "EXPENSE", "Food & Dining", "2026-10-18", "Cash", "Fresh produce, eggs, fruits");
        addTx(20, "Online Cloud Certification Course", new BigDecimal("49.99"), "EXPENSE", "Education", "2026-10-19", "Credit Card", "AWS Solutions Architect prep course");
        addTx(21, "Autumn Clothing & Apparel", new BigDecimal("129.50"), "EXPENSE", "Shopping", "2026-10-20", "Credit Card", "Warm winter jackets and apparel");
        addTx(22, "Subway & Public Transit Pass", new BigDecimal("35.00"), "EXPENSE", "Transportation", "2026-10-22", "Debit Card", "Monthly commuter transit card");

        // Previous months expenses
        addTx(23, "Monthly Apartment Rent (Sept)", new BigDecimal("1250.00"), "EXPENSE", "Rent & Housing", "2026-09-02", "Bank Transfer", "September residential lease");
        addTx(24, "Supermarket Groceries (Sept)", new BigDecimal("380.00"), "EXPENSE", "Food & Dining", "2026-09-12", "Debit Card", "September total groceries");
        addTx(25, "Utilities Electricity & Gas (Sept)", new BigDecimal("135.00"), "EXPENSE", "Utilities", "2026-09-14", "Debit Card", "September utility bills");
        addTx(26, "Transit & Transportation (Sept)", new BigDecimal("110.00"), "EXPENSE", "Transportation", "2026-09-20", "Credit Card", "Fuel & Metro transit");
        addTx(27, "Books & Technical Manuals", new BigDecimal("75.00"), "EXPENSE", "Education", "2026-09-22", "Credit Card", "Software architecture textbooks");

        addTx(28, "Monthly Apartment Rent (Aug)", new BigDecimal("1250.00"), "EXPENSE", "Rent & Housing", "2026-08-02", "Bank Transfer", "August residential lease");
        addTx(29, "Supermarket Groceries (Aug)", new BigDecimal("395.00"), "EXPENSE", "Food & Dining", "2026-08-11", "Debit Card", "August total groceries");
        addTx(30, "Utilities Electricity & Gas (Aug)", new BigDecimal("142.00"), "EXPENSE", "Utilities", "2026-08-15", "Debit Card", "August electricity bill");
        addTx(31, "Concert & Summer Entertainment", new BigDecimal("120.00"), "EXPENSE", "Entertainment", "2026-08-25", "Credit Card", "Summer music concert ticket");
    }

    private static void addTx(int id, String title, BigDecimal amount, String type, String category,
                              String date, String paymentMethod, String notes) {
        Transaction t = new Transaction(id, title, amount, type, category, date, paymentMethod, notes);
        transactions.put(id, t);
    }

    public static List<Transaction> getAll(String type, String category, String startDate, String endDate, String search) {
        return transactions.values().stream()
                .filter(t -> type == null || type.isEmpty() || "ALL".equalsIgnoreCase(type) || t.getType().equalsIgnoreCase(type))
                .filter(t -> category == null || category.isEmpty() || "ALL".equalsIgnoreCase(category) || t.getCategory().equalsIgnoreCase(category))
                .filter(t -> startDate == null || startDate.isEmpty() || t.getTransactionDate().compareTo(startDate) >= 0)
                .filter(t -> endDate == null || endDate.isEmpty() || t.getTransactionDate().compareTo(endDate) <= 0)
                .filter(t -> {
                    if (search == null || search.trim().isEmpty()) return true;
                    String q = search.toLowerCase();
                    return (t.getTitle() != null && t.getTitle().toLowerCase().contains(q))
                            || (t.getCategory() != null && t.getCategory().toLowerCase().contains(q))
                            || (t.getNotes() != null && t.getNotes().toLowerCase().contains(q))
                            || (t.getPaymentMethod() != null && t.getPaymentMethod().toLowerCase().contains(q));
                })
                .sorted((a, b) -> b.getTransactionDate().compareTo(a.getTransactionDate()))
                .collect(Collectors.toList());
    }

    public static Transaction getById(int id) {
        return transactions.get(id);
    }

    public static Transaction insert(Transaction t) {
        int newId = idCounter.incrementAndGet();
        t.setId(newId);
        transactions.put(newId, t);
        return t;
    }

    public static boolean update(Transaction t) {
        if (!transactions.containsKey(t.getId())) {
            return false;
        }
        transactions.put(t.getId(), t);
        return true;
    }

    public static boolean delete(int id) {
        return transactions.remove(id) != null;
    }

    public static List<Category> getCategories(String type) {
        return categories.values().stream()
                .filter(c -> type == null || type.isEmpty() || "ALL".equalsIgnoreCase(type) || c.getType().equalsIgnoreCase(type))
                .sorted(Comparator.comparing(Category::getName))
                .collect(Collectors.toList());
    }

    public static DashboardSummary getDashboardSummary() {
        DashboardSummary ds = new DashboardSummary();
        BigDecimal totalInc = BigDecimal.ZERO;
        BigDecimal totalExp = BigDecimal.ZERO;

        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        BigDecimal curMonthInc = BigDecimal.ZERO;
        BigDecimal curMonthExp = BigDecimal.ZERO;

        Map<String, BigDecimal> expenseCatMap = new HashMap<>();
        Map<String, Integer> expenseCatCount = new HashMap<>();

        for (Transaction t : transactions.values()) {
            if ("INCOME".equalsIgnoreCase(t.getType())) {
                totalInc = totalInc.add(t.getAmount());
                if (t.getTransactionDate() != null && t.getTransactionDate().startsWith(currentMonth)) {
                    curMonthInc = curMonthInc.add(t.getAmount());
                }
            } else if ("EXPENSE".equalsIgnoreCase(t.getType())) {
                totalExp = totalExp.add(t.getAmount());
                if (t.getTransactionDate() != null && t.getTransactionDate().startsWith(currentMonth)) {
                    curMonthExp = curMonthExp.add(t.getAmount());
                }
                expenseCatMap.put(t.getCategory(), expenseCatMap.getOrDefault(t.getCategory(), BigDecimal.ZERO).add(t.getAmount()));
                expenseCatCount.put(t.getCategory(), expenseCatCount.getOrDefault(t.getCategory(), 0) + 1);
            }
        }

        BigDecimal netBal = totalInc.subtract(totalExp);
        double savingsRate = 0.0;
        if (totalInc.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = netBal.divide(totalInc, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
            if (savingsRate < 0) savingsRate = 0;
        }

        List<CategorySummary> catBreakdown = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : expenseCatMap.entrySet()) {
            double pct = 0.0;
            if (totalExp.compareTo(BigDecimal.ZERO) > 0) {
                pct = entry.getValue().divide(totalExp, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
            }
            Category cat = categories.get(entry.getKey().toLowerCase());
            String color = cat != null ? cat.getColor() : "#6366f1";
            String icon = cat != null ? cat.getIcon() : "fa-tag";
            catBreakdown.add(new CategorySummary(entry.getKey(), "EXPENSE", entry.getValue(), expenseCatCount.get(entry.getKey()), pct, color, icon));
        }
        catBreakdown.sort((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()));

        List<Transaction> recent = transactions.values().stream()
                .sorted((a, b) -> b.getTransactionDate().compareTo(a.getTransactionDate()))
                .limit(8)
                .collect(Collectors.toList());

        ds.setTotalIncome(totalInc);
        ds.setTotalExpense(totalExp);
        ds.setNetBalance(netBal);
        ds.setThisMonthIncome(curMonthInc);
        ds.setThisMonthExpense(curMonthExp);
        ds.setSavingsRate(savingsRate);
        ds.setTotalTransactions(transactions.size());
        ds.setPersonName(personName);
        ds.setCategoryBreakdown(catBreakdown);
        ds.setRecentTransactions(recent);
        return ds;
    }

    public static ReportData getReportData() {
        ReportData rd = new ReportData();
        Map<String, MonthlySummary> monthMap = new TreeMap<>();
        Map<String, BigDecimal> expenseCatMap = new HashMap<>();
        Map<String, Integer> expenseCatCount = new HashMap<>();
        Map<String, BigDecimal> incomeCatMap = new HashMap<>();
        Map<String, Integer> incomeCatCount = new HashMap<>();

        BigDecimal totalInc = BigDecimal.ZERO;
        BigDecimal totalExp = BigDecimal.ZERO;

        for (Transaction t : transactions.values()) {
            String mKey = t.getTransactionDate() != null && t.getTransactionDate().length() >= 7
                    ? t.getTransactionDate().substring(0, 7) : "2026-10";

            MonthlySummary ms = monthMap.computeIfAbsent(mKey, k -> {
                MonthlySummary m = new MonthlySummary();
                m.setMonthKey(k);
                m.setMonthLabel(formatMonthLabel(k));
                return m;
            });

            if ("INCOME".equalsIgnoreCase(t.getType())) {
                totalInc = totalInc.add(t.getAmount());
                ms.setIncome(ms.getIncome().add(t.getAmount()));
                incomeCatMap.put(t.getCategory(), incomeCatMap.getOrDefault(t.getCategory(), BigDecimal.ZERO).add(t.getAmount()));
                incomeCatCount.put(t.getCategory(), incomeCatCount.getOrDefault(t.getCategory(), 0) + 1);
            } else if ("EXPENSE".equalsIgnoreCase(t.getType())) {
                totalExp = totalExp.add(t.getAmount());
                ms.setExpense(ms.getExpense().add(t.getAmount()));
                expenseCatMap.put(t.getCategory(), expenseCatMap.getOrDefault(t.getCategory(), BigDecimal.ZERO).add(t.getAmount()));
                expenseCatCount.put(t.getCategory(), expenseCatCount.getOrDefault(t.getCategory(), 0) + 1);
            }
        }

        List<MonthlySummary> monthlyList = new ArrayList<>();
        for (MonthlySummary ms : monthMap.values()) {
            ms.setNetSavings(ms.getIncome().subtract(ms.getExpense()));
            double sr = 0.0;
            if (ms.getIncome().compareTo(BigDecimal.ZERO) > 0) {
                sr = ms.getNetSavings().divide(ms.getIncome(), 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
            }
            ms.setSavingsRate(sr);
            monthlyList.add(ms);
        }

        List<CategorySummary> expCats = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : expenseCatMap.entrySet()) {
            double pct = totalExp.compareTo(BigDecimal.ZERO) > 0
                    ? e.getValue().divide(totalExp, 4, RoundingMode.HALF_UP).doubleValue() * 100.0 : 0.0;
            Category c = categories.get(e.getKey().toLowerCase());
            expCats.add(new CategorySummary(e.getKey(), "EXPENSE", e.getValue(), expenseCatCount.get(e.getKey()), pct,
                    c != null ? c.getColor() : "#ef4444", c != null ? c.getIcon() : "fa-tag"));
        }
        expCats.sort((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()));

        List<CategorySummary> incCats = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : incomeCatMap.entrySet()) {
            double pct = totalInc.compareTo(BigDecimal.ZERO) > 0
                    ? e.getValue().divide(totalInc, 4, RoundingMode.HALF_UP).doubleValue() * 100.0 : 0.0;
            Category c = categories.get(e.getKey().toLowerCase());
            incCats.add(new CategorySummary(e.getKey(), "INCOME", e.getValue(), incomeCatCount.get(e.getKey()), pct,
                    c != null ? c.getColor() : "#10b981", c != null ? c.getIcon() : "fa-money-bill-wave"));
        }
        incCats.sort((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()));

        rd.setMonthlyTrends(monthlyList);
        rd.setExpenseCategories(expCats);
        rd.setIncomeCategories(incCats);
        rd.setTotalIncome(totalInc);
        rd.setTotalExpense(totalExp);
        rd.setNetSavings(totalInc.subtract(totalExp));

        double overallSr = totalInc.compareTo(BigDecimal.ZERO) > 0
                ? rd.getNetSavings().divide(totalInc, 4, RoundingMode.HALF_UP).doubleValue() * 100.0 : 0.0;
        rd.setOverallSavingsRate(overallSr);

        int monthCount = Math.max(1, monthlyList.size());
        rd.setAverageMonthlyExpense(totalExp.divide(new BigDecimal(monthCount), 2, RoundingMode.HALF_UP));

        if (!expCats.isEmpty()) {
            rd.setTopExpenseCategory(expCats.get(0).getCategory());
            rd.setTopExpenseCategoryAmount(expCats.get(0).getTotalAmount());
        }
        if (!incCats.isEmpty()) {
            rd.setTopIncomeCategory(incCats.get(0).getCategory());
            rd.setTopIncomeCategoryAmount(incCats.get(0).getTotalAmount());
        }

        return rd;
    }

    private static String formatMonthLabel(String yearMonth) {
        try {
            LocalDate d = LocalDate.parse(yearMonth + "-01");
            return d.format(DateTimeFormatter.ofPattern("MMM yyyy"));
        } catch (Exception e) {
            return yearMonth;
        }
    }
}
