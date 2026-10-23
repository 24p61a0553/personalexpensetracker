-- =====================================================================
-- Personal Expense Tracker - Database Schema & Initial Data
-- Database Name: expense_tracker
-- Compatibility: MySQL 8.0+
-- =====================================================================

CREATE DATABASE IF NOT EXISTS expense_tracker
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE expense_tracker;

-- 1. User Profile Table (for Person Name persistence)
CREATE TABLE IF NOT EXISTS expense_user_profile (
    id INT AUTO_INCREMENT PRIMARY KEY,
    person_name VARCHAR(100) NOT NULL DEFAULT 'Srinivas',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

INSERT INTO expense_user_profile (id, person_name) VALUES (1, 'Srinivas')
ON DUPLICATE KEY UPDATE person_name = VALUES(person_name);

-- 2. Categories Master Table
CREATE TABLE IF NOT EXISTS categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(60) NOT NULL UNIQUE,
    type ENUM('EXPENSE', 'INCOME') NOT NULL,
    icon VARCHAR(60) NOT NULL DEFAULT 'fa-tag',
    color VARCHAR(20) NOT NULL DEFAULT '#6366f1',
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 3. Transactions Table (associated with Person Profile)
CREATE TABLE IF NOT EXISTS transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    type ENUM('INCOME', 'EXPENSE') NOT NULL,
    category VARCHAR(60) NOT NULL,
    transaction_date DATE NOT NULL,
    payment_method VARCHAR(50) DEFAULT 'Cash',
    notes VARCHAR(500),
    profile_id INT DEFAULT 1,
    person_name VARCHAR(100) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_trans_date (transaction_date),
    INDEX idx_trans_type (type),
    INDEX idx_trans_category (category),
    INDEX idx_trans_profile (profile_id)
) ENGINE=InnoDB;

-- =====================================================================
-- Seed Data: Standard Expense & Income Categories
-- =====================================================================
INSERT INTO categories (name, type, icon, color, description) VALUES
-- Expense Categories
('Food & Dining', 'EXPENSE', 'fa-utensils', '#f59e0b', 'Groceries, restaurants, cafes, food delivery'),
('Rent & Housing', 'EXPENSE', 'fa-home', '#ef4444', 'Apartment rent, property tax, maintenance'),
('Utilities', 'EXPENSE', 'fa-bolt', '#3b82f6', 'Electricity, water, gas, home internet, mobile plan'),
('Transportation', 'EXPENSE', 'fa-car', '#8b5cf6', 'Fuel, public transit, cab fares, car maintenance'),
('Entertainment', 'EXPENSE', 'fa-film', '#ec4899', 'Movies, concerts, gaming, streaming subscriptions'),
('Shopping', 'EXPENSE', 'fa-shopping-bag', '#14b8a6', 'Clothing, electronics, personal accessories'),
('Healthcare', 'EXPENSE', 'fa-heartbeat', '#06b6d4', 'Medication, doctor visits, health insurance, fitness'),
('Education', 'EXPENSE', 'fa-graduation-cap', '#6366f1', 'Tuition, textbooks, online courses, software tools'),
('Miscellaneous', 'EXPENSE', 'fa-ellipsis-h', '#64748b', 'Other unplanned personal expenditures'),

-- Income Categories
('Salary', 'INCOME', 'fa-money-bill-wave', '#10b981', 'Primary job monthly salary or wage'),
('Freelance', 'INCOME', 'fa-laptop-code', '#06b6d4', 'Freelancing projects and contract work'),
('Investments', 'INCOME', 'fa-chart-line', '#3b82f6', 'Dividends, capital gains, interest returns'),
('Business', 'INCOME', 'fa-briefcase', '#8b5cf6', 'Side business revenue, product sales'),
('Gifts & Grants', 'INCOME', 'fa-gift', '#ec4899', 'Cash gifts, allowances, educational grants'),
('Other Income', 'INCOME', 'fa-wallet', '#10b981', 'Refunds, cashbacks, other miscellaneous income')
ON DUPLICATE KEY UPDATE description=VALUES(description);

-- =====================================================================
-- Seed Data: Realistic Initial Transactions for Instant Dashboard Analytics
-- =====================================================================
INSERT INTO transactions (title, amount, type, category, transaction_date, payment_method, notes) VALUES
-- Income entries
('Monthly Primary Salary', 4850.00, 'INCOME', 'Salary', '2026-10-01', 'Bank Transfer', 'Direct corporate payroll deposit'),
('Freelance Web App Development', 950.00, 'INCOME', 'Freelance', '2026-10-08', 'PayPal', 'Client website UI redesign contract'),
('Quarterly Stock Dividends', 145.50, 'INCOME', 'Investments', '2026-10-12', 'Direct Deposit', 'Index fund dividend payout'),
('Mobile App Consulting Session', 300.00, 'INCOME', 'Business', '2026-10-18', 'Bank Transfer', 'Tech architecture advisory session'),
('Credit Card Cashback Reward', 42.80, 'INCOME', 'Other Income', '2026-10-21', 'Credit Card', 'Monthly cash reward rebate'),

-- September Incomes (for trend analysis)
('Monthly Primary Salary (Sept)', 4850.00, 'INCOME', 'Salary', '2026-09-01', 'Bank Transfer', 'Payroll deposit September'),
('Freelance Dashboard API Project', 800.00, 'INCOME', 'Freelance', '2026-09-15', 'PayPal', 'REST API microservice integration'),

-- August Incomes (for trend analysis)
('Monthly Primary Salary (Aug)', 4850.00, 'INCOME', 'Salary', '2026-08-01', 'Bank Transfer', 'Payroll deposit August'),
('Stock Portfolio Dividend', 120.00, 'INCOME', 'Investments', '2026-08-14', 'Direct Deposit', 'August dividend distribution'),

-- Current Month Expenses (October 2026)
('Monthly Apartment Rent', 1250.00, 'EXPENSE', 'Rent & Housing', '2026-10-02', 'Bank Transfer', 'October residential lease payment'),
('Whole Foods Grocery Mart', 194.30, 'EXPENSE', 'Food & Dining', '2026-10-04', 'Debit Card', 'Weekly household groceries'),
('High-Speed Fiber Internet', 65.00, 'EXPENSE', 'Utilities', '2026-10-06', 'Credit Card', '1 Gbps monthly broadband subscription'),
('Gas Station Fuel Refill', 54.00, 'EXPENSE', 'Transportation', '2026-10-07', 'Credit Card', 'Vehicle unleaded fuel refill'),
('Dining at Italian Bistro', 78.50, 'EXPENSE', 'Food & Dining', '2026-10-10', 'Credit Card', 'Dinner with college friends'),
('Electric & Municipal Water Utility', 118.25, 'EXPENSE', 'Utilities', '2026-10-11', 'Debit Card', 'City electricity and water bill'),
('Gym Membership Monthly Fee', 45.00, 'EXPENSE', 'Healthcare', '2026-10-13', 'Credit Card', 'Fitness center monthly membership'),
('Pharmacy & Health Vitamins', 32.60, 'EXPENSE', 'Healthcare', '2026-10-15', 'Debit Card', 'Daily multivitamin supplements'),
('Streaming Video & Music Services', 28.98, 'EXPENSE', 'Entertainment', '2026-10-16', 'Credit Card', 'Netflix & Spotify subscriptions'),
('Weekend Farmers Market', 62.40, 'EXPENSE', 'Food & Dining', '2026-10-18', 'Cash', 'Fresh produce, eggs, fruits'),
('Online Cloud Certification Course', 49.99, 'EXPENSE', 'Education', '2026-10-19', 'Credit Card', 'AWS Solutions Architect prep course'),
('Autumn Clothing & Apparel', 129.50, 'EXPENSE', 'Shopping', '2026-10-20', 'Credit Card', 'Warm winter jackets and apparel'),
('Subway & Public Transit Pass', 35.00, 'EXPENSE', 'Transportation', '2026-10-22', 'Debit Card', 'Monthly commuter transit card'),

-- September Expenses (for trend analysis)
('Monthly Apartment Rent (Sept)', 1250.00, 'EXPENSE', 'Rent & Housing', '2026-09-02', 'Bank Transfer', 'September residential lease'),
('Supermarket Groceries (Sept)', 380.00, 'EXPENSE', 'Food & Dining', '2026-09-12', 'Debit Card', 'September total groceries'),
('Utilities Electricity & Gas (Sept)', 135.00, 'EXPENSE', 'Utilities', '2026-09-14', 'Debit Card', 'September utility bills'),
('Transit & Transportation (Sept)', 110.00, 'EXPENSE', 'Transportation', '2026-09-20', 'Credit Card', 'Fuel & Metro transit'),
('Books & Technical Manuals', 75.00, 'EXPENSE', 'Education', '2026-09-22', 'Credit Card', 'Software architecture textbooks'),

-- August Expenses (for trend analysis)
('Monthly Apartment Rent (Aug)', 1250.00, 'EXPENSE', 'Rent & Housing', '2026-08-02', 'Bank Transfer', 'August residential lease'),
('Supermarket Groceries (Aug)', 395.00, 'EXPENSE', 'Food & Dining', '2026-08-11', 'Debit Card', 'August total groceries'),
('Utilities Electricity & Gas (Aug)', 142.00, 'EXPENSE', 'Utilities', '2026-08-15', 'Debit Card', 'August electricity bill'),
('Concert & Summer Entertainment', 120.00, 'EXPENSE', 'Entertainment', '2026-08-25', 'Credit Card', 'Summer music concert ticket');
