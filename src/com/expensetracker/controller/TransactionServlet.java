package com.expensetracker.controller;

import com.expensetracker.dao.TransactionDAO;
import com.expensetracker.model.Transaction;
import com.expensetracker.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Servlet handling CRUD operations and filtering for Income and Expense transactions.
 */
public class TransactionServlet extends HttpServlet {

    private final TransactionDAO transactionDAO = new TransactionDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idParam = request.getParameter("id");
        if (idParam != null && !idParam.trim().isEmpty()) {
            try {
                int id = Integer.parseInt(idParam.trim());
                Transaction t = transactionDAO.getById(id);
                if (t != null) {
                    JsonUtil.sendSuccess(response, "Transaction retrieved", t);
                } else {
                    JsonUtil.sendError(response, HttpServletResponse.SC_NOT_FOUND, "Transaction not found with ID " + id);
                }
            } catch (NumberFormatException e) {
                JsonUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid transaction ID format");
            }
            return;
        }

        String type = request.getParameter("type");
        String category = request.getParameter("category");
        String startDate = request.getParameter("startDate");
        String endDate = request.getParameter("endDate");
        String search = request.getParameter("search");

        try {
            List<Transaction> list = transactionDAO.getAll(type, category, startDate, endDate, search);
            JsonUtil.sendSuccess(response, "Transactions retrieved successfully", list);
        } catch (Exception e) {
            System.err.println("[TransactionServlet] Error listing transactions: " + e.getMessage());
            JsonUtil.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        String action = request.getParameter("action");

        // Support POST override for update and delete
        if ("/update".equals(pathInfo) || "update".equalsIgnoreCase(action)) {
            doPut(request, response);
            return;
        }
        if ("/delete".equals(pathInfo) || "delete".equalsIgnoreCase(action)) {
            doDelete(request, response);
            return;
        }

        try {
            Transaction input = JsonUtil.readRequestBody(request, Transaction.class);
            String validationError = validateTransaction(input, false);
            if (validationError != null) {
                JsonUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST, validationError);
                return;
            }

            Transaction saved = transactionDAO.insert(input);
            JsonUtil.sendSuccess(response, "Transaction added successfully", saved);
        } catch (Exception e) {
            System.err.println("[TransactionServlet] Error creating transaction: " + e.getMessage());
            JsonUtil.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create transaction: " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            Transaction input = JsonUtil.readRequestBody(request, Transaction.class);
            String validationError = validateTransaction(input, true);
            if (validationError != null) {
                JsonUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST, validationError);
                return;
            }

            boolean updated = transactionDAO.update(input);
            if (updated) {
                JsonUtil.sendSuccess(response, "Transaction updated successfully", input);
            } else {
                JsonUtil.sendError(response, HttpServletResponse.SC_NOT_FOUND, "Transaction not found for ID " + input.getId());
            }
        } catch (Exception e) {
            System.err.println("[TransactionServlet] Error updating transaction: " + e.getMessage());
            JsonUtil.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to update transaction: " + e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idParam = request.getParameter("id");
        if (idParam == null || idParam.trim().isEmpty()) {
            try {
                Transaction body = JsonUtil.readRequestBody(request, Transaction.class);
                if (body != null && body.getId() > 0) {
                    idParam = String.valueOf(body.getId());
                }
            } catch (Exception ignored) {}
        }

        if (idParam == null || idParam.trim().isEmpty()) {
            JsonUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Transaction ID is required for deletion");
            return;
        }

        try {
            int id = Integer.parseInt(idParam.trim());
            boolean deleted = transactionDAO.delete(id);
            if (deleted) {
                JsonUtil.sendSuccess(response, "Transaction deleted successfully", id);
            } else {
                JsonUtil.sendError(response, HttpServletResponse.SC_NOT_FOUND, "Transaction not found or already deleted");
            }
        } catch (NumberFormatException e) {
            JsonUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid transaction ID format");
        } catch (Exception e) {
            System.err.println("[TransactionServlet] Error deleting transaction: " + e.getMessage());
            JsonUtil.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to delete transaction: " + e.getMessage());
        }
    }

    private String validateTransaction(Transaction t, boolean isUpdate) {
        if (t == null) {
            return "Transaction data payload cannot be null";
        }
        if (isUpdate && t.getId() <= 0) {
            return "Valid transaction ID is required for update";
        }
        if (t.getTitle() == null || t.getTitle().trim().isEmpty()) {
            return "Transaction description/title is required";
        }
        if (t.getTitle().length() > 150) {
            return "Transaction title exceeds maximum length of 150 characters";
        }
        if (t.getAmount() == null || t.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return "Amount must be a positive number greater than 0";
        }
        if (t.getType() == null || (!"INCOME".equalsIgnoreCase(t.getType()) && !"EXPENSE".equalsIgnoreCase(t.getType()))) {
            return "Transaction type must be either 'INCOME' or 'EXPENSE'";
        }
        if (t.getCategory() == null || t.getCategory().trim().isEmpty()) {
            return "Category is required";
        }
        if (t.getTransactionDate() == null || t.getTransactionDate().trim().isEmpty()) {
            return "Transaction date is required";
        }
        try {
            LocalDate.parse(t.getTransactionDate().trim());
        } catch (Exception e) {
            return "Transaction date must be in YYYY-MM-DD format";
        }
        return null;
    }
}
