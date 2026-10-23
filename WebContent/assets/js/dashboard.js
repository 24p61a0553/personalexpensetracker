/**
 * Personal Expense Tracker - Dashboard Controller
 * Connects frontend directly to Java Servlets (/api/dashboard, /api/transactions)
 */

let categoryExpenseChart = null;
let monthlyComparisonChart = null;
let currentEditingId = null;
let pendingDeleteId = null;
let cachedCategories = [];

document.addEventListener('DOMContentLoaded', () => {
  API.initPersonProfile();
  initDateDisplay();
  loadCategories();
  loadDashboardData();
  setupEventListeners();
});

function initDateDisplay() {
  const dateEl = document.getElementById('currentDateText');
  if (dateEl) {
    const today = new Date();
    dateEl.textContent = today.toLocaleDateString('en-US', {
      weekday: 'short',
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }
}

async function loadCategories() {
  try {
    const res = await API.get('/api/categories');
    if (res && res.data) {
      cachedCategories = res.data;
      populateCategoryDropdown();
    }
  } catch (err) {
    console.warn('Could not load categories:', err);
  }
}

function populateCategoryDropdown(selectedCategory = '', targetType = 'EXPENSE') {
  const catSelect = document.getElementById('txCategory');
  if (!catSelect) return;

  catSelect.innerHTML = '<option value="">-- Select Category --</option>';
  const filtered = cachedCategories.filter(c => c.type.toUpperCase() === targetType.toUpperCase());

  filtered.forEach(c => {
    const opt = document.createElement('option');
    opt.value = c.name;
    opt.textContent = c.name;
    if (c.name === selectedCategory) opt.selected = true;
    catSelect.appendChild(opt);
  });
}

async function loadDashboardData() {
  try {
    const res = await API.get('/api/dashboard');
    if (!res || !res.data) throw new Error('Invalid dashboard response');

    const d = res.data;

    // Update KPI Cards
    animateNumber('kpiTotalIncome', d.totalIncome || 0, true);
    animateNumber('kpiTotalExpense', d.totalExpense || 0, true);
    animateNumber('kpiNetBalance', d.netBalance || 0, true);
    animateNumber('kpiThisMonthExpense', d.thisMonthExpense || 0, true);

    const balanceCard = document.getElementById('balanceKpiCard');
    if (balanceCard) {
      if ((d.netBalance || 0) >= 0) {
        balanceCard.classList.remove('kpi-expense');
        balanceCard.classList.add('kpi-balance');
      } else {
        balanceCard.classList.remove('kpi-balance');
        balanceCard.classList.add('kpi-expense');
      }
    }

    const savingsRateEl = document.getElementById('kpiSavingsRate');
    if (savingsRateEl) {
      savingsRateEl.textContent = `${(d.savingsRate || 0).toFixed(1)}% savings rate`;
    }

    // Update Database Status Badge
    updateDatabaseStatus(d.databaseConnected, d.databaseSource);

    // Render Charts
    renderCategoryChart(d.categoryBreakdown || []);
    loadAndRenderMonthlyChart();

    // Render Recent Transactions
    renderRecentTransactions(d.recentTransactions || []);

  } catch (err) {
    console.error('Failed to load dashboard data:', err);
    API.showToast('Failed to load dashboard: ' + err.message, 'error');
  }
}

function updateDatabaseStatus(connected, source) {
  const dot = document.getElementById('dbStatusDot');
  const text = document.getElementById('dbStatusText');
  if (!dot || !text) return;

  if (connected) {
    dot.className = 'status-dot connected';
    text.textContent = 'MySQL Connected';
    text.title = source || 'Connected to expense_tracker MySQL database';
  } else {
    dot.className = 'status-dot offline';
    text.textContent = 'Demo Mode (Offline)';
    text.title = 'MySQL not detected. Using memory fallback. Run setup_db.bat to link MySQL.';
  }
}

function renderCategoryChart(categories) {
  const canvas = document.getElementById('categoryExpenseChart');
  if (!canvas) return;

  const ctx = canvas.getContext('2d');
  if (categoryExpenseChart) {
    categoryExpenseChart.destroy();
  }

  const pillsContainer = document.getElementById('categoryPillsList');
  if (pillsContainer) pillsContainer.innerHTML = '';

  if (!categories || categories.length === 0) {
    categoryExpenseChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: ['No Expenses Yet'],
        datasets: [{
          data: [1],
          backgroundColor: ['rgba(255, 255, 255, 0.08)'],
          borderWidth: 0
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: { enabled: false }
        }
      }
    });
    return;
  }

  const labels = categories.map(c => c.category);
  const data = categories.map(c => c.totalAmount);
  const colors = categories.map(c => c.color || '#6366f1');

  categoryExpenseChart = new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: labels,
      datasets: [{
        data: data,
        backgroundColor: colors,
        borderColor: '#0c152a',
        borderWidth: 2,
        hoverOffset: 6
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'right',
          labels: {
            color: '#cbd5e1',
            font: { family: 'Inter', size: 12 },
            padding: 12,
            boxWidth: 12,
            boxHeight: 12,
            usePointStyle: true
          }
        },
        tooltip: {
          backgroundColor: '#0c152a',
          titleColor: '#f8fafc',
          bodyColor: '#cbd5e1',
          borderColor: 'rgba(255, 255, 255, 0.1)',
          borderWidth: 1,
          padding: 10,
          callbacks: {
            label: function(ctx) {
              const label = ctx.label || '';
              const val = ctx.raw || 0;
              const total = ctx.dataset.data.reduce((a, b) => a + b, 0);
              const pct = total > 0 ? ((val / total) * 100).toFixed(1) : 0;
              return ` ${label}: ${API.formatCurrency(val)} (${pct}%)`;
            }
          }
        }
      },
      cutout: '68%'
    }
  });

  if (pillsContainer) {
    categories.forEach(c => {
      const pill = document.createElement('div');
      pill.className = 'cat-pill';
      pill.innerHTML = `
        <span class="cat-color-dot" style="background: ${c.color || '#6366f1'};"></span>
        <span>${c.category}: <strong>${API.formatCurrency(c.totalAmount)}</strong> (${(c.percentage || 0).toFixed(1)}%)</span>
      `;
      pillsContainer.appendChild(pill);
    });
  }
}

async function loadAndRenderMonthlyChart() {
  try {
    const res = await API.get('/api/reports');
    if (!res || !res.data) return;

    const canvas = document.getElementById('monthlyComparisonChart');
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (monthlyComparisonChart) {
      monthlyComparisonChart.destroy();
    }

    const trends = res.data.monthlyTrends || [];
    const labels = trends.map(t => t.monthLabel);
    const incomeData = trends.map(t => t.income);
    const expenseData = trends.map(t => t.expense);

    monthlyComparisonChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [
          {
            label: 'Income',
            data: incomeData,
            backgroundColor: 'rgba(16, 185, 129, 0.85)',
            borderColor: '#10b981',
            borderRadius: 6,
            maxBarThickness: 32
          },
          {
            label: 'Expenses',
            data: expenseData,
            backgroundColor: 'rgba(239, 68, 68, 0.85)',
            borderColor: '#ef4444',
            borderRadius: 6,
            maxBarThickness: 32
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: {
            grid: { display: false },
            ticks: { color: '#94a3b8', font: { family: 'Inter', size: 11 } }
          },
          y: {
            grid: { color: 'rgba(255, 255, 255, 0.05)' },
            ticks: {
              color: '#94a3b8',
              font: { family: 'Inter', size: 11 },
              callback: value => '₹' + Number(value).toLocaleString('en-IN')
            }
          }
        },
        plugins: {
          legend: {
            position: 'top',
            labels: { color: '#cbd5e1', font: { family: 'Inter', size: 12 }, usePointStyle: true }
          },
          tooltip: {
            backgroundColor: '#0c152a',
            borderColor: 'rgba(255, 255, 255, 0.1)',
            borderWidth: 1,
            callbacks: {
              label: ctx => ` ${ctx.dataset.label}: ${API.formatCurrency(ctx.raw)}`
            }
          }
        }
      }
    });
  } catch (err) {
    console.warn('Could not load monthly chart:', err);
  }
}

function renderRecentTransactions(transactions) {
  const tbody = document.getElementById('recentTransactionsTbody');
  const emptyState = document.getElementById('recentEmptyState');
  if (!tbody) return;

  tbody.innerHTML = '';

  if (!transactions || transactions.length === 0) {
    if (emptyState) emptyState.style.display = 'block';
    return;
  }

  if (emptyState) emptyState.style.display = 'none';

  transactions.forEach(t => {
    const isIncome = t.type.toUpperCase() === 'INCOME';
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>
        <span class="badge ${isIncome ? 'badge-income' : 'badge-expense'}">
          <i class="fas ${isIncome ? 'fa-arrow-down' : 'fa-arrow-up'}"></i> ${t.type}
        </span>
      </td>
      <td>
        <div style="font-weight: 600; color: #f8fafc;">${escapeHtml(t.title)}</div>
        <div style="font-size: 0.78rem; color: #94a3b8;">${escapeHtml(t.paymentMethod || 'Cash')}${t.notes ? ' &bull; ' + escapeHtml(t.notes) : ''}</div>
      </td>
      <td>
        <span class="badge badge-category">${escapeHtml(t.category)}</span>
      </td>
      <td>${API.formatDate(t.transactionDate)}</td>
      <td class="${isIncome ? 'amount-income' : 'amount-expense'}" style="font-size: 0.95rem;">
        ${isIncome ? '+' : '-'}${API.formatCurrency(t.amount)}
      </td>
      <td>
        <div class="action-buttons">
          <button class="btn-icon" title="Edit Transaction" onclick="openEditTransactionModal(${t.id})">
            <i class="fas fa-edit"></i>
          </button>
          <button class="btn-icon delete" title="Delete Transaction" onclick="confirmDeleteTransaction(${t.id}, '${escapeQuotes(t.title)}')">
            <i class="fas fa-trash-alt"></i>
          </button>
        </div>
      </td>
    `;
    tbody.appendChild(tr);
  });
}

function setupEventListeners() {
  // Mobile sidebar toggle and backdrop
  const toggleBtn = document.getElementById('sidebarToggleBtn');
  const sidebar = document.getElementById('sidebar');
  const backdrop = document.getElementById('sidebarBackdrop');

  if (toggleBtn && sidebar) {
    toggleBtn.addEventListener('click', () => {
      sidebar.classList.toggle('open');
      if (backdrop) backdrop.classList.toggle('active');
    });
  }

  if (backdrop && sidebar) {
    backdrop.addEventListener('click', () => {
      sidebar.classList.remove('open');
      backdrop.classList.remove('active');
    });
  }

  // Type change in modal to update categories
  const typeSelect = document.getElementById('txType');
  if (typeSelect) {
    typeSelect.addEventListener('change', () => {
      populateCategoryDropdown('', typeSelect.value);
    });
  }

  // Transaction form submit
  const form = document.getElementById('transactionForm');
  if (form) {
    form.addEventListener('submit', handleSaveTransaction);
  }

  // Set today's date in add modal by default
  const dateInput = document.getElementById('txDate');
  if (dateInput && !dateInput.value) {
    dateInput.value = new Date().toISOString().split('T')[0];
  }
}

function openAddTransactionModal(presetType = 'EXPENSE') {
  currentEditingId = null;
  document.getElementById('modalTitle').innerHTML = '<i class="fas fa-plus-circle" style="color: var(--secondary);"></i> Add New Transaction';
  document.getElementById('transactionForm').reset();

  const typeSelect = document.getElementById('txType');
  if (typeSelect) {
    typeSelect.value = presetType;
    populateCategoryDropdown('', presetType);
  }

  const dateInput = document.getElementById('txDate');
  if (dateInput) {
    dateInput.value = new Date().toISOString().split('T')[0];
  }

  document.getElementById('transactionModal').classList.add('open');
}

async function openEditTransactionModal(id) {
  currentEditingId = id;
  document.getElementById('modalTitle').innerHTML = '<i class="fas fa-edit" style="color: var(--secondary);"></i> Edit Transaction';

  try {
    const res = await API.get(`/api/transactions?id=${id}`);
    if (!res || !res.data) throw new Error('Transaction not found');

    const t = res.data;
    document.getElementById('txTitle').value = t.title || '';
    document.getElementById('txAmount').value = t.amount || '';
    document.getElementById('txType').value = t.type || 'EXPENSE';
    populateCategoryDropdown(t.category, t.type);
    document.getElementById('txDate').value = t.transactionDate || '';
    document.getElementById('txPayment').value = t.paymentMethod || 'Cash';
    document.getElementById('txNotes').value = t.notes || '';

    document.getElementById('transactionModal').classList.add('open');
  } catch (err) {
    API.showToast('Could not load transaction: ' + err.message, 'error');
  }
}

function closeTransactionModal() {
  document.getElementById('transactionModal').classList.remove('open');
  currentEditingId = null;
}

async function handleSaveTransaction(e) {
  e.preventDefault();

  const title = document.getElementById('txTitle').value.trim();
  const amount = parseFloat(document.getElementById('txAmount').value);
  const type = document.getElementById('txType').value;
  const category = document.getElementById('txCategory').value;
  const transactionDate = document.getElementById('txDate').value;
  const paymentMethod = document.getElementById('txPayment').value;
  const notes = document.getElementById('txNotes').value.trim();

  if (!title) {
    API.showToast('Please provide a description/title', 'warning');
    return;
  }
  if (isNaN(amount) || amount <= 0) {
    API.showToast('Amount must be a positive number', 'warning');
    return;
  }
  if (!category) {
    API.showToast('Please select a category', 'warning');
    return;
  }
  if (!transactionDate) {
    API.showToast('Please select a date', 'warning');
    return;
  }

  const payload = {
    title,
    amount,
    type,
    category,
    transactionDate,
    paymentMethod,
    notes
  };

  const saveBtn = document.getElementById('saveTransactionBtn');
  const originalText = saveBtn.innerHTML;
  saveBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Saving...';
  saveBtn.disabled = true;

  try {
    if (currentEditingId) {
      payload.id = currentEditingId;
      await API.put('/api/transactions', payload);
      API.showToast('Transaction updated successfully', 'success');
    } else {
      await API.post('/api/transactions', payload);
      API.showToast('Transaction added successfully', 'success');
    }

    closeTransactionModal();
    loadDashboardData();
  } catch (err) {
    API.showToast(err.message || 'Failed to save transaction', 'error');
  } finally {
    saveBtn.innerHTML = originalText;
    saveBtn.disabled = false;
  }
}

function confirmDeleteTransaction(id, title) {
  pendingDeleteId = id;
  document.getElementById('deleteTxTitle').textContent = `"${title}"`;
  document.getElementById('deleteConfirmModal').classList.add('open');
}

function closeDeleteModal() {
  document.getElementById('deleteConfirmModal').classList.remove('open');
  pendingDeleteId = null;
}

async function executeDeleteTransaction() {
  if (!pendingDeleteId) return;

  const btn = document.getElementById('confirmDeleteBtn');
  const orig = btn.innerHTML;
  btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Deleting...';
  btn.disabled = true;

  try {
    await API.delete(`/api/transactions?id=${pendingDeleteId}`);
    API.showToast('Transaction deleted successfully', 'success');
    closeDeleteModal();
    loadDashboardData();
  } catch (err) {
    API.showToast('Failed to delete transaction: ' + err.message, 'error');
  } finally {
    btn.innerHTML = orig;
    btn.disabled = false;
  }
}

function animateNumber(elementId, targetValue, isCurrency = false) {
  const el = document.getElementById(elementId);
  if (!el) return;
  el.textContent = isCurrency ? API.formatCurrency(targetValue) : targetValue;
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function escapeQuotes(str) {
  if (!str) return '';
  return str.replace(/'/g, "\\'").replace(/"/g, '&quot;');
}
