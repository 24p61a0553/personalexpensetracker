package com.expensetracker.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Utility for converting Java objects to/from JSON and sending structured HTTP responses.
 */
public class JsonUtil {

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .serializeNulls()
            .create();

    public static Gson getGson() {
        return gson;
    }

    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }

    public static <T> T readRequestBody(HttpServletRequest request, Class<T> classOfT) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return fromJson(sb.toString(), classOfT);
    }

    public static void sendJsonResponse(HttpServletResponse response, int statusCode, Object data) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(statusCode);
        try (PrintWriter out = response.getWriter()) {
            out.print(gson.toJson(data));
            out.flush();
        }
    }

    public static void sendSuccess(HttpServletResponse response, String message, Object payload) throws IOException {
        JsonObject obj = new JsonObject();
        obj.addProperty("success", true);
        obj.addProperty("message", message);
        if (payload != null) {
            obj.add("data", gson.toJsonTree(payload));
        }
        sendJsonResponse(response, HttpServletResponse.SC_OK, obj);
    }

    public static void sendError(HttpServletResponse response, int statusCode, String message) throws IOException {
        JsonObject obj = new JsonObject();
        obj.addProperty("success", false);
        obj.addProperty("message", sanitizeErrorMessage(message));
        sendJsonResponse(response, statusCode, obj);
    }

    private static String sanitizeErrorMessage(String raw) {
        if (raw == null) return "An unexpected error occurred.";
        return raw.replaceAll("(?i)(password=)[^&;\\s]*", "$1*****")
                  .replaceAll("(?i)(user=)[^&;\\s]*", "$1*****")
                  .replaceAll("(?i)(DB_PASS)[=:][^&;\\s]*", "$1=*****")
                  .replaceAll("(?i)(DB_USER)[=:][^&;\\s]*", "$1=*****");
    }
}
