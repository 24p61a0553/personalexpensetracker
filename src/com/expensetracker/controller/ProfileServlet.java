package com.expensetracker.controller;

import com.expensetracker.dao.ProfileDAO;
import com.expensetracker.model.UserProfile;
import com.expensetracker.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Servlet handling fetching and updating the account holder / person name.
 * Persists data to MySQL database via ProfileDAO.
 */
public class ProfileServlet extends HttpServlet {

    private final ProfileDAO profileDAO = new ProfileDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            UserProfile profile = profileDAO.getProfile();
            JsonUtil.sendSuccess(response, "Profile retrieved successfully", profile);
        } catch (Exception e) {
            System.err.println("[ProfileServlet] Error getting profile: " + e.getMessage());
            JsonUtil.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to retrieve profile: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleSaveProfile(request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleSaveProfile(request, response);
    }

    private void handleSaveProfile(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            UserProfile input = JsonUtil.readRequestBody(request, UserProfile.class);
            if (input == null || input.getPersonName() == null || input.getPersonName().trim().isEmpty()) {
                JsonUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Person name cannot be blank. Please enter a valid name.");
                return;
            }

            String name = input.getPersonName().trim();
            if (name.length() > 100) {
                JsonUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Person name cannot exceed 100 characters.");
                return;
            }

            boolean saved = profileDAO.saveProfile(name);
            if (saved) {
                UserProfile updated = profileDAO.getProfile();
                JsonUtil.sendSuccess(response, "Person name saved and updated successfully!", updated);
            } else {
                JsonUtil.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Could not save person name to the database.");
            }
        } catch (IllegalArgumentException iae) {
            JsonUtil.sendError(response, HttpServletResponse.SC_BAD_REQUEST, iae.getMessage());
        } catch (SQLException sqle) {
            System.err.println("[ProfileServlet] Database error saving profile: " + sqle.getMessage());
            JsonUtil.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Database error while saving profile: " + sqle.getMessage());
        } catch (Exception e) {
            System.err.println("[ProfileServlet] Unexpected error: " + e.getMessage());
            JsonUtil.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to save profile: " + e.getMessage());
        }
    }
}
