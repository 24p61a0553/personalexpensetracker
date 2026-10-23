package com.expensetracker.dao;

import com.expensetracker.model.*;
import com.expensetracker.util.AppConfig;
import com.expensetracker.util.DBConnection;
import com.expensetracker.util.DemoDataStore;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Data Access Object for Transactions.
 * Implements CRUD operations, filtering, and aggregated financial summaries.
 * Automatically respects AppConfig (DEMO vs DATABASE mode).
 */
public class TransactionDAO {

    private final ProfileDAO profileDAO = new ProfileDAO();

    public List<Transaction> getAll(String type, String category, String startDate, String endDate, String search) {
        if (AppConfig.isDemoMode()) {
            return DemoDataStore.getAll(type, category, startDate, endDate, search);
        }

        List<Transaction> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT id, title, amount, type, category, transaction_date, payment_method, notes, profile_id, person_name, created_at, updated_at FROM transactions WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (type != null && !type.trim().isEmpty() && !"ALL".equalsIgnoreCase(type)) {
            sql.append("AND type = ? ");
            params.add(type.toUpperCase());
        }

        if (category != null && !category.trim().isEmpty() && !"ALL".equalsIgnoreCase(category)) {
            sql.append("AND category = ? ");
            params.add(category);
        }

        if (startDate != null && !startDate.trim().isEmpty()) {
            sql.append("AND transaction_date >= ? ");
            params.add(startDate);
        }

        if (endDate != null && !endDate.trim().isEmpty()) {
            sql.append("AND transaction_date <= ? ");
            params.add(endDate);
        }

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (title LIKE ? OR category LIKE ? OR notes LIKE ? OR payment_method LIKE ?) ");
            String q = "%" + search.trim() + "%";
            params.add(q);
            params.add(q);
            params.add(q);
            params.add(q);
        }

        sql.append("ORDER BY transaction_date DESC, id DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Transaction t = mapResultSetToTransaction(rs);
                    list.add(t);
                }
            }
            return list;
        } catch (SQLException e) {
            System.err.println("[TransactionDAO] getAll error in DATABASE mode: " + e.getMessage());
            throw new RuntimeException("Database error in DATABASE mode: " + e.getMessage(), e);
        }
    }

    public Transaction getById(int id) {
        if (AppConfig.isDemoMode()) {
            return DemoDataStore.getById(id);
        }

        String sql = "SELECT id, title, amount, type, category, transaction_date, payment_method, notes, profile_id, person_name, created_at, updated_at FROM transactions WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTransaction(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            System.err.println("[TransactionDAO] getById error in DATABASE mode: " + e.getMessage());
            throw new RuntimeException("Database error in DATABASE mode: " + e.getMessage(), e);
        }
    }

    public Transaction insert(Transaction t) {
        if (AppConfig.isDemoMode()) {
            return DemoDataStore.insert(t);
        }

        String personName = t.getPersonName();
        if (personName == null || personName.trim().isEmpty() || "Enter your name".equalsIgnoreCase(personName.trim())) {
            personName = profileDAO.getPersonName();
        }
        int profileId = t.getProfileId() > 0 ? t.getProfileId() : 1;

        String sql = "INSERT INTO transactions (title, amount, type, category, transaction_date, payment_method, notes, profile_id, person_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, t.getTitle());
            ps.setBigDecimal(2, t.getAmount());
            ps.setString(3, t.getType().toUpperCase());
            ps.setString(4, t.getCategory());
            ps.setDate(5, java.sql.Date.valueOf(t.getTransactionDate()));
            ps.setString(6, t.getPaymentMethod() != null ? t.getPaymentMethod() : "Cash");
            ps.setString(7, t.getNotes());
            ps.setInt(8, profileId);
            ps.setString(9, personName);

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        t.setId(generatedKeys.getInt(1));
                    }
                }
            }
            return t;
        } catch (SQLException e) {
            System.err.println("[TransactionDAO] insert error in DATABASE mode: " + e.getMessage());
            throw new RuntimeException("Database error in DATABASE mode: " + e.getMessage(), e);
        }
    }

    public boolean update(Transaction t) {
        if (AppConfig.isDemoMode()) {
            return DemoDataStore.update(t);
        }

        String sql = "UPDATE transactions SET title = ?, amount = ?, type = ?, category = ?, transaction_date = ?, payment_method = ?, notes = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, t.getTitle());
            ps.setBigDecimal(2, t.getAmount());
            ps.setString(3, t.getType().toUpperCase());
            ps.setString(4, t.getCategory());
            ps.setDate(5, java.sql.Date.valueOf(t.getTransactionDate()));
            ps.setString(6, t.getPaymentMethod() != null ? t.getPaymentMethod() : "Cash");
            ps.setString(7, t.getNotes());
            ps.setInt(8, t.getId());

            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            System.err.println("[TransactionDAO] update error in DATABASE mode: " + e.getMessage());
            throw new RuntimeException("Database error in DATABASE mode: " + e.getMessage(), e);
        }
    }

    public boolean delete(int id) {
        if (AppConfig.isDemoMode()) {
            return DemoDataStore.delete(id);
        }

        String sql = "DELETE FROM transactions WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            System.err.println("[TransactionDAO] delete error in DATABASE mode: " + e.getMessage());
            throw new RuntimeException("Database error in DATABASE mode: " + e.getMessage(), e);
        }
    }

    public DashboardSummary getDashboardSummary() {
        if (AppConfig.isDemoMode()) {
            return DemoDataStore.getDashboardSummary();
        }

        DashboardSummary ds = new DashboardSummary();
        ds.setDatabaseConnected(true);
        ds.setAppMode("database");
        ds.setDatabaseSource("MySQL Database (" + DBConnection.getConfigSource() + ")");
        ds.setPersonName(profileDAO.getPersonName());

        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        try (Connection conn = DBConnection.getConnection()) {
            // 1. Totals (Total income, total expense, total count)
            String totalSql = "SELECT " +
                    "COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS total_income, " +
                    "COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS total_expense, " +
                    "COUNT(*) AS total_count " +
                    "FROM transactions";

            try (PreparedStatement ps = conn.prepareStatement(totalSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal totalInc = rs.getBigDecimal("total_income");
                    BigDecimal totalExp = rs.getBigDecimal("total_expense");
                    ds.setTotalIncome(totalInc);
                    ds.setTotalExpense(totalExp);
                    ds.setNetBalance(totalInc.subtract(totalExp));
                    ds.setTotalTransactions(rs.getInt("total_count"));

                    if (totalInc.compareTo(BigDecimal.ZERO) > 0) {
                        double sr = ds.getNetBalance().divide(totalInc, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
                        ds.setSavingsRate(Math.max(0, sr));
                    }
                }
            }

            // 2. This Month totals
            String curMonthSql = "SELECT " +
                    "COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS month_income, " +
                    "COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS month_expense " +
                    "FROM transactions WHERE DATE_FORMAT(transaction_date, '%Y-%m') = ?";

            try (PreparedStatement ps = conn.prepareStatement(curMonthSql)) {
                ps.setString(1, currentMonth);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        ds.setThisMonthIncome(rs.getBigDecimal("month_income"));
                        ds.setThisMonthExpense(rs.getBigDecimal("month_expense"));
                    }
                }
            }

            // 3. Category Breakdown (Expenses)
            String catSql = "SELECT t.category, SUM(t.amount) AS total_amount, COUNT(t.id) AS tx_count, " +
                    "COALESCE(c.color, '#6366f1') AS color, COALESCE(c.icon, 'fa-tag') AS icon " +
                    "FROM transactions t " +
                    "LEFT JOIN categories c ON t.category = c.name " +
                    "WHERE t.type = 'EXPENSE' " +
                    "GROUP BY t.category, c.color, c.icon " +
                    "ORDER BY total_amount DESC";

            List<CategorySummary> catList = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(catSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal amt = rs.getBigDecimal("total_amount");
                    double pct = ds.getTotalExpense().compareTo(BigDecimal.ZERO) > 0
                            ? amt.divide(ds.getTotalExpense(), 4, RoundingMode.HALF_UP).doubleValue() * 100.0 : 0.0;
                    catList.add(new CategorySummary(
                            rs.getString("category"),
                            "EXPENSE",
                            amt,
                            rs.getInt("tx_count"),
                            pct,
                            rs.getString("color"),
                            rs.getString("icon")
                    ));
                }
            }
            ds.setCategoryBreakdown(catList);

            // 4. Recent Transactions (last 8)
            String recentSql = "SELECT id, title, amount, type, category, transaction_date, payment_method, notes, profile_id, person_name, created_at, updated_at " +
                    "FROM transactions ORDER BY transaction_date DESC, id DESC LIMIT 8";
            List<Transaction> recentList = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(recentSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    recentList.add(mapResultSetToTransaction(rs));
                }
            }
            ds.setRecentTransactions(recentList);

            return ds;
        } catch (SQLException e) {
            System.err.println("[TransactionDAO] getDashboardSummary SQL error in DATABASE mode: " + e.getMessage());
            throw new RuntimeException("Database query failed in DATABASE mode: " + e.getMessage(), e);
        }
    }

    public ReportData getReportData() {
        if (AppConfig.isDemoMode()) {
            return DemoDataStore.getReportData();
        }

        ReportData rd = new ReportData();
        try (Connection conn = DBConnection.getConnection()) {
            // 1. Overall Totals
            String totalSql = "SELECT " +
                    "COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS total_income, " +
                    "COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS total_expense " +
                    "FROM transactions";

            try (PreparedStatement ps = conn.prepareStatement(totalSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal totInc = rs.getBigDecimal("total_income");
                    BigDecimal totExp = rs.getBigDecimal("total_expense");
                    rd.setTotalIncome(totInc);
                    rd.setTotalExpense(totExp);
                    rd.setNetSavings(totInc.subtract(totExp));

                    if (totInc.compareTo(BigDecimal.ZERO) > 0) {
                        double sr = rd.getNetSavings().divide(totInc, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
                        rd.setOverallSavingsRate(Math.max(0, sr));
                    }
                }
            }

            // 2. Monthly Trends (Past 12 months)
            String monthSql = "SELECT " +
                    "DATE_FORMAT(transaction_date, '%Y-%m') AS month_key, " +
                    "DATE_FORMAT(transaction_date, '%b %Y') AS month_label, " +
                    "COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS month_income, " +
                    "COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS month_expense " +
                    "FROM transactions " +
                    "GROUP BY month_key, month_label " +
                    "ORDER BY month_key ASC";

            List<MonthlySummary> monthlyTrends = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(monthSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal inc = rs.getBigDecimal("month_income");
                    BigDecimal exp = rs.getBigDecimal("month_expense");
                    BigDecimal net = inc.subtract(exp);
                    double sr = inc.compareTo(BigDecimal.ZERO) > 0
                            ? net.divide(inc, 4, RoundingMode.HALF_UP).doubleValue() * 100.0 : 0.0;
                    monthlyTrends.add(new MonthlySummary(
                            rs.getString("month_key"),
                            rs.getString("month_label"),
                            inc,
                            exp,
                            net,
                            Math.max(0, sr)
                    ));
                }
            }
            rd.setMonthlyTrends(monthlyTrends);

            int monthCount = Math.max(1, monthlyTrends.size());
            rd.setAverageMonthlyExpense(rd.getTotalExpense().divide(new BigDecimal(monthCount), 2, RoundingMode.HALF_UP));

            // 3. Expense Categories
            String expCatSql = "SELECT t.category, SUM(t.amount) AS total_amount, COUNT(t.id) AS tx_count, " +
                    "COALESCE(c.color, '#ef4444') AS color, COALESCE(c.icon, 'fa-tag') AS icon " +
                    "FROM transactions t " +
                    "LEFT JOIN categories c ON t.category = c.name " +
                    "WHERE t.type = 'EXPENSE' " +
                    "GROUP BY t.category, c.color, c.icon " +
                    "ORDER BY total_amount DESC";

            List<CategorySummary> expList = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(expCatSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal amt = rs.getBigDecimal("total_amount");
                    double pct = rd.getTotalExpense().compareTo(BigDecimal.ZERO) > 0
                            ? amt.divide(rd.getTotalExpense(), 4, RoundingMode.HALF_UP).doubleValue() * 100.0 : 0.0;
                    expList.add(new CategorySummary(
                            rs.getString("category"), "EXPENSE", amt, rs.getInt("tx_count"), pct,
                            rs.getString("color"), rs.getString("icon")));
                }
            }
            rd.setExpenseCategories(expList);
            if (!expList.isEmpty()) {
                rd.setTopExpenseCategory(expList.get(0).getCategory());
                rd.setTopExpenseCategoryAmount(expList.get(0).getTotalAmount());
            }

            // 4. Income Categories
            String incCatSql = "SELECT t.category, SUM(t.amount) AS total_amount, COUNT(t.id) AS tx_count, " +
                    "COALESCE(c.color, '#10b981') AS color, COALESCE(c.icon, 'fa-money-bill-wave') AS icon " +
                    "FROM transactions t " +
                    "LEFT JOIN categories c ON t.category = c.name " +
                    "WHERE t.type = 'INCOME' " +
                    "GROUP BY t.category, c.color, c.icon " +
                    "ORDER BY total_amount DESC";

            List<CategorySummary> incList = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(incCatSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal amt = rs.getBigDecimal("total_amount");
                    double pct = rd.getTotalIncome().compareTo(BigDecimal.ZERO) > 0
                            ? amt.divide(rd.getTotalIncome(), 4, RoundingMode.HALF_UP).doubleValue() * 100.0 : 0.0;
                    incList.add(new CategorySummary(
                            rs.getString("category"), "INCOME", amt, rs.getInt("tx_count"), pct,
                            rs.getString("color"), rs.getString("icon")));
                }
            }
            rd.setIncomeCategories(incList);
            if (!incList.isEmpty()) {
                rd.setTopIncomeCategory(incList.get(0).getCategory());
                rd.setTopIncomeCategoryAmount(incList.get(0).getTotalAmount());
            }

            return rd;
        } catch (SQLException e) {
            System.err.println("[TransactionDAO] getReportData SQL error in DATABASE mode: " + e.getMessage());
            throw new RuntimeException("Database error in DATABASE mode: " + e.getMessage(), e);
        }
    }

    private Transaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setId(rs.getInt("id"));
        t.setTitle(rs.getString("title"));
        t.setAmount(rs.getBigDecimal("amount"));
        t.setType(rs.getString("type"));
        t.setCategory(rs.getString("category"));
        java.sql.Date d = rs.getDate("transaction_date");
        t.setTransactionDate(d != null ? d.toString() : "");
        t.setPaymentMethod(rs.getString("payment_method"));
        t.setNotes(rs.getString("notes"));
        try {
            t.setProfileId(rs.getInt("profile_id"));
            t.setPersonName(rs.getString("person_name"));
        } catch (SQLException ignored) {}
        t.setCreatedAt(rs.getTimestamp("created_at"));
        t.setUpdatedAt(rs.getTimestamp("updated_at"));
        return t;
    }
}
