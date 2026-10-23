package com.expensetracker.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Enterprise Database Connection Manager for MySQL.
 * 
 * Supports flexible configuration loading in priority order:
 * 1. JVM System properties (-Ddb.pass=..., -Ddb.user=..., -Ddb.config.file=...)
 * 2. Environment variables (DATABASE_URL, DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASS)
 * 3. External Tomcat configuration file ($CATALINA_BASE/conf/db.properties)
 * 4. External working directory configuration file (./db.properties)
 * 5. Classpath configuration file (WEB-INF/classes/db.properties)
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

    static {
        loadDriver();
        loadConfiguration();
    }

    public static boolean isProductionEnvironment() {
        return "true".equalsIgnoreCase(System.getenv("RENDER"))
                || System.getenv("RENDER_SERVICE_ID") != null
                || "production".equalsIgnoreCase(System.getenv("ENVIRONMENT"))
                || "production".equalsIgnoreCase(System.getenv("APP_ENV"))
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

    public static synchronized void loadConfiguration() {
        Properties props = new Properties();
        configSource = "defaults";

        // 1. JVM system property -Ddb.config.file=...
        String customConfigPath = System.getProperty("db.config.file");
        boolean loaded = false;

        if (customConfigPath != null && !customConfigPath.trim().isEmpty()) {
            File customFile = new File(customConfigPath.trim());
            if (customFile.exists() && customFile.canRead()) {
                try (InputStream fis = new FileInputStream(customFile)) {
                    props.load(fis);
                    configSource = "system property file (" + customFile.getAbsolutePath() + ")";
                    loaded = true;
                } catch (Exception e) {
                    System.err.println("[DBConnection] Could not read custom config file: " + e.getMessage());
                }
            }
        }

        // 2. Tomcat conf/db.properties
        if (!loaded) {
            String catalinaBase = System.getProperty("catalina.base", System.getProperty("catalina.home", System.getenv("CATALINA_BASE")));
            if (catalinaBase != null && !catalinaBase.trim().isEmpty()) {
                File tomcatConf = new File(new File(catalinaBase.trim(), "conf"), "db.properties");
                if (tomcatConf.exists() && tomcatConf.canRead()) {
                    try (InputStream fis = new FileInputStream(tomcatConf)) {
                        props.load(fis);
                        configSource = "Tomcat conf (" + tomcatConf.getAbsolutePath() + ")";
                        loaded = true;
                    } catch (Exception e) {
                        System.err.println("[DBConnection] Could not read Tomcat conf/db.properties: " + e.getMessage());
                    }
                }
            }
        }

        // 3. Current working directory ./db.properties
        if (!loaded) {
            File currentDirFile = new File("db.properties");
            if (currentDirFile.exists() && currentDirFile.canRead()) {
                try (InputStream fis = new FileInputStream(currentDirFile)) {
                    props.load(fis);
                    configSource = "current directory (" + currentDirFile.getAbsolutePath() + ")";
                    loaded = true;
                } catch (Exception e) {
                    System.err.println("[DBConnection] Could not read current dir db.properties: " + e.getMessage());
                }
            }
        }

        // 4. Classpath WEB-INF/classes/db.properties
        if (!loaded) {
            try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (in != null) {
                    props.load(in);
                    configSource = "classpath (WEB-INF/classes/db.properties)";
                    loaded = true;
                }
            } catch (Exception e) {
                System.err.println("[DBConnection] Notice: db.properties not found in classpath.");
            }
        }

        boolean isProd = isProductionEnvironment();

        // 1. Detect environment-based configuration priority
        if (System.getenv("DATABASE_URL") != null && !System.getenv("DATABASE_URL").trim().isEmpty()) {
            configSource = "Environment variable (DATABASE_URL)";
        } else if (System.getenv("MYSQL_URL") != null && !System.getenv("MYSQL_URL").trim().isEmpty()) {
            configSource = "Environment variable (MYSQL_URL)";
        } else if (System.getenv("DB_HOST") != null && !System.getenv("DB_HOST").trim().isEmpty()) {
            String envDbName = System.getenv("DB_NAME") != null ? System.getenv("DB_NAME") : "expense_tracker";
            configSource = "Environment variables (DB_HOST=" + System.getenv("DB_HOST") + ", DB_NAME=" + envDbName + ")";
        }

        // Check for direct database URL (e.g. DATABASE_URL, DB_URL, MYSQL_URL)
        String directUrl = resolveValue(
                new String[]{"db.url", "jdbc.url", "DB_URL", "DATABASE_URL"},
                new String[]{"DATABASE_URL", "DB_URL", "MYSQL_URL", "JDBC_DATABASE_URL"},
                props,
                new String[]{"db.url", "jdbc.url", "url"},
                null
        );

        String host = resolveValue(
                new String[]{"db.host", "mysql.host", "DB_HOST", "MYSQL_HOST"},
                new String[]{"DB_HOST", "MYSQL_HOST", "MYSQLHOST", "DATABASE_HOST"},
                props,
                new String[]{"db.host", "mysql.host", "host"},
                "localhost"
        );

        String port = resolveValue(
                new String[]{"db.port", "mysql.port", "DB_PORT", "MYSQL_PORT"},
                new String[]{"DB_PORT", "MYSQL_PORT", "MYSQLPORT"},
                props,
                new String[]{"db.port", "mysql.port", "port"},
                "3306"
        );

        String dbName = resolveValue(
                new String[]{"db.name", "db.database", "mysql.database", "DB_NAME"},
                new String[]{"DB_NAME", "DB_DATABASE", "MYSQL_DATABASE"},
                props,
                new String[]{"db.name", "db.database", "database"},
                "expense_tracker"
        );

        username = resolveValue(
                new String[]{"db.user", "db.username", "mysql.user", "DB_USER"},
                new String[]{"DB_USER", "DB_USERNAME", "MYSQL_USER"},
                props,
                new String[]{"db.user", "db.username", "username", "user"},
                "root"
        );

        password = resolvePassword(props);

        boolean isRemoteHost = host != null && !host.equalsIgnoreCase("localhost") && !host.equals("127.0.0.1") && !host.equalsIgnoreCase("db");
        String explicitSsl = System.getenv("DB_SSL");
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
            url = "jdbc:mysql://" + (host != null ? host : "localhost") + ":" + port + "/" + dbName
                    + "?" + sslParam + "&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
        }

        lastLoadTime = System.currentTimeMillis();
        System.out.println("[DBConnection] Loaded DB Config from: " + configSource + " -> URL: " + sanitizeUrl(url) + ", User: " + username);

        // Test connection and auto-initialize tables if connected
        testAndInitializeDatabase();
    }

    private static String resolveValue(String[] sysKeys, String[] envKeys, Properties props, String[] propKeys, String defaultVal) {
        for (String k : sysKeys) {
            String val = System.getProperty(k);
            if (val != null && !val.trim().isEmpty()) return val.trim();
        }
        for (String k : envKeys) {
            String val = System.getenv(k);
            if (val != null && !val.trim().isEmpty()) return val.trim();
        }
        for (String k : propKeys) {
            String val = props.getProperty(k);
            if (val != null && !val.trim().isEmpty()) return val.trim();
        }
        return defaultVal;
    }

    private static String resolvePassword(Properties props) {
        String[] sysKeys = {"db.pass", "db.password", "mysql.password", "DB_PASS", "DB_PASSWORD"};
        for (String k : sysKeys) {
            String val = System.getProperty(k);
            if (val != null) return val;
        }

        String[] envKeys = {"DB_PASS", "DB_PASSWORD", "MYSQL_PASSWORD", "MYSQLPASSWORD", "DATABASE_PASSWORD"};
        for (String k : envKeys) {
            String val = System.getenv(k);
            if (val != null) return val;
        }

        String[] propKeys = {"db.pass", "db.password", "password", "pass"};
        for (String k : propKeys) {
            if (props.containsKey(k)) {
                return props.getProperty(k);
            }
        }
        return "";
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

        try (Connection conn = DriverManager.getConnection(url, username, password != null ? password : "")) {
            isDbAvailable = true;
            System.out.println("[DBConnection] SUCCESS: Connected to MySQL database (" + url + ")");

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
