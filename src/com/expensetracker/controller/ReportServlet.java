package com.expensetracker.controller;

import com.expensetracker.dao.TransactionDAO;
import com.expensetracker.model.ReportData;
import com.expensetracker.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet providing comprehensive financial reports, monthly trends, and category analysis.
 */
public class ReportServlet extends HttpServlet {

    private final TransactionDAO transactionDAO = new TransactionDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            ReportData report = transactionDAO.getReportData();
            JsonUtil.sendSuccess(response, "Financial report data loaded successfully", report);
        } catch (Exception e) {
            System.err.println("[ReportServlet] Error: " + e.getMessage());
            JsonUtil.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to generate reports: " + e.getMessage());
        }
    }
}
