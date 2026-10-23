package com.expensetracker.util;

/**
 * Centralized Application Mode & Configuration Manager.
 * 
 * Supports two distinct, strictly isolated operational modes:
 *  - APP_MODE=demo     (Zero external database dependency, thread-safe in-memory store)
 *  - APP_MODE=database (Enterprise MySQL database accessed via JDBC)
 * 
 * Mode is resolved in priority order:
 *  1. Environment variable: APP_MODE
 *  2. JVM System property: app.mode
 *  3. Fallback: If database credentials (DB_USER or DATABASE_URL) are detected -> DATABASE,
 *     otherwise defaults safely to DEMO.
 * 
 * Invalid APP_MODE values produce an explicit configuration error rather than
 * silent fallback. In DATABASE mode, database failures produce genuine errors
 * and never silently switch into demo mode.
 */
public class AppConfig {

    public enum Mode {
        DEMO,
        DATABASE
    }

    private static Mode activeMode = null;
    private static String configError = null;

    static {
        determineMode();
    }

    public static synchronized void determineMode() {
        configError = null;
        String raw = resolveRawMode();

        if (raw != null && !raw.trim().isEmpty()) {
            String clean = raw.trim().toLowerCase();
            if ("demo".equals(clean)) {
                activeMode = Mode.DEMO;
                System.out.println("[AppConfig] Operational Mode: DEMO (In-memory store, zero database dependency)");
            } else if ("database".equals(clean) || "db".equals(clean)) {
                activeMode = Mode.DATABASE;
                System.out.println("[AppConfig] Operational Mode: DATABASE (MySQL JDBC persistence)");
            } else {
                configError = "Invalid APP_MODE configuration: '" + raw + "'. Supported modes are 'demo' or 'database'.";
                System.err.println("[AppConfig] FATAL: " + configError);
                throw new IllegalStateException(configError);
            }
        } else {
            // Mode not explicitly specified - inspect environment for database credentials
            if (DBConnection.hasDatabaseCredentials()) {
                activeMode = Mode.DATABASE;
                System.out.println("[AppConfig] APP_MODE not specified. Database credentials detected -> Defaulting to DATABASE mode");
            } else {
                activeMode = Mode.DEMO;
                System.out.println("[AppConfig] APP_MODE not specified. No database credentials -> Defaulting to DEMO mode");
            }
        }
    }

    private static String resolveRawMode() {
        String val = System.getenv("APP_MODE");
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }
        val = System.getProperty("app.mode");
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }
        return DBConnection.getEnvValue("APP_MODE");
    }

    public static Mode getMode() {
        if (activeMode == null) {
            determineMode();
        }
        return activeMode;
    }

    public static boolean isDemoMode() {
        return getMode() == Mode.DEMO;
    }

    public static boolean isDatabaseMode() {
        return getMode() == Mode.DATABASE;
    }

    public static String getModeName() {
        return getMode().name().toLowerCase();
    }

    public static String getConfigError() {
        return configError;
    }

    /**
     * For unit and integration testing: explicitly sets the operational mode.
     */
    public static synchronized void setModeForTesting(Mode mode) {
        activeMode = mode;
    }
}
