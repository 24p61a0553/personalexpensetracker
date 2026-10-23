/**
 * Personal Expense Tracker - Income Management Controller
 */

let cachedIncomes = [];
let cachedCategories = [];
let currentEditingId = null;
let pendingDeleteId = null;

document.addEventListener('DOMContentLoaded', () => {
  API.initPersonProfile();
  initDatePresets();
  loadIncomeCategories();
  loadIncomes();
  setupEventListeners();
});

function initDatePresets() {
  const dateInput = document.getElementById('txDate');
  if (dateInput && !dateInput.value) {
    dateInput.value = new Date().toISOString().split('T')[0];
  }
}

async function loadIncomeCategories() {
  try {
    const res = await API.get('/api/categories?type=INCOME');
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
        modalSelect.innerHTML = '<option value="">-- Select Source --</option>';
        cachedCategories.forEach(c => {
          const opt = document.createElement('option');
          opt.value = c.name;
          opt.textContent = c.name;
          modalSelect.appendChild(opt);
        });
      }
    }
  } catch (err) {
    console.warn('Could not load income categories:', err);
  }
}

async function loadIncomes() {
  const search = document.getElementById('searchInput')?.value.trim() || '';
  const category = document.getElementById('filterCategory')?.value || '';
  const startDate = document.getElementById('startDate')?.value || '';
  const endDate = document.getElementById('endDate')?.value || '';

  const params = new URLSearchParams({ type: 'INCOME' });
  if (search) params.append('search', search);
  if (category) params.append('category', category);
  if (startDate) params.append('startDate', startDate);
  if (endDate) params.append('endDate', endDate);

  try {
    const res = await API.get(`/api/transactions?${params.toString()}`);
    if (res && res.data) {
      cachedIncomes = res.data;
      renderIncomesTable(cachedIncomes);
      updateIncomeStats(cachedIncomes);
    }
  } catch (err) {
    console.error('Failed to load incomes:', err);
    API.showToast('Failed to load income records: ' + err.message, 'error');
  }
}

function updateIncomeStats(incomes) {
  const total = incomes.reduce((sum, e) => sum + parseFloat(e.amount || 0), 0);
  const count = incomes.length;
  const avg = count > 0 ? total / count : 0;

  const catSums = {};
  incomes.forEach(e => {
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

  document.getElementById('statTotalIncome').textContent = API.formatCurrency(total);
  document.getElementById('statIncomeCount').textContent = count;
  document.getElementById('statAvgIncome').textContent = API.formatCurrency(avg);
  document.getElementById('statTopSource').textContent = topCat;
}

function renderIncomesTable(incomes) {
  const tbody = document.getElementById('incomesTbody');
  const emptyState = document.getElementById('incomesEmptyState');
  if (!tbody) return;

  tbody.innerHTML = '';

  if (!incomes || incomes.length === 0) {
    if (emptyState) emptyState.style.display = 'block';
    return;
  }

  if (emptyState) emptyState.style.display = 'none';

  incomes.forEach(t => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>
        <div style="font-weight: 600; color: #f8fafc;">${escapeHtml(t.title)}</div>
        <div style="font-size: 0.78rem; color: #94a3b8;">${escapeHtml(t.paymentMethod || 'Bank Transfer')}${t.notes ? ' &bull; ' + escapeHtml(t.notes) : ''}</div>
      </td>
      <td>
        <span class="badge badge-category" style="color: var(--income); border-color: rgba(16, 185, 129, 0.3);">
          <i class="fas fa-arrow-down" style="font-size: 0.7rem;"></i> ${escapeHtml(t.category)}
        </span>
      </td>
      <td>${API.formatDate(t.transactionDate)}</td>
      <td class="amount-income" style="font-size: 0.95rem;">
        +${API.formatCurrency(t.amount)}
      </td>
      <td>
        <div class="action-buttons">
          <button class="btn-icon" title="Edit Income" onclick="openEditIncomeModal(${t.id})">
            <i class="fas fa-edit"></i>
          </button>
          <button class="btn-icon delete" title="Delete Income" onclick="confirmDeleteIncome(${t.id}, '${escapeQuotes(t.title)}')">
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
      timeout = setTimeout(loadIncomes, 300);
    });
  }

  const catFilter = document.getElementById('filterCategory');
  if (catFilter) catFilter.addEventListener('change', loadIncomes);

  const startFilter = document.getElementById('startDate');
  if (startFilter) startFilter.addEventListener('change', loadIncomes);

  const endFilter = document.getElementById('endDate');
  if (endFilter) endFilter.addEventListener('change', loadIncomes);

  const quickFilter = document.getElementById('quickDateFilter');
  if (quickFilter) {
    quickFilter.addEventListener('change', (e) => {
      applyQuickDatePreset(e.target.value);
    });
  }

  const form = document.getElementById('incomeForm');
  if (form) form.addEventListener('submit', handleSaveIncome);

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
  loadIncomes();
}

function openAddIncomeModal() {
  currentEditingId = null;
  document.getElementById('modalTitle').innerHTML = '<i class="fas fa-plus-circle" style="color: var(--income);"></i> Record New Income';
  document.getElementById('incomeForm').reset();
  const dateInput = document.getElementById('txDate');
  if (dateInput) dateInput.value = new Date().toISOString().split('T')[0];
  document.getElementById('incomeModal').classList.add('open');
}

async function openEditIncomeModal(id) {
  currentEditingId = id;
  document.getElementById('modalTitle').innerHTML = '<i class="fas fa-edit" style="color: var(--secondary);"></i> Edit Income';

  try {
    const res = await API.get(`/api/transactions?id=${id}`);
    if (!res || !res.data) throw new Error('Income record not found');

    const t = res.data;
    document.getElementById('txTitle').value = t.title || '';
    document.getElementById('txAmount').value = t.amount || '';
    document.getElementById('txCategory').value = t.category || '';
    document.getElementById('txDate').value = t.transactionDate || '';
    document.getElementById('txPayment').value = t.paymentMethod || 'Bank Transfer';
    document.getElementById('txNotes').value = t.notes || '';

    document.getElementById('incomeModal').classList.add('open');
  } catch (err) {
    API.showToast('Could not load income: ' + err.message, 'error');
  }
}

function closeIncomeModal() {
  document.getElementById('incomeModal').classList.remove('open');
  currentEditingId = null;
}

async function handleSaveIncome(e) {
  e.preventDefault();

  const title = document.getElementById('txTitle').value.trim();
  const amount = parseFloat(document.getElementById('txAmount').value);
  const category = document.getElementById('txCategory').value;
  const transactionDate = document.getElementById('txDate').value;
  const paymentMethod = document.getElementById('txPayment').value;
  const notes = document.getElementById('txNotes').value.trim();

  if (!title) {
    API.showToast('Please provide a source description/title', 'warning');
    return;
  }
  if (isNaN(amount) || amount <= 0) {
    API.showToast('Amount must be a positive number', 'warning');
    return;
  }
  if (!category) {
    API.showToast('Please select an income source', 'warning');
    return;
  }
  if (!transactionDate) {
    API.showToast('Please select a date', 'warning');
    return;
  }

  const payload = {
    title,
    amount,
    type: 'INCOME',
    category,
    transactionDate,
    paymentMethod,
    notes
  };

  const saveBtn = document.getElementById('saveIncomeBtn');
  const originalText = saveBtn.innerHTML;
  saveBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Saving...';
  saveBtn.disabled = true;

  try {
    if (currentEditingId) {
      payload.id = currentEditingId;
      await API.put('/api/transactions', payload);
      API.showToast('Income record updated successfully', 'success');
    } else {
      await API.post('/api/transactions', payload);
      API.showToast('Income recorded successfully', 'success');
    }

    closeIncomeModal();
    loadIncomes();
  } catch (err) {
    API.showToast(err.message || 'Failed to save income', 'error');
  } finally {
    saveBtn.innerHTML = originalText;
    saveBtn.disabled = false;
  }
}

function confirmDeleteIncome(id, title) {
  pendingDeleteId = id;
  document.getElementById('deleteTxTitle').textContent = `"${title}"`;
  document.getElementById('deleteConfirmModal').classList.add('open');
}

function closeDeleteModal() {
  document.getElementById('deleteConfirmModal').classList.remove('open');
  pendingDeleteId = null;
}

async function executeDeleteIncome() {
  if (!pendingDeleteId) return;

  const btn = document.getElementById('confirmDeleteBtn');
  const orig = btn.innerHTML;
  btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Deleting...';
  btn.disabled = true;

  try {
    await API.delete(`/api/transactions?id=${pendingDeleteId}`);
    API.showToast('Income record deleted successfully', 'success');
    closeDeleteModal();
    loadIncomes();
  } catch (err) {
    API.showToast('Failed to delete income: ' + err.message, 'error');
  } finally {
    btn.innerHTML = orig;
    btn.disabled = false;
  }
}

function exportIncomeCSV() {
  if (!cachedIncomes || !cachedIncomes.length) {
    API.showToast('No income records found to export', 'warning');
    return;
  }

  const exportData = cachedIncomes.map(e => ({
    ID: e.id,
    Title: e.title,
    'Amount (INR)': e.amount,
    Category: e.category,
    Date: e.transactionDate,
    PaymentMethod: e.paymentMethod,
    Notes: e.notes || ''
  }));

  API.exportToCSV(exportData, `income_${new Date().toISOString().split('T')[0]}.csv`);
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function escapeQuotes(str) {
  if (!str) return '';
  return str.replace(/'/g, "\\'").replace(/"/g, '&quot;');
}
