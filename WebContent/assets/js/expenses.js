/**
 * Personal Expense Tracker - Expense Management Controller
 */

let cachedExpenses = [];
let cachedCategories = [];
let currentEditingId = null;
let pendingDeleteId = null;

document.addEventListener('DOMContentLoaded', () => {
  API.initPersonProfile();
  initDatePresets();
  loadExpenseCategories();
  loadExpenses();
  setupEventListeners();
});

function initDatePresets() {
  const dateInput = document.getElementById('txDate');
  if (dateInput && !dateInput.value) {
    dateInput.value = new Date().toISOString().split('T')[0];
  }
}

async function loadExpenseCategories() {
  try {
    const res = await API.get('/api/categories?type=EXPENSE');
    if (res && res.data) {
      cachedCategories = res.data;
      const select = document.getElementById('filterCategory');
      const modalSelect = document.getElementById('txCategory');

      if (select) {
        select.innerHTML = '<option value="">All Categories</option>';
        cachedCategories.forEach(c => {
          const opt = document.createElement('option');
          opt.value = c.name;
          opt.textContent = c.name;
          select.appendChild(opt);
        });
      }

      if (modalSelect) {
        modalSelect.innerHTML = '<option value="">-- Select Category --</option>';
        cachedCategories.forEach(c => {
          const opt = document.createElement('option');
          opt.value = c.name;
          opt.textContent = c.name;
          modalSelect.appendChild(opt);
        });
      }
    }
  } catch (err) {
    console.warn('Could not load expense categories:', err);
  }
}

async function loadExpenses() {
  const search = document.getElementById('searchInput')?.value.trim() || '';
  const category = document.getElementById('filterCategory')?.value || '';
  const startDate = document.getElementById('startDate')?.value || '';
  const endDate = document.getElementById('endDate')?.value || '';

  const params = new URLSearchParams({ type: 'EXPENSE' });
  if (search) params.append('search', search);
  if (category) params.append('category', category);
  if (startDate) params.append('startDate', startDate);
  if (endDate) params.append('endDate', endDate);

  try {
    const res = await API.get(`/api/transactions?${params.toString()}`);
    if (res && res.data) {
      cachedExpenses = res.data;
      renderExpensesTable(cachedExpenses);
      updateExpenseStats(cachedExpenses);
    }
  } catch (err) {
    console.error('Failed to load expenses:', err);
    API.showToast('Failed to load expenses: ' + err.message, 'error');
  }
}

function updateExpenseStats(expenses) {
  const total = expenses.reduce((sum, e) => sum + parseFloat(e.amount || 0), 0);
  const count = expenses.length;
  const avg = count > 0 ? total / count : 0;

  // Find top category
  const catSums = {};
  expenses.forEach(e => {
    catSums[e.category] = (catSums[e.category] || 0) + parseFloat(e.amount || 0);
  });
  let topCat = 'None';
  let topAmt = 0;
  for (const [c, a] of Object.entries(catSums)) {
    if (a > topAmt) {
      topAmt = a;
      topCat = c;
    }
  }

  document.getElementById('statTotalExpenses').textContent = API.formatCurrency(total);
  document.getElementById('statExpenseCount').textContent = count;
  document.getElementById('statAvgExpense').textContent = API.formatCurrency(avg);
  document.getElementById('statTopCategory').textContent = topCat;
}

function renderExpensesTable(expenses) {
  const tbody = document.getElementById('expensesTbody');
  const emptyState = document.getElementById('expensesEmptyState');
  if (!tbody) return;

  tbody.innerHTML = '';

  if (!expenses || expenses.length === 0) {
    if (emptyState) emptyState.style.display = 'block';
    return;
  }

  if (emptyState) emptyState.style.display = 'none';

  expenses.forEach(t => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>
        <div style="font-weight: 600; color: #f8fafc;">${escapeHtml(t.title)}</div>
        <div style="font-size: 0.78rem; color: #94a3b8;">${escapeHtml(t.paymentMethod || 'Cash')}${t.notes ? ' &bull; ' + escapeHtml(t.notes) : ''}</div>
      </td>
      <td>
        <span class="badge badge-category">${escapeHtml(t.category)}</span>
      </td>
      <td>${API.formatDate(t.transactionDate)}</td>
      <td class="amount-expense" style="font-size: 0.95rem;">
        -${API.formatCurrency(t.amount)}
      </td>
      <td>
        <div class="action-buttons">
          <button class="btn-icon" title="Edit Expense" onclick="openEditExpenseModal(${t.id})">
            <i class="fas fa-edit"></i>
          </button>
          <button class="btn-icon delete" title="Delete Expense" onclick="confirmDeleteExpense(${t.id}, '${escapeQuotes(t.title)}')">
            <i class="fas fa-trash-alt"></i>
          </button>
        </div>
      </td>
    `;
    tbody.appendChild(tr);
  });
}

function setupEventListeners() {
  const searchInput = document.getElementById('searchInput');
  if (searchInput) {
    let timeout = null;
    searchInput.addEventListener('input', () => {
      clearTimeout(timeout);
      timeout = setTimeout(loadExpenses, 300);
    });
  }

  const catFilter = document.getElementById('filterCategory');
  if (catFilter) catFilter.addEventListener('change', loadExpenses);

  const startFilter = document.getElementById('startDate');
  if (startFilter) startFilter.addEventListener('change', loadExpenses);

  const endFilter = document.getElementById('endDate');
  if (endFilter) endFilter.addEventListener('change', loadExpenses);

  const quickFilter = document.getElementById('quickDateFilter');
  if (quickFilter) {
    quickFilter.addEventListener('change', (e) => {
      applyQuickDatePreset(e.target.value);
    });
  }

  const form = document.getElementById('expenseForm');
  if (form) form.addEventListener('submit', handleSaveExpense);

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

function applyQuickDatePreset(preset) {
  const now = new Date();
  const startEl = document.getElementById('startDate');
  const endEl = document.getElementById('endDate');

  if (preset === 'THIS_MONTH') {
    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
    startEl.value = firstDay.toISOString().split('T')[0];
    endEl.value = now.toISOString().split('T')[0];
  } else if (preset === 'LAST_30') {
    const past = new Date();
    past.setDate(past.getDate() - 30);
    startEl.value = past.toISOString().split('T')[0];
    endEl.value = now.toISOString().split('T')[0];
  } else if (preset === 'THIS_YEAR') {
    const firstDay = new Date(now.getFullYear(), 0, 1);
    startEl.value = firstDay.toISOString().split('T')[0];
    endEl.value = now.toISOString().split('T')[0];
  } else {
    startEl.value = '';
    endEl.value = '';
  }
  loadExpenses();
}

function openAddExpenseModal() {
  currentEditingId = null;
  document.getElementById('modalTitle').innerHTML = '<i class="fas fa-plus-circle" style="color: var(--expense);"></i> Add New Expense';
  document.getElementById('expenseForm').reset();
  const dateInput = document.getElementById('txDate');
  if (dateInput) dateInput.value = new Date().toISOString().split('T')[0];
  document.getElementById('expenseModal').classList.add('open');
}

async function openEditExpenseModal(id) {
  currentEditingId = id;
  document.getElementById('modalTitle').innerHTML = '<i class="fas fa-edit" style="color: var(--secondary);"></i> Edit Expense';

  try {
    const res = await API.get(`/api/transactions?id=${id}`);
    if (!res || !res.data) throw new Error('Expense not found');

    const t = res.data;
    document.getElementById('txTitle').value = t.title || '';
    document.getElementById('txAmount').value = t.amount || '';
    document.getElementById('txCategory').value = t.category || '';
    document.getElementById('txDate').value = t.transactionDate || '';
    document.getElementById('txPayment').value = t.paymentMethod || 'Cash';
    document.getElementById('txNotes').value = t.notes || '';

    document.getElementById('expenseModal').classList.add('open');
  } catch (err) {
    API.showToast('Could not load expense: ' + err.message, 'error');
  }
}

function closeExpenseModal() {
  document.getElementById('expenseModal').classList.remove('open');
  currentEditingId = null;
}

async function handleSaveExpense(e) {
  e.preventDefault();

  const title = document.getElementById('txTitle').value.trim();
  const amount = parseFloat(document.getElementById('txAmount').value);
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
    type: 'EXPENSE',
    category,
    transactionDate,
    paymentMethod,
    notes
  };

  const saveBtn = document.getElementById('saveExpenseBtn');
  const originalText = saveBtn.innerHTML;
  saveBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Saving...';
  saveBtn.disabled = true;

  try {
    if (currentEditingId) {
      payload.id = currentEditingId;
      await API.put('/api/transactions', payload);
      API.showToast('Expense updated successfully', 'success');
    } else {
      await API.post('/api/transactions', payload);
      API.showToast('Expense recorded successfully', 'success');
    }

    closeExpenseModal();
    loadExpenses();
  } catch (err) {
    API.showToast(err.message || 'Failed to save expense', 'error');
  } finally {
    saveBtn.innerHTML = originalText;
    saveBtn.disabled = false;
  }
}

function confirmDeleteExpense(id, title) {
  pendingDeleteId = id;
  document.getElementById('deleteTxTitle').textContent = `"${title}"`;
  document.getElementById('deleteConfirmModal').classList.add('open');
}

function closeDeleteModal() {
  document.getElementById('deleteConfirmModal').classList.remove('open');
  pendingDeleteId = null;
}

async function executeDeleteExpense() {
  if (!pendingDeleteId) return;

  const btn = document.getElementById('confirmDeleteBtn');
  const orig = btn.innerHTML;
  btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Deleting...';
  btn.disabled = true;

  try {
    await API.delete(`/api/transactions?id=${pendingDeleteId}`);
    API.showToast('Expense deleted successfully', 'success');
    closeDeleteModal();
    loadExpenses();
  } catch (err) {
    API.showToast('Failed to delete expense: ' + err.message, 'error');
  } finally {
    btn.innerHTML = orig;
    btn.disabled = false;
  }
}

function exportExpensesCSV() {
  if (!cachedExpenses || !cachedExpenses.length) {
    API.showToast('No expenses found to export', 'warning');
    return;
  }

  const exportData = cachedExpenses.map(e => ({
    ID: e.id,
    Title: e.title,
    'Amount (INR)': e.amount,
    Category: e.category,
    Date: e.transactionDate,
    PaymentMethod: e.paymentMethod,
    Notes: e.notes || ''
  }));

  API.exportToCSV(exportData, `expenses_${new Date().toISOString().split('T')[0]}.csv`);
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function escapeQuotes(str) {
  if (!str) return '';
  return str.replace(/'/g, "\\'").replace(/"/g, '&quot;');
}
