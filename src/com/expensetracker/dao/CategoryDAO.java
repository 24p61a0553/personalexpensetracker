package com.expensetracker.dao;

import com.expensetracker.model.Category;
import com.expensetracker.util.DBConnection;
import com.expensetracker.util.DemoDataStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Categories.
 */
public class CategoryDAO {

    public List<Category> getAll() {
        return getByType(null);
    }

    public List<Category> getByType(String type) {
        if (!DBConnection.isAvailable()) {
            return DemoDataStore.getCategories(type);
        }

        List<Category> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT id, name, type, icon, color, description FROM categories ");
        boolean hasType = type != null && !type.trim().isEmpty() && !"ALL".equalsIgnoreCase(type);
        if (hasType) {
            sql.append("WHERE type = ? ");
        }
        sql.append("ORDER BY name ASC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            if (hasType) {
                ps.setString(1, type.toUpperCase());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Category c = new Category(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("type"),
                            rs.getString("icon"),
                            rs.getString("color"),
                            rs.getString("description")
                    );
                    list.add(c);
                }
            }

            if (list.isEmpty()) {
                return DemoDataStore.getCategories(type);
            }
            return list;
        } catch (SQLException e) {
            System.err.println("[CategoryDAO] Error reading categories from database: " + e.getMessage());
            return DemoDataStore.getCategories(type);
        }
    }
}
