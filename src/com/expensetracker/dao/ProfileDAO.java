package com.expensetracker.dao;

import com.expensetracker.model.UserProfile;
import com.expensetracker.util.DBConnection;
import com.expensetracker.util.DemoDataStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object for Person Profile management.
 * Persists the person's name in MySQL using PreparedStatements and updates
 * associated transactions to link with the active user profile.
 */
public class ProfileDAO {

    public UserProfile getProfile() {
        if (!DBConnection.isAvailable()) {
            return DemoDataStore.getProfile();
        }

        String sql = "SELECT id, person_name, updated_at FROM expense_user_profile WHERE id = 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("person_name");
                    if (name == null || name.trim().isEmpty()) {
                        name = "Srinivas";
                    }
                    return new UserProfile(rs.getInt("id"), name, rs.getTimestamp("updated_at"));
                }
            }

            // If table has no record yet, return default profile
            return new UserProfile(1, "Srinivas");
        } catch (SQLException e) {
            System.err.println("[ProfileDAO] getProfile SQL error: " + e.getMessage());
            return DemoDataStore.getProfile();
        }
    }

    public boolean saveProfile(String personName) throws SQLException {
        if (personName == null || personName.trim().isEmpty()) {
            throw new IllegalArgumentException("Person name cannot be blank or empty");
        }

        String cleanedName = personName.trim();
        if (cleanedName.length() > 100) {
            throw new IllegalArgumentException("Person name exceeds maximum allowed length of 100 characters");
        }

        if (!DBConnection.isAvailable()) {
            return DemoDataStore.saveProfile(cleanedName);
        }

        String upsertProfileSql = "INSERT INTO expense_user_profile (id, person_name) VALUES (1, ?) " +
                "ON DUPLICATE KEY UPDATE person_name = ?";
        String updateTransactionsSql = "UPDATE transactions SET person_name = ? WHERE profile_id = 1 OR profile_id IS NULL";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psProfile = conn.prepareStatement(upsertProfileSql);
                 PreparedStatement psTrans = conn.prepareStatement(updateTransactionsSql)) {

                psProfile.setString(1, cleanedName);
                psProfile.setString(2, cleanedName);
                psProfile.executeUpdate();

                psTrans.setString(1, cleanedName);
                psTrans.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                System.err.println("[ProfileDAO] Transaction rolled back due to error: " + ex.getMessage());
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public String getPersonName() {
        UserProfile p = getProfile();
        return (p != null && p.getPersonName() != null && !p.getPersonName().trim().isEmpty())
                ? p.getPersonName()
                : "Srinivas";
    }
}
