/**
 * Personal Expense Tracker - Financial Reports & Analytics Controller
 */

let monthlyCashflowChart = null;
let expenseCategoryChart = null;
let incomeSourceChart = null;
let cachedReportData = null;

document.addEventListener('DOMContentLoaded', () => {
  API.initPersonProfile();
  loadReports();
  setupEventListeners();
});

async function loadReports() {
  try {
    const res = await API.get('/api/reports');
    if (!res || !res.data) throw new Error('Invalid reports payload');

    cachedReportData = res.data;
    const rd = cachedReportData;

    // Populate Key Metrics Cards
    document.getElementById('metricAvgExpense').textContent = API.formatCurrency(rd.averageMonthlyExpense || 0);

    const savingsRate = rd.overallSavingsRate || 0;
    const savingsEl = document.getElementById('metricSavingsRate');
    savingsEl.textContent = `${savingsRate.toFixed(1)}%`;
    if (savingsRate >= 30) {
      savingsEl.className = 'metric-card-val savings-healthy';
    } else if (savingsRate >= 10) {
      savingsEl.className = 'metric-card-val savings-warning';
    } else {
      savingsEl.className = 'metric-card-val savings-danger';
    }

    document.getElementById('metricTopExpense').textContent = rd.topExpenseCategory || 'None';
    document.getElementById('metricTopExpenseSub').textContent = API.formatCurrency(rd.topExpenseCategoryAmount || 0);

    document.getElementById('metricTopIncome').textContent = rd.topIncomeCategory || 'None';
    document.getElementById('metricTopIncomeSub').textContent = API.formatCurrency(rd.topIncomeCategoryAmount || 0);

    // Render Charts
    renderCashflowChart(rd.monthlyTrends || []);
    renderExpenseCategoryChart(rd.expenseCategories || []);
    renderIncomeSourceChart(rd.incomeCategories || []);

    // Render Detailed Table
    renderMonthlyBreakdownTable(rd.monthlyTrends || []);

  } catch (err) {
    console.error('Failed to load reports:', err);
    API.showToast('Failed to load financial reports: ' + err.message, 'error');
  }
}

function renderCashflowChart(trends) {
  const canvas = document.getElementById('monthlyCashflowChart');
  if (!canvas) return;

  const ctx = canvas.getContext('2d');
  if (monthlyCashflowChart) monthlyCashflowChart.destroy();

  const labels = trends.map(t => t.monthLabel);
  const incomeData = trends.map(t => t.income);
  const expenseData = trends.map(t => t.expense);
  const savingsData = trends.map(t => t.netSavings);

  monthlyCashflowChart = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: labels,
      datasets: [
        {
          label: 'Income',
          data: incomeData,
          backgroundColor: 'rgba(16, 185, 129, 0.85)',
          borderRadius: 6,
          order: 2
        },
        {
          label: 'Expense',
          data: expenseData,
          backgroundColor: 'rgba(239, 68, 68, 0.85)',
          borderRadius: 6,
          order: 2
        },
        {
          label: 'Net Savings',
          data: savingsData,
          type: 'line',
          borderColor: '#38bdf8',
          backgroundColor: 'rgba(56, 189, 248, 0.15)',
          fill: true,
          tension: 0.35,
          borderWidth: 3,
          pointRadius: 4,
          pointBackgroundColor: '#38bdf8',
          order: 1
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
            callback: v => '₹' + Number(v).toLocaleString('en-IN')
          }
        }
      },
      plugins: {
        legend: {
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
}

function renderExpenseCategoryChart(categories) {
  const canvas = document.getElementById('expenseCategoryChart');
  if (!canvas) return;

  const ctx = canvas.getContext('2d');
  if (expenseCategoryChart) expenseCategoryChart.destroy();

  if (!categories || categories.length === 0) return;

  expenseCategoryChart = new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: categories.map(c => c.category),
      datasets: [{
        data: categories.map(c => c.totalAmount),
        backgroundColor: categories.map(c => c.color || '#ef4444'),
        borderColor: '#0c152a',
        borderWidth: 2
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'right',
          labels: { color: '#cbd5e1', font: { family: 'Inter', size: 11 }, boxWidth: 12, usePointStyle: true }
        },
        tooltip: {
          backgroundColor: '#0c152a',
          callbacks: {
            label: ctx => {
              const total = ctx.dataset.data.reduce((a, b) => a + b, 0);
              const pct = total > 0 ? ((ctx.raw / total) * 100).toFixed(1) : 0;
              return ` ${ctx.label}: ${API.formatCurrency(ctx.raw)} (${pct}%)`;
            }
          }
        }
      },
      cutout: '62%'
    }
  });
}

function renderIncomeSourceChart(categories) {
  const canvas = document.getElementById('incomeSourceChart');
  if (!canvas) return;

  const ctx = canvas.getContext('2d');
  if (incomeSourceChart) incomeSourceChart.destroy();

  if (!categories || categories.length === 0) return;

  incomeSourceChart = new Chart(ctx, {
    type: 'pie',
    data: {
      labels: categories.map(c => c.category),
      datasets: [{
        data: categories.map(c => c.totalAmount),
        backgroundColor: categories.map(c => c.color || '#10b981'),
        borderColor: '#0c152a',
        borderWidth: 2
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'right',
          labels: { color: '#cbd5e1', font: { family: 'Inter', size: 11 }, boxWidth: 12, usePointStyle: true }
        },
        tooltip: {
          backgroundColor: '#0c152a',
          callbacks: {
            label: ctx => {
              const total = ctx.dataset.data.reduce((a, b) => a + b, 0);
              const pct = total > 0 ? ((ctx.raw / total) * 100).toFixed(1) : 0;
              return ` ${ctx.label}: ${API.formatCurrency(ctx.raw)} (${pct}%)`;
            }
          }
        }
      }
    }
  });
}

function renderMonthlyBreakdownTable(trends) {
  const tbody = document.getElementById('monthlyBreakdownTbody');
  if (!tbody) return;

  tbody.innerHTML = '';

  trends.forEach(t => {
    const isSurplus = t.netSavings >= 0;
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td style="font-weight: 600; color: #f8fafc;">${escapeHtml(t.monthLabel)}</td>
      <td class="amount-income">+${API.formatCurrency(t.income)}</td>
      <td class="amount-expense">-${API.formatCurrency(t.expense)}</td>
      <td style="font-weight: 700; color: ${isSurplus ? 'var(--income)' : 'var(--expense)'};">
        ${isSurplus ? '+' : ''}${API.formatCurrency(t.netSavings)}
      </td>
      <td>${(t.savingsRate || 0).toFixed(1)}%</td>
      <td>
        <span class="badge ${isSurplus ? 'badge-income' : 'badge-expense'}">
          <i class="fas ${isSurplus ? 'fa-check' : 'fa-exclamation'}"></i> ${isSurplus ? 'Surplus' : 'Deficit'}
        </span>
      </td>
    `;
    tbody.appendChild(tr);
  });
}

function setupEventListeners() {
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
}

function exportReportCSV() {
  if (!cachedReportData || !cachedReportData.monthlyTrends) {
    API.showToast('No report data to export', 'warning');
    return;
  }

  const exportData = cachedReportData.monthlyTrends.map(t => ({
    Month: t.monthLabel,
    'Total Income (INR)': t.income,
    'Total Expense (INR)': t.expense,
    'Net Savings (INR)': t.netSavings,
    'Savings Rate': `${(t.savingsRate || 0).toFixed(1)}%`
  }));

  API.exportToCSV(exportData, `financial_report_${new Date().toISOString().split('T')[0]}.csv`);
}

function printReport() {
  window.print();
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
