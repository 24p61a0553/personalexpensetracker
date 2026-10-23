package com.expensetracker.controller;

import com.expensetracker.dao.TransactionDAO;
import com.expensetracker.model.DashboardSummary;
import com.expensetracker.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet providing aggregated summary data for the primary dashboard.
 */
public class DashboardServlet extends HttpServlet {

    private final TransactionDAO transactionDAO = new TransactionDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            DashboardSummary summary = transactionDAO.getDashboardSummary();
            JsonUtil.sendSuccess(response, "Dashboard summary loaded successfully", summary);
        } catch (Exception e) {
            System.err.println("[DashboardServlet] Error: " + e.getMessage());
            JsonUtil.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to load dashboard summary: " + e.getMessage());
        }
    }
}
