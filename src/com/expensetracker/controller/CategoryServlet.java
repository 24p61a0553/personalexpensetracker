package com.expensetracker.controller;

import com.expensetracker.dao.CategoryDAO;
import com.expensetracker.model.Category;
import com.expensetracker.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Servlet providing available transaction categories with icons, colors, and types.
 */
public class CategoryServlet extends HttpServlet {

    private final CategoryDAO categoryDAO = new CategoryDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String type = request.getParameter("type");
        try {
            List<Category> list = categoryDAO.getByType(type);
            JsonUtil.sendSuccess(response, "Categories loaded successfully", list);
        } catch (Exception e) {
            System.err.println("[CategoryServlet] Error: " + e.getMessage());
            JsonUtil.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to load categories: " + e.getMessage());
        }
    }
}
