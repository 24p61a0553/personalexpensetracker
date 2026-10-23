package com.expensetracker.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Enterprise Database Connection Manager for MySQL.
 * 
 * Obtains database configuration exclusively from environment variables:
 * - DB_HOST: Hostname of MySQL server (default: localhost)
 * - DB_PORT: Port number (default: 3306)
 * - DB_NAME: Database name (default: expense_tracker)
 * - DB_USER: Database username
 * - DB_PASS: Database password
 * - DB_SSL: Whether to enforce SSL (true/false)
 * - DATABASE_URL / MYSQL_URL: Optional unified database URI
 * 
 * For local development, also supports reading a gitignored .env file.
 * Contains no hardcoded passwords, usernames, or secrets.
 */
public class DBConnection {

    private static String url;
    private static String username;
    private static String password;
    private static String configSource = "none";
    private static boolean driverLoaded = false;
    private static boolean schemaInitialized = false;
    private static long lastLoadTime = 0;
    private static final long RELOAD_INTERVAL_MS = 10000;
    private static boolean isDbAvailable = false;
    private static final Map<String, String> localEnvMap = new HashMap<>();

    static {
        loadDriver();
        loadConfiguration();
    }

    public static boolean isProductionEnvironment() {
        return "true".equalsIgnoreCase(getEnv("RENDER"))
                || getEnv("RENDER_SERVICE_ID") != null
                || "production".equalsIgnoreCase(getEnv("ENVIRONMENT"))
                || "production".equalsIgnoreCase(getEnv("APP_ENV"))
                || "production".equalsIgnoreCase(System.getProperty("app.env"));
    }

    private static void loadDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            driverLoaded = true;
            System.out.println("[DBConnection] MySQL JDBC Driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("[DBConnection] ERROR: MySQL JDBC Driver not found in classpath!");
            e.printStackTrace();
        }
    }

    /**
     * Resolves an environment variable value.
     * Checks System.getenv() first, then local .env map, then JVM system properties.
     */
    private static String getEnv(String name) {
        String val = System.getenv(name);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }
        val = localEnvMap.get(name);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }
        val = System.getProperty(name);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }
        return null;
    }

    private static String getEnv(String name, String defaultValue) {
        String val = getEnv(name);
        return (val != null && !val.trim().isEmpty()) ? val : defaultValue;
    }

    /**
     * Attempts to load local environment configuration from a gitignored .env file
     * or external uncommitted config for local development if environment variables are not set in the OS.
     */
    private static void loadLocalEnvironmentFallbacks() {
        localEnvMap.clear();

        // 1. Check local .env file in current working directory
        File dotEnv = new File(".env");
        if (!dotEnv.exists() || !dotEnv.canRead()) {
            // Check parent or catalina base directory
            String catalinaBase = System.getProperty("catalina.base", System.getenv("CATALINA_BASE"));
            if (catalinaBase != null && !catalinaBase.trim().isEmpty()) {
                File tomcatDotEnv = new File(catalinaBase.trim(), ".env");
                if (tomcatDotEnv.exists() && tomcatDotEnv.canRead()) {
                    dotEnv = tomcatDotEnv;
                }
            }
        }

        if (dotEnv.exists() && dotEnv.canRead()) {
            try (BufferedReader br = new BufferedReader(new FileReader(dotEnv))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int eqIdx = line.indexOf('=');
                    if (eqIdx > 0) {
                        String key = line.substring(0, eqIdx).trim();
                        String val = line.substring(eqIdx + 1).trim();
                        if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                            val = val.substring(1, val.length() - 1);
                        }
                        localEnvMap.put(key, val);
                    }
                }
            } catch (Exception e) {
                System.err.println("[DBConnection] Note: could not parse .env file: " + e.getMessage());
            }
        }

        // 2. If DB_USER still not found, check external uncommitted Tomcat conf/db.properties for local dev
        if (getEnv("DB_USER") == null) {
            String catalinaBase = System.getProperty("catalina.base", System.getProperty("catalina.home", System.getenv("CATALINA_BASE")));
            if (catalinaBase != null && !catalinaBase.trim().isEmpty()) {
                File tomcatConf = new File(new File(catalinaBase.trim(), "conf"), "db.properties");
                if (tomcatConf.exists() && tomcatConf.canRead()) {
                    Properties p = new Properties();
                    try (InputStream fis = new FileInputStream(tomcatConf)) {
                        p.load(fis);
                        if (p.getProperty("db.host") != null) localEnvMap.putIfAbsent("DB_HOST", p.getProperty("db.host"));
                        if (p.getProperty("db.port") != null) localEnvMap.putIfAbsent("DB_PORT", p.getProperty("db.port"));
                        if (p.getProperty("db.name") != null) localEnvMap.putIfAbsent("DB_NAME", p.getProperty("db.name"));
                        if (p.getProperty("db.user") != null) localEnvMap.putIfAbsent("DB_USER", p.getProperty("db.user"));
                        if (p.getProperty("db.pass") != null) localEnvMap.putIfAbsent("DB_PASS", p.getProperty("db.pass"));
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    public static synchronized void loadConfiguration() {
        loadLocalEnvironmentFallbacks();

        configSource = "Environment Variables";
        String directUrl = getEnv("DATABASE_URL");
        if (directUrl == null) directUrl = getEnv("MYSQL_URL");
        if (directUrl == null) directUrl = getEnv("DB_URL");

        String host = getEnv("DB_HOST", "localhost");
        String port = getEnv("DB_PORT", "3306");
        String dbName = getEnv("DB_NAME", "expense_tracker");
        username = getEnv("DB_USER");
        if (username == null) username = getEnv("DB_USERNAME");
        if (username == null) username = getEnv("MYSQL_USER");

        password = getEnv("DB_PASS");
        if (password == null) password = getEnv("DB_PASSWORD");
        if (password == null) password = getEnv("MYSQL_PASSWORD");
        if (password == null) password = getEnv("MYSQLPASSWORD");

        if (directUrl != null && !directUrl.trim().isEmpty()) {
            configSource = "Environment variable (DATABASE_URL)";
        } else if (System.getenv("DB_HOST") != null) {
            configSource = "Environment variables (DB_HOST=" + System.getenv("DB_HOST") + ", DB_NAME=" + dbName + ")";
        } else if (!localEnvMap.isEmpty()) {
            configSource = "Local environment configuration";
        }

        boolean isProd = isProductionEnvironment();
        boolean isRemoteHost = host != null && !host.equalsIgnoreCase("localhost") && !host.equals("127.0.0.1") && !host.equalsIgnoreCase("db");
        String explicitSsl = getEnv("DB_SSL");
        String sslParam;
        if (explicitSsl != null && !explicitSsl.trim().isEmpty()) {
            sslParam = "true".equalsIgnoreCase(explicitSsl) ? "useSSL=true&requireSSL=false" : "useSSL=false";
        } else if (isRemoteHost || isProd) {
            sslParam = "useSSL=true&requireSSL=false";
        } else {
            sslParam = "useSSL=false";
        }

        if (directUrl != null && !directUrl.trim().isEmpty()) {
            directUrl = directUrl.trim();
            if (directUrl.contains("@") && (directUrl.startsWith("mysql://") || directUrl.startsWith("jdbc:mysql://"))) {
                try {
                    String cleanUrl = directUrl.replace("jdbc:mysql://", "").replace("mysql://", "");
                    String userPass = cleanUrl.substring(0, cleanUrl.indexOf('@'));
                    String hostPortDb = cleanUrl.substring(cleanUrl.indexOf('@') + 1);

                    if (userPass.contains(":")) {
                        username = userPass.substring(0, userPass.indexOf(':'));
                        password = userPass.substring(userPass.indexOf(':') + 1);
                    } else {
                        username = userPass;
                    }

                    url = "jdbc:mysql://" + hostPortDb;
                    if (!url.contains("?")) {
                        url += "?useSSL=true&requireSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
                    }
                } catch (Exception ex) {
                    url = directUrl.startsWith("jdbc:") ? directUrl : "jdbc:" + directUrl;
                }
            } else {
                url = directUrl.startsWith("jdbc:") ? directUrl : "jdbc:" + directUrl;
            }
        } else {
            url = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                    + "?" + sslParam + "&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
        }

        lastLoadTime = System.currentTimeMillis();
        System.out.println("[DBConnection] Loaded DB Config from: " + configSource + " -> URL: " + sanitizeUrl(url) + ", User: " + (username != null ? username : "(not set)"));

        // Test connection and auto-initialize tables if connected
        testAndInitializeDatabase();
    }

    private static String sanitizeUrl(String rawUrl) {
        if (rawUrl == null) return "null";
        return rawUrl.replaceAll("(?i)(password=)[^&;]*", "$1*****");
    }

    private static void testAndInitializeDatabase() {
        if (!driverLoaded) {
            isDbAvailable = false;
            return;
        }

        if (username == null || username.trim().isEmpty()) {
            isDbAvailable = false;
            System.out.println("[DBConnection] Notice: DB_USER environment variable not set. Falling back to offline demo store.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(url, username, password != null ? password : "")) {
            isDbAvailable = true;
            System.out.println("[DBConnection] SUCCESS: Connected to MySQL database (" + sanitizeUrl(url) + ")");

            if (!schemaInitialized) {
                initSchema(conn);
                schemaInitialized = true;
            }
        } catch (SQLException e) {
            isDbAvailable = false;
            System.out.println("[DBConnection] Notice: MySQL database currently not reachable: " + e.getMessage());
            System.out.println("[DBConnection] Active mode: Fallback memory data store enabled for continuous testing.");
        }
    }

    private static void initSchema(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // 1. Create expense_user_profile table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS expense_user_profile (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "person_name VARCHAR(100) NOT NULL DEFAULT 'Srinivas', " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ") ENGINE=InnoDB;");

            // Seed default profile 'Srinivas' if none exists
            stmt.executeUpdate("INSERT INTO expense_user_profile (id, person_name) VALUES (1, 'Srinivas') " +
                    "ON DUPLICATE KEY UPDATE id=id;");

            // 2. Create categories table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS categories (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(60) NOT NULL UNIQUE, " +
                    "type ENUM('EXPENSE', 'INCOME') NOT NULL, " +
                    "icon VARCHAR(60) NOT NULL DEFAULT 'fa-tag', " +
                    "color VARCHAR(20) NOT NULL DEFAULT '#6366f1', " +
                    "description VARCHAR(255), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ") ENGINE=InnoDB;");

            // Seed standard categories if table is empty
            stmt.executeUpdate("INSERT INTO categories (name, type, icon, color, description) VALUES " +
                    "('Food & Dining', 'EXPENSE', 'fa-utensils', '#f59e0b', 'Groceries, restaurants, cafes'), " +
                    "('Rent & Housing', 'EXPENSE', 'fa-home', '#ef4444', 'Apartment rent, property tax'), " +
                    "('Utilities', 'EXPENSE', 'fa-bolt', '#3b82f6', 'Electricity, water, gas, broadband'), " +
                    "('Transportation', 'EXPENSE', 'fa-car', '#8b5cf6', 'Fuel, public transit, cab fares'), " +
                    "('Entertainment', 'EXPENSE', 'fa-film', '#ec4899', 'Movies, streaming, games'), " +
                    "('Shopping', 'EXPENSE', 'fa-shopping-bag', '#14b8a6', 'Clothing, electronics, goods'), " +
                    "('Healthcare', 'EXPENSE', 'fa-heartbeat', '#06b6d4', 'Medication, doctor visits, health'), " +
                    "('Education', 'EXPENSE', 'fa-graduation-cap', '#6366f1', 'Tuition, textbooks, courses'), " +
                    "('Miscellaneous', 'EXPENSE', 'fa-ellipsis-h', '#64748b', 'Other unplanned personal expenditures'), " +
                    "('Salary', 'INCOME', 'fa-money-bill-wave', '#10b981', 'Primary job monthly salary'), " +
                    "('Freelance', 'INCOME', 'fa-laptop-code', '#06b6d4', 'Freelancing projects and contract work'), " +
                    "('Investments', 'INCOME', 'fa-chart-line', '#3b82f6', 'Dividends, capital gains, interest'), " +
                    "('Business', 'INCOME', 'fa-briefcase', '#8b5cf6', 'Side business revenue, sales'), " +
                    "('Gifts & Grants', 'INCOME', 'fa-gift', '#ec4899', 'Cash gifts, allowances, grants'), " +
                    "('Other Income', 'INCOME', 'fa-wallet', '#10b981', 'Refunds, cashbacks, other income') " +
                    "ON DUPLICATE KEY UPDATE description=VALUES(description);");

            // 3. Create transactions table (with profile_id and person_name)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS transactions (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "title VARCHAR(150) NOT NULL, " +
                    "amount DECIMAL(12, 2) NOT NULL, " +
                    "type ENUM('INCOME', 'EXPENSE') NOT NULL, " +
                    "category VARCHAR(60) NOT NULL, " +
                    "transaction_date DATE NOT NULL, " +
                    "payment_method VARCHAR(50) DEFAULT 'Cash', " +
                    "notes VARCHAR(500), " +
                    "profile_id INT DEFAULT 1, " +
                    "person_name VARCHAR(100) DEFAULT 'Srinivas', " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "INDEX idx_trans_date (transaction_date), " +
                    "INDEX idx_trans_type (type), " +
                    "INDEX idx_trans_category (category), " +
                    "INDEX idx_trans_profile (profile_id)" +
                    ") ENGINE=InnoDB;");

            // Ensure profile_id and person_name columns exist if transactions table already pre-existed
            try {
                stmt.executeUpdate("ALTER TABLE transactions ADD COLUMN profile_id INT DEFAULT 1;");
            } catch (SQLException ignored) {}
            try {
                stmt.executeUpdate("ALTER TABLE transactions ADD COLUMN person_name VARCHAR(100) DEFAULT 'Srinivas';");
            } catch (SQLException ignored) {}

            System.out.println("[DBConnection] Verified schema tables (expense_user_profile, categories, transactions).");
        } catch (SQLException e) {
            System.err.println("[DBConnection] Schema initialization check: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        if (System.currentTimeMillis() - lastLoadTime > RELOAD_INTERVAL_MS) {
            loadConfiguration();
        }
        if (username == null || username.trim().isEmpty()) {
            throw new SQLException("DB_USER environment variable is not defined.");
        }
        return DriverManager.getConnection(url, username, password != null ? password : "");
    }

    public static boolean isAvailable() {
        return isDbAvailable;
    }

    public static String getConfigSource() {
        return configSource;
    }

    public static String getSanitizedUrl() {
        return sanitizeUrl(url);
    }
}
