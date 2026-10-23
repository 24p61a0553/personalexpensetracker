# Personal Expense Tracker 💰📊

A modern, responsive, and full-featured **Personal Expense & Income Tracker** web application built with **Java Servlets**, **JDBC**, **MySQL**, **HTML5**, **CSS3**, and **JavaScript**, running on **Apache Tomcat 9** and packaged with **Apache Ant**. Fully configured for immediate cloud deployment on **Render** via Docker.

---

## 🌟 Application Features

1. **Direct Access Dashboard (No Login or Registration)**
   - Opens straight to your financial overview without authentication barriers.
   - Live KPI cards: **Total Income**, **Total Expenses**, **Remaining Balance**, and **This Month's Spending**.
   - Real-time Savings Rate percentage calculator.

2. **Interactive Visual Analytics (Chart.js)**
   - **Category-wise Expense Chart**: Interactive doughnut chart breaking down spending by category.
   - **Monthly Cashflow Trends**: Side-by-side bar chart comparing Income vs. Expenses over time with a Net Savings trajectory.
   - **Income Distribution**: Breakdown of revenue by income streams.

3. **Complete Expense Management**
   - Record new expenses with Amount, Category, Date, Payment Method, and Notes.
   - Edit any existing expense in an intuitive modal.
   - Delete expenses with a secure confirmation prompt.
   - Search by description and filter by category or date ranges (Today, This Month, Last 30 Days, Custom).
   - Export expenses to **CSV**.

4. **Comprehensive Income Management**
   - Record income inflows (Salary, Freelance, Investments, Business, Gifts, etc.).
   - Edit and delete income records.
   - Filter and search income history.
   - Export income data to **CSV**.

5. **Financial Performance Statements & Reports**
   - Monthly performance table with calculated surplus/deficit badges.
   - Average monthly spending benchmark.
   - Print-ready statements and CSV reporting.

6. **Enterprise Architecture**
   - **MVC Pattern**: Model classes (`Transaction`, `Category`, `DashboardSummary`), Data Access Objects (`TransactionDAO`, `CategoryDAO`), and Controllers (`TransactionServlet`, `DashboardServlet`, `ReportServlet`, `CategoryServlet`).
   - **SQL Injection Prevention**: All SQL queries utilize `PreparedStatement`.
   - **Monetary Precision**: Built using `DECIMAL(12, 2)` and Java `BigDecimal` to prevent floating-point rounding errors.
   - **Resilient Database Layer**: Live MySQL database persistence with automatic schema initialization and an offline test fallback.

---

## 🏗️ Project Architecture & Directory Structure

```
PERSONALEXPENSETRACKER/
├── build.xml                    # Apache Ant build script (clean, init, compile, war, deploy)
├── build.properties             # Compiler flags and Tomcat deployment paths
├── build.bat                    # One-click Ant build for Windows
├── build.ps1                    # PowerShell build & package script
├── Dockerfile                   # Multi-stage build for Render cloud deployment
├── docker-compose.yml           # Docker Compose for Tomcat + MySQL
├── .gitignore                   # Git ignore for build artifacts and credentials
├── README.md                    # Project documentation
├── setup_db.bat                 # Automated MySQL schema & seed data setup script
├── setup_db.ps1                 # PowerShell MySQL setup script
│
├── database/
│   ├── database.sql             # Complete consolidated SQL schema and seed data
│   ├── schema.sql               # Database schema definition
│   └── seed_data.sql            # Realistic initial transactions
│
├── lib/                         # Build-time dependencies
│   ├── javax.servlet-api-4.0.1.jar
│   ├── mysql-connector-j-8.3.0.jar
│   └── gson-2.10.1.jar
│
├── src/
│   ├── db.properties            # Classpath database properties (overridable)
│   └── com/expensetracker/
│       ├── controller/
│       │   ├── DashboardServlet.java    # /api/dashboard
│       │   ├── TransactionServlet.java  # /api/transactions
│       │   ├── ReportServlet.java       # /api/reports
│       │   └── CategoryServlet.java     # /api/categories
│       ├── dao/
│       │   ├── TransactionDAO.java      # JDBC transaction CRUD & summaries
│       │   └── CategoryDAO.java         # JDBC category queries
│       ├── filter/
│       │   └── CorsAndEncodingFilter.java # UTF-8 and CORS filter
│       ├── model/
│       │   ├── Transaction.java         # Transaction POJO (BigDecimal amount)
│       │   ├── Category.java            # Category POJO
│       │   ├── DashboardSummary.java    # Dashboard DTO
│       │   ├── CategorySummary.java     # Aggregated category metrics
│       │   ├── MonthlySummary.java      # Aggregated monthly cashflow
│       │   └── ReportData.java          # Analytics report DTO
│       └── util/
│           ├── DBConnection.java        # Enterprise connection manager
│           ├── JsonUtil.java            # Gson JSON serializer
│           └── DemoDataStore.java       # Offline fallback store
│
└── WebContent/                  # Web application root
    ├── index.html               # Main Dashboard (direct entry, no login)
    ├── expenses.html            # Expenses management interface
    ├── income.html              # Income management interface
    ├── reports.html             # Financial reports & analytics
    ├── WEB-INF/
    │   ├── web.xml              # Servlet mappings & web config
    │   └── lib/                 # Runtime JARs bundled in WAR
    │       ├── mysql-connector-j-8.3.0.jar
    │       └── gson-2.10.1.jar
    └── assets/
        ├── css/
        │   ├── global.css       # Design system & dark theme tokens
        │   ├── dashboard.css    # Dashboard & charts styling
        │   ├── transactions.css # Filter bar & tables styling
        │   └── reports.css      # Analytics layout styling
        └── js/
            ├── api.js           # API client & currency/date formatters
            ├── dashboard.js     # Dashboard state & charts
            ├── expenses.js      # Expense CRUD & CSV exporter
            ├── income.js        # Income CRUD & CSV exporter
            └── reports.js       # Reports & trends visualizer
```

---

## ⚙️ Prerequisites

Before running the application locally, ensure you have:
1. **Java Development Kit (JDK 8 or 17+)**
   - Check with: `java -version` and `javac -version`
2. **Apache Ant (1.10+)**
   - Check with: `ant -version`
3. **Apache Tomcat 9**
   - Installed at `C:\apache-tomcat-9` (or update `tomcat.home` in `build.properties`)
4. **MySQL Server 8.0+**
   - Running locally on port `3306`

---

## 🚀 Step-by-Step Local Setup & Execution

### Stage 1: Initialize the MySQL Database

1. Open **Command Prompt** or **PowerShell** in the project directory.
2. Run the automated database setup script:
   ```cmd
   setup_db.bat
   ```
   *(Or in PowerShell: `.\setup_db.ps1`)*
3. Enter your MySQL `root` password when prompted.
4. The script creates the `expense_tracker` database, sets up `categories` and `transactions` tables, seeds sample data, and automatically updates Tomcat's `conf\db.properties`.

> **Manual Setup via MySQL Workbench:**
> If you prefer MySQL Workbench:
> - Open `database/database.sql`
> - Execute the entire script.

---

### Stage 2: Configure Database Credentials (No Hardcoding)

Credentials can be supplied in **any** of the following ways (in priority order):

1. **External Tomcat Configuration (Recommended for Local Dev):**
   Create or edit `C:\apache-tomcat-9\conf\db.properties`:
   ```properties
   db.host=localhost
   db.port=3306
   db.name=expense_tracker
   db.user=root
   db.pass=YourMySQLPassword
   ```
2. **Environment Variables:**
   ```cmd
   set DB_HOST=localhost
   set DB_PORT=3306
   set DB_NAME=expense_tracker
   set DB_USER=root
   set DB_PASS=YourMySQLPassword
   ```
   Or set a unified URL:
   ```cmd
   set DATABASE_URL=mysql://root:YourPassword@localhost:3306/expense_tracker
   ```
3. **War-bundled properties:**
   Edit `src/db.properties` before building.

---

### Stage 3: Build the Application with Apache Ant

To compile Java sources and package the deployable WAR:

```cmd
ant clean war
```
*(Or simply double-click `build.bat` or run `.\build.ps1` in PowerShell)*

The build will output:
```
BUILD SUCCESSFUL
WAR generated at: dist/PersonalExpenseTracker.war
```

---

### Stage 4: Deploy and Run on Apache Tomcat 9

#### Option A: Direct Ant Deployment
If Tomcat is installed at `C:\apache-tomcat-9`:
```cmd
ant deploy
```
This automatically copies the WAR to `C:\apache-tomcat-9\webapps\PersonalExpenseTracker.war` and `C:\apache-tomcat-9\webapps\ROOT.war`.

#### Option B: Manual Deployment
1. Copy `dist/PersonalExpenseTracker.war` to `C:\apache-tomcat-9\webapps\ROOT.war`.
2. Start Tomcat:
   ```cmd
   C:\apache-tomcat-9\bin\startup.bat
   ```
3. Open your browser and navigate to:
   - Root URL: **`http://localhost:8080/`**
   - Context URL: **`http://localhost:8080/PersonalExpenseTracker/`**

---

## 🧪 Local Testing Instructions

1. **Dashboard Verification:**
   - Open `http://localhost:8080/` in Chrome/Edge/Firefox.
   - Verify KPI cards display **Total Income**, **Total Expenses**, and **Remaining Balance**.
   - Confirm the sidebar database indicator shows `MySQL Connected` (or `Demo Mode` if testing offline).
   - Check that the **Category-wise Expense** doughnut chart and **Monthly Cashflow** bar chart render cleanly.

2. **Testing Expense Management:**
   - Click **Expenses** in the sidebar.
   - Click **+ Add Expense**.
   - Enter: Title: `Office Lunch`, Amount: `25.50`, Category: `Food & Dining`, Date: Today's date. Click **Save Expense**.
   - Verify the new entry appears immediately at the top of the table.
   - Click the **Edit** icon on `Office Lunch`, modify the amount to `32.00`, and save.
   - Click the **Delete** icon on `Office Lunch`. A confirmation modal will appear. Confirm deletion and check that totals recalculate.
   - Click **Export CSV** and verify the `.csv` download.

3. **Testing Income Management:**
   - Click **Income** in the sidebar.
   - Click **+ Add Income**.
   - Enter: Title: `Freelance Consulting`, Amount: `500.00`, Source: `Freelance`, Date: Today. Click **Save Income**.
   - Verify that your Remaining Balance and Total Income on the Dashboard increase accordingly.

4. **Testing Reports & Trends:**
   - Click **Reports & Trends** in the sidebar.
   - Check the 3 interactive charts (Cashflow Dynamics, Expense Breakdown, Income Sources).
   - Click **Print Report** to inspect the printable statement.

---

## 🚢 Render Cloud Deployment Instructions (Beginner-Friendly)

Personal Expense Tracker is configured for cloud deployment on **Render** using a multi-stage **Dockerfile** that runs on **Apache Tomcat 9**, automatically respects Render's dynamic `$PORT`, enables `RemoteIpValve` for HTTPS SSL reverse-proxy termination, and connects securely via JDBC over SSL to any cloud MySQL database.

---

### Step 1: Push Project Updates to GitHub
Your local repository is already connected to:
`https://github.com/24p61a0553/personalexpensetracker.git`

To push the latest code:
```bash
git add .
git commit -m "Configure Render cloud deployment with cloud JDBC and Srinivas profile"
git push -u origin main
```

---

### Step 2: Set Up Your Cloud MySQL Database
You can use any cloud-hosted MySQL provider, such as:
* **Aiven MySQL** (Free tier available at [aiven.io](https://aiven.io/))
* **Railway MySQL** ([railway.app](https://railway.app/))
* **TiDB Cloud Serverless** ([tidbcloud.com](https://tidbcloud.com/))
* **DigitalOcean / AWS RDS / Cleardb**

#### A. Initialize the Cloud Database Schema
Open your cloud provider's MySQL query console (or connect via MySQL Workbench or DBeaver) and run the safe cloud initialization script:
* File: **[`database/cloud_init.sql`](database/cloud_init.sql)**
* This script uses `CREATE TABLE IF NOT EXISTS` and `ON DUPLICATE KEY UPDATE`.
* It safely creates:
  1. `expense_user_profile` with default person name **Srinivas**.
  2. `categories` table with 15 initial income and expense categories.
  3. `transactions` table with initial demo records associated with **Srinivas**.
* It never drops or deletes any tables.

#### B. Note Your Cloud Database Connection Details
From your cloud database dashboard, copy:
- **Host**: e.g., `mysql-1234abcd-myproject.aivencloud.com`
- **Port**: e.g., `12345` (or `3306`)
- **Database Name**: e.g., `defaultdb` or `expense_tracker`
- **User**: e.g., `avnadmin` or `root`
- **Password**: *(Keep this private; never commit or paste it into public chats!)*

---

### Step 3: Create the Web Service on Render

1. Log in to your [Render Dashboard](https://dashboard.render.com/).
2. Click **New +** (top right) and select **Web Service**.
3. Choose **Build and deploy from a Git repository**.
4. Connect or select your GitHub repository:
   `24p61a0553/personalexpensetracker`
5. Configure the web service settings:
   * **Name**: `personalexpensetracker` (or your preferred name)
   * **Region**: Choose the region closest to your cloud database (e.g. Frankfurt, Oregon, Singapore)
   * **Branch**: `main`
   * **Runtime**: **Docker** *(Render detects the root `Dockerfile` automatically)*
   * **Instance Type**: **Free**
6. Scroll down to the **Environment Variables** section. Click **Add Environment Variable** for each of the following:

   | Environment Variable | Description / Example Value |
   | :--- | :--- |
   | `DB_HOST` | Hostname of your cloud database (e.g. `mysql-xyz.aivencloud.com`) |
   | `DB_PORT` | Port of your cloud database (e.g. `3306` or `12345`) |
   | `DB_NAME` | Database name (e.g. `defaultdb` or `expense_tracker`) |
   | `DB_USER` | Database username (e.g. `avnadmin` or `root`) |
   | `DB_PASS` | Your secure cloud database password *(Render masks this automatically)* |
   | `DB_SSL` | `true` *(Enables SSL encryption between Tomcat and Cloud MySQL)* |

   > **Alternative (Single Variable):** If your cloud provider gives a full connection URI (e.g. `mysql://user:pass@host:port/dbname`), you can simply add a single environment variable named `DATABASE_URL` with that value.

7. Click **Create Web Service** at the bottom of the page.

---

### Step 4: Monitor Build & Verify Deployment

1. Render will fetch the latest code from `main`, execute the multi-stage Docker build with Apache Ant, deploy the WAR as `ROOT.war`, and launch Tomcat on Render's assigned `$PORT`.
2. In the Render service logs, look for:
   ```
   [Startup] Binding Tomcat HTTP connector to port: 10000
   [DBConnection] SUCCESS: Connected to MySQL database (...)
   [DBConnection] Verified schema tables (expense_user_profile, categories, transactions).
   ```
3. Once the deployment status turns green (**Live**), click your public Render URL:
   `https://personalexpensetracker.onrender.com/` (or your specific service name URL).
4. Verify:
   - Header Account Profile shows **Srinivas**.
   - Sidebar displays **Welcome, Srinivas**.
   - Currency displays in Indian Rupees (**₹**) with Indian number formatting (e.g., `₹17,250.50`).
   - Adding and editing transactions persists directly into your cloud MySQL database.

---

## 🛠️ Technology Stack Summary

| Layer | Technology | Purpose |
| :--- | :--- | :--- |
| **Frontend** | HTML5, CSS3, Modern ES6+ JavaScript | Responsive layout, dark theme, interactive modals |
| **Data Visualization** | Chart.js 4.4.1 | Doughnut, Bar, and Pie cashflow analytics |
| **Backend** | Java Servlets (Java 8/17) | RESTful API endpoints for transactions and analytics |
| **Database Connectivity** | JDBC, `PreparedStatement` | High-performance, parameterized SQL communication |
| **JSON Serialization** | Google Gson 2.10.1 | Serialization/deserialization between Java and JSON |
| **Database** | MySQL 8.0+ | Persistent storage with `DECIMAL` monetary types |
| **Web Server** | Apache Tomcat 9 | Servlet container hosting the application |
| **Build Automation** | Apache Ant 1.10+ | Clean, compile, package WAR, and deploy tasks |
| **Containerization** | Docker, Docker Compose | Production container with dynamic Render `$PORT` support |

---

## 📄 License
Developed for educational and personal finance tracking purposes. Open-source and freely customizable.
