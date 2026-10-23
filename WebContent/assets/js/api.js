/**
 * Personal Expense Tracker - Core API Client & Shared Utilities
 */

const API = {
  // Automatically detects servlet context path (e.g. /PersonalExpenseTracker or /)
  getBaseUrl() {
    const path = window.location.pathname;
    const segments = path.split('/').filter(Boolean);
    if (segments.length > 0 && !segments[0].endsWith('.html') && segments[0] !== 'api') {
      return `/${segments[0]}`;
    }
    return '';
  },

  async request(endpoint, options = {}) {
    const url = `${this.getBaseUrl()}${endpoint}`;
    const defaultHeaders = {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };

    const config = {
      ...options,
      headers: {
        ...defaultHeaders,
        ...(options.headers || {})
      }
    };

    try {
      const response = await fetch(url, config);
      const data = await response.json().catch(() => null);

      if (!response.ok) {
        throw new Error(data?.message || `HTTP ${response.status}: Request failed`);
      }

      return data;
    } catch (err) {
      console.error(`[API Error] ${endpoint}:`, err);
      throw err;
    }
  },

  get(endpoint) {
    return this.request(endpoint, { method: 'GET' });
  },

  post(endpoint, body) {
    return this.request(endpoint, {
      method: 'POST',
      body: JSON.stringify(body)
    });
  },

  put(endpoint, body) {
    return this.request(endpoint, {
      method: 'PUT',
      body: JSON.stringify(body)
    });
  },

  delete(endpoint, body = null) {
    return this.request(endpoint, {
      method: 'DELETE',
      body: body ? JSON.stringify(body) : undefined
    });
  },

  // Currency Formatter: Indian Rupee (₹) with Indian number format (e.g. ₹1,25,000.00)
  formatCurrency(amount) {
    const num = parseFloat(amount || 0);
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(num);
  },

  async getProfile() {
    return this.get('/api/profile');
  },

  async saveProfile(personName) {
    return this.post('/api/profile', { personName });
  },

  // Initialize and synchronize Person Name across the application
  async initPersonProfile() {
    const nameInput = document.getElementById('personNameInput');
    const saveBtn = document.getElementById('savePersonNameBtn');
    const displayEls = document.querySelectorAll('.display-person-name, #displayPersonName, #sidebarPersonName');

    const updateDisplays = (name) => {
      const isSaved = name && name.trim() && name.trim() !== 'Enter your name';
      const val = isSaved ? name.trim() : 'Srinivas';
      displayEls.forEach(el => {
        el.textContent = val;
        el.classList.remove('not-set');
      });
      if (nameInput) {
        nameInput.value = val;
      }
    };

    try {
      const res = await this.getProfile();
      if (res && res.data && res.data.personName) {
        updateDisplays(res.data.personName);
      } else {
        updateDisplays('Srinivas');
      }
    } catch (e) {
      console.warn('Could not load profile from server:', e);
      updateDisplays('Srinivas');
    }

    if (saveBtn && nameInput) {
      saveBtn.addEventListener('click', async () => {
        const entered = nameInput.value.trim();
        if (!entered) {
          API.showToast('Please enter your name before saving.', 'warning');
          nameInput.focus();
          return;
        }

        const originalHtml = saveBtn.innerHTML;
        saveBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Saving...';
        saveBtn.disabled = true;

        try {
          const res = await API.saveProfile(entered);
          if (res && res.success) {
            updateDisplays(res.data?.personName || entered);
            API.showToast('Person name saved successfully to database!', 'success');
          } else {
            throw new Error(res?.message || 'Failed to save name');
          }
        } catch (err) {
          console.error('Error saving name:', err);
          API.showToast(err.message || 'Database error: Could not save person name.', 'error');
        } finally {
          saveBtn.innerHTML = originalHtml;
          saveBtn.disabled = false;
        }
      });

      nameInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
          e.preventDefault();
          saveBtn.click();
        }
      });
    }
  },

  // Date Formatter: "Oct 24, 2026"
  formatDate(dateStr) {
    if (!dateStr) return '—';
    try {
      const [year, month, day] = dateStr.split('-');
      const date = new Date(year, parseInt(month) - 1, day);
      return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      });
    } catch {
      return dateStr;
    }
  },

  // Toast Notification System
  showToast(message, type = 'info') {
    let container = document.querySelector('.toast-container');
    if (!container) {
      container = document.createElement('div');
      container.className = 'toast-container';
      document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;

    let iconClass = 'fa-info-circle';
    if (type === 'success') iconClass = 'fa-check-circle';
    if (type === 'error') iconClass = 'fa-exclamation-triangle';
    if (type === 'warning') iconClass = 'fa-exclamation-circle';

    toast.innerHTML = `
      <i class="fas ${iconClass}"></i>
      <span class="toast-text">${message}</span>
      <button class="toast-close" onclick="this.parentElement.remove()">&times;</button>
    `;

    container.appendChild(toast);

    setTimeout(() => {
      toast.classList.add('toast-hiding');
      setTimeout(() => toast.remove(), 300);
    }, 4000);
  },

  // Export array of objects to CSV
  exportToCSV(data, filename = 'transactions.csv') {
    if (!data || !data.length) {
      API.showToast('No data available to export', 'warning');
      return;
    }

    const headers = Object.keys(data[0]);
    const csvRows = [headers.join(',')];

    for (const row of data) {
      const values = headers.map(header => {
        const val = row[header] === null || row[header] === undefined ? '' : String(row[header]);
        const escaped = val.replace(/"/g, '""');
        return `"${escaped}"`;
      });
      csvRows.push(values.join(','));
    }

    const blob = new Blob([csvRows.join('\n')], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute('download', filename);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    API.showToast(`Exported ${data.length} records to ${filename}`, 'success');
  }
};
