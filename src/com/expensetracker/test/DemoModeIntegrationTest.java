package com.expensetracker.test;

import com.expensetracker.dao.CategoryDAO;
import com.expensetracker.dao.ProfileDAO;
import com.expensetracker.dao.TransactionDAO;
import com.expensetracker.model.Category;
import com.expensetracker.model.DashboardSummary;
import com.expensetracker.model.ReportData;
import com.expensetracker.model.Transaction;
import com.expensetracker.model.UserProfile;
import com.expensetracker.util.AppConfig;
import com.expensetracker.util.DBConnection;

import java.math.BigDecimal;
import java.util.List;

/**
 * Automated Verification Suite for Public Demo Mode.
 * Tests application execution, CRUD operations, calculations, and isolation without MySQL.
 */
public class DemoModeIntegrationTest {

    public static void main(String[] args) {
        System.out.println("====================================================================");
        System.out.println(" Personal Expense Tracker - Public Demo Mode Integration Tests");
        System.out.println("====================================================================");

        int passed = 0;
        int failed = 0;

        try {
            // STEP 1: Explicitly activate DEMO Mode
            AppConfig.setModeForTesting(AppConfig.Mode.DEMO);
            assertCondition(AppConfig.isDemoMode(), "1. AppConfig reports DEMO mode active");
            passed++;

            // STEP 2: Verify DBConnection is NOT available in DEMO mode
            assertCondition(!DBConnection.isAvailable(), "2. DBConnection is disabled in DEMO mode (Zero MySQL Dependency)");
            passed++;

            // STEP 3: Profile API in Demo Mode
            ProfileDAO profileDAO = new ProfileDAO();
            UserProfile profile = profileDAO.getProfile();
            assertCondition(profile != null && "Srinivas".equalsIgnoreCase(profile.getPersonName()),
                    "3. Profile retrieved in DEMO mode: personName = " + (profile != null ? profile.getPersonName() : "null"));
            passed++;

            // STEP 4: Category API in Demo Mode
            CategoryDAO categoryDAO = new CategoryDAO();
            List<Category> categories = categoryDAO.getAll();
            assertCondition(categories != null && categories.size() >= 15,
                    "4. Categories loaded in DEMO mode: count = " + (categories != null ? categories.size() : 0));
            passed++;

            // STEP 5: Initial Dashboard Summary
            TransactionDAO txDAO = new TransactionDAO();
            DashboardSummary dsBefore = txDAO.getDashboardSummary();
            assertCondition(dsBefore != null && dsBefore.getTotalIncome().compareTo(BigDecimal.ZERO) > 0,
                    "5. Dashboard summary loaded in DEMO mode: Total Income = " + dsBefore.getTotalIncome()
                            + ", Total Expense = " + dsBefore.getTotalExpense()
                            + ", Net Balance = " + dsBefore.getNetBalance()
                            + ", AppMode = " + dsBefore.getAppMode());
            passed++;

            BigDecimal initialExp = dsBefore.getTotalExpense();
            int initialCount = dsBefore.getTotalTransactions();

            // STEP 6: Transaction Listing
            List<Transaction> allTx = txDAO.getAll(null, null, null, null, null);
            assertCondition(allTx != null && allTx.size() >= 20,
                    "6. Transaction listing in DEMO mode: total retrieved = " + (allTx != null ? allTx.size() : 0));
            passed++;

            // STEP 7: Create (Insert) Transaction in Demo Mode
            Transaction newTx = new Transaction();
            newTx.setTitle("Demo Mode Laptop Stand");
            newTx.setAmount(new BigDecimal("1500.00"));
            newTx.setType("EXPENSE");
            newTx.setCategory("Shopping");
            newTx.setTransactionDate("2026-10-23");
            newTx.setPaymentMethod("Credit Card");
            newTx.setNotes("Synthetic test transaction created during demo");

            Transaction inserted = txDAO.insert(newTx);
            assertCondition(inserted != null && inserted.getId() > 0,
                    "7. Insert transaction in DEMO mode: generated ID = " + (inserted != null ? inserted.getId() : -1));
            passed++;

            // STEP 8: Verify Dashboard Recalculation after Insert
            DashboardSummary dsAfterInsert = txDAO.getDashboardSummary();
            BigDecimal expectedExp = initialExp.add(new BigDecimal("1500.00"));
            assertCondition(dsAfterInsert.getTotalExpense().compareTo(expectedExp) == 0
                    && dsAfterInsert.getTotalTransactions() == initialCount + 1,
                    "8. Dashboard recalculation after insert: Total Expense increased from "
                            + initialExp + " to " + dsAfterInsert.getTotalExpense());
            passed++;

            // STEP 9: Update Transaction in Demo Mode
            inserted.setAmount(new BigDecimal("2000.00"));
            inserted.setTitle("Demo Mode Ergonomic Laptop Stand (Updated)");
            boolean updated = txDAO.update(inserted);
            assertCondition(updated, "9. Update transaction in DEMO mode: updated = " + updated);
            passed++;

            // STEP 10: Verify Dashboard Recalculation after Update
            DashboardSummary dsAfterUpdate = txDAO.getDashboardSummary();
            BigDecimal expectedExpAfterUpdate = initialExp.add(new BigDecimal("2000.00"));
            assertCondition(dsAfterUpdate.getTotalExpense().compareTo(expectedExpAfterUpdate) == 0,
                    "10. Dashboard recalculation after update: Total Expense updated to "
                            + dsAfterUpdate.getTotalExpense());
            passed++;

            // STEP 11: Delete Transaction in Demo Mode
            boolean deleted = txDAO.delete(inserted.getId());
            assertCondition(deleted, "11. Delete transaction in DEMO mode: deleted = " + deleted);
            passed++;

            // STEP 12: Verify Dashboard Recalculation after Delete
            DashboardSummary dsAfterDelete = txDAO.getDashboardSummary();
            assertCondition(dsAfterDelete.getTotalExpense().compareTo(initialExp) == 0
                    && dsAfterDelete.getTotalTransactions() == initialCount,
                    "12. Dashboard recalculation after delete: Total Expense restored to initial "
                            + dsAfterDelete.getTotalExpense());
            passed++;

            // STEP 13: Report Generation in Demo Mode
            ReportData report = txDAO.getReportData();
            assertCondition(report != null && report.getMonthlyTrends() != null && !report.getMonthlyTrends().isEmpty(),
                    "13. Report analytics generated in DEMO mode: monthly trend count = "
                            + (report != null && report.getMonthlyTrends() != null ? report.getMonthlyTrends().size() : 0));
            passed++;

            // STEP 14: Profile Update in Demo Mode
            boolean nameUpdated = profileDAO.saveProfile("Srinivas Test");
            UserProfile updatedProf = profileDAO.getProfile();
            assertCondition(nameUpdated && "Srinivas Test".equalsIgnoreCase(updatedProf.getPersonName()),
                    "14. Person name updated in DEMO mode: " + updatedProf.getPersonName());
            passed++;

            // Restore name to Srinivas
            profileDAO.saveProfile("Srinivas");

            // STEP 15: Invalid APP_MODE Handling
            boolean invalidModeCaught = false;
            try {
                System.setProperty("app.mode", "invalid_mode_xyz");
                AppConfig.determineMode();
            } catch (IllegalStateException expected) {
                invalidModeCaught = true;
            } finally {
                System.clearProperty("app.mode");
            }
            assertCondition(invalidModeCaught, "15. Invalid APP_MODE rejected with clear configuration error");
            passed++;

            // STEP 16: Database Mode Isolation (when APP_MODE=database)
            AppConfig.setModeForTesting(AppConfig.Mode.DATABASE);
            assertCondition(AppConfig.isDatabaseMode(), "16. Switched to DATABASE mode successfully");
            passed++;

        } catch (Throwable t) {
            System.err.println("TEST FAILURE: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        System.out.println("====================================================================");
        System.out.println(" RESULTS: Passed = " + passed + " | Failed = " + failed);
        System.out.println("====================================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void assertCondition(boolean condition, String message) {
        if (condition) {
            System.out.println("[PASS] " + message);
        } else {
            System.err.println("[FAIL] " + message);
            throw new AssertionError("Assertion failed: " + message);
        }
    }
}
