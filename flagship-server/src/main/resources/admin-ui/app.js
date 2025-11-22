const API_BASE = 'http://localhost:8080';

let token = localStorage.getItem('token');
let currentProjectId = null;

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    if (token) {
        showDashboard();
    } else {
        showLogin();
    }
    
    setupEventListeners();
});

function setupEventListeners() {
    document.getElementById('login-form').addEventListener('submit', handleLogin);
    document.getElementById('register-form').addEventListener('submit', handleRegister);
    document.getElementById('register-link').addEventListener('click', (e) => {
        e.preventDefault();
        document.getElementById('login-form').style.display = 'none';
        document.getElementById('register-form').style.display = 'block';
    });
    document.getElementById('login-link').addEventListener('click', (e) => {
        e.preventDefault();
        document.getElementById('register-form').style.display = 'none';
        document.getElementById('login-form').style.display = 'block';
    });
    document.getElementById('logout-btn').addEventListener('click', handleLogout);
    document.getElementById('create-project-btn').addEventListener('click', showCreateProjectModal);
    document.querySelectorAll('.tab').forEach(tab => {
        tab.addEventListener('click', () => switchTab(tab.dataset.tab));
    });
    document.getElementById('create-flag-btn').addEventListener('click', showCreateFlagModal);
    document.getElementById('create-experiment-btn').addEventListener('click', showCreateExperimentModal);
    document.getElementById('create-api-key-btn').addEventListener('click', showCreateApiKeyModal);
    document.querySelector('.close').addEventListener('click', closeModal);
}

async function handleLogin(e) {
    e.preventDefault();
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    
    try {
        const response = await fetch(`${API_BASE}/api/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });
        
        if (response.ok) {
            const data = await response.json();
            token = data.token;
            localStorage.setItem('token', token);
            showDashboard();
        } else {
            alert('Ошибка входа');
        }
    } catch (error) {
        alert('Ошибка: ' + error.message);
    }
}

async function handleRegister(e) {
    e.preventDefault();
    const name = document.getElementById('reg-name').value;
    const email = document.getElementById('reg-email').value;
    const password = document.getElementById('reg-password').value;
    
    try {
        const response = await fetch(`${API_BASE}/api/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password, name })
        });
        
        if (response.ok) {
            const data = await response.json();
            token = data.token;
            localStorage.setItem('token', token);
            showDashboard();
        } else {
            alert('Ошибка регистрации');
        }
    } catch (error) {
        alert('Ошибка: ' + error.message);
    }
}

function handleLogout() {
    token = null;
    localStorage.removeItem('token');
    showLogin();
}

function showLogin() {
    document.getElementById('login-screen').style.display = 'block';
    document.getElementById('dashboard-screen').style.display = 'none';
}

function showDashboard() {
    document.getElementById('login-screen').style.display = 'none';
    document.getElementById('dashboard-screen').style.display = 'block';
    loadProjects();
}

async function loadProjects() {
    try {
        const response = await fetch(`${API_BASE}/api/admin/projects`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (response.ok) {
            const projects = await response.json();
            renderProjects(projects);
        }
    } catch (error) {
        console.error('Error loading projects:', error);
    }
}

function renderProjects(projects) {
    const list = document.getElementById('projects-list');
    list.innerHTML = '';
    
    projects.forEach(project => {
        const li = document.createElement('li');
        li.textContent = project.name;
        li.addEventListener('click', () => selectProject(project));
        list.appendChild(li);
    });
}

function selectProject(project) {
    currentProjectId = project.id;
    document.querySelectorAll('#projects-list li').forEach(li => li.classList.remove('active'));
    event.target.classList.add('active');
    
    document.getElementById('project-name').textContent = project.name;
    document.getElementById('project-details').style.display = 'block';
    document.getElementById('empty-state').style.display = 'none';
    
    loadFlags();
    loadExperiments();
    loadApiKeys();
}

async function loadFlags() {
    if (!currentProjectId) return;
    
    try {
        const response = await fetch(`${API_BASE}/api/projects/${currentProjectId}/flags`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (response.ok) {
            const flags = await response.json();
            renderFlags(flags);
        }
    } catch (error) {
        console.error('Error loading flags:', error);
    }
}

function renderFlags(flags) {
    const tbody = document.getElementById('flags-tbody');
    tbody.innerHTML = '';
    
    Object.entries(flags).forEach(([key, flag]) => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${key}</td>
            <td>${flag.type}</td>
            <td>${JSON.stringify(flag.value)}</td>
            <td><span class="status">Active</span></td>
            <td>
                <button class="btn-danger" onclick="deleteFlag('${key}')">Удалить</button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

async function loadExperiments() {
    if (!currentProjectId) return;
    
    try {
        const response = await fetch(`${API_BASE}/api/projects/${currentProjectId}/experiments`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (response.ok) {
            const experiments = await response.json();
            renderExperiments(experiments);
        }
    } catch (error) {
        console.error('Error loading experiments:', error);
    }
}

function renderExperiments(experiments) {
    const tbody = document.getElementById('experiments-tbody');
    tbody.innerHTML = '';
    
    Object.entries(experiments).forEach(([key, exp]) => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${key}</td>
            <td>${exp.variants.map(v => v.name).join(', ')}</td>
            <td><span class="status">Active</span></td>
            <td>
                <button class="btn-danger" onclick="deleteExperiment('${key}')">Удалить</button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

async function loadApiKeys() {
    if (!currentProjectId) return;
    
    try {
        const response = await fetch(`${API_BASE}/api/admin/projects/${currentProjectId}/api-keys`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (response.ok) {
            const keys = await response.json();
            renderApiKeys(keys);
        }
    } catch (error) {
        console.error('Error loading API keys:', error);
    }
}

function renderApiKeys(keys) {
    const tbody = document.getElementById('api-keys-tbody');
    tbody.innerHTML = '';
    
    keys.forEach(key => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${key.name}</td>
            <td>${key.type}</td>
            <td>${new Date(key.createdAt * 1000).toLocaleDateString()}</td>
            <td>
                <button class="btn-danger" onclick="deleteApiKey('${key.id}')">Удалить</button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function switchTab(tabName) {
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
    
    document.querySelector(`[data-tab="${tabName}"]`).classList.add('active');
    document.getElementById(`${tabName}-tab`).classList.add('active');
}

function showCreateProjectModal() {
    const content = `
        <h2>Создать проект</h2>
        <form id="create-project-form">
            <input type="text" id="project-name-input" placeholder="Название" required>
            <input type="text" id="project-slug-input" placeholder="Slug" required>
            <textarea id="project-desc-input" placeholder="Описание"></textarea>
            <button type="submit" class="btn-primary">Создать</button>
        </form>
    `;
    showModal(content);
    
    document.getElementById('create-project-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const name = document.getElementById('project-name-input').value;
        const slug = document.getElementById('project-slug-input').value;
        const description = document.getElementById('project-desc-input').value;
        
        try {
            const response = await fetch(`${API_BASE}/api/admin/projects`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ name, slug, description })
            });
            
            if (response.ok) {
                closeModal();
                loadProjects();
            } else {
                const errorData = await response.json().catch(() => ({ error: 'Unknown error' }));
                alert('Ошибка создания проекта: ' + (errorData.error || response.statusText));
            }
        } catch (error) {
            alert('Ошибка: ' + error.message);
        }
    });
}

function showCreateFlagModal() {
    const content = `
        <h2>Создать флаг</h2>
        <form id="create-flag-form">
            <input type="text" id="flag-key-input" placeholder="Ключ" required>
            <select id="flag-type-input" required>
                <option value="bool">Boolean</option>
                <option value="string">String</option>
                <option value="number">Number</option>
            </select>
            <input type="text" id="flag-value-input" placeholder="Значение" required>
            <button type="submit" class="btn-primary">Создать</button>
        </form>
    `;
    showModal(content);
    
    document.getElementById('create-flag-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const key = document.getElementById('flag-key-input').value;
        const type = document.getElementById('flag-type-input').value;
        const value = document.getElementById('flag-value-input').value;
        
        let flagValue;
        if (type === 'bool') {
            flagValue = { type: 'bool', value: value === 'true' };
        } else if (type === 'number') {
            flagValue = { type: 'number', value: parseFloat(value) };
        } else {
            flagValue = { type: 'string', value: value };
        }
        
        try {
            const response = await fetch(`${API_BASE}/api/projects/${currentProjectId}/flags`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ [key]: flagValue })
            });
            
            if (response.ok) {
                closeModal();
                loadFlags();
            }
        } catch (error) {
            alert('Ошибка: ' + error.message);
        }
    });
}

function showCreateExperimentModal() {
    // Simplified for MVP
    alert('Создание экспериментов будет добавлено в следующей версии');
}

function showCreateApiKeyModal() {
    const content = `
        <h2>Создать API ключ</h2>
        <form id="create-api-key-form">
            <input type="text" id="api-key-name-input" placeholder="Название" required>
            <select id="api-key-type-input" required>
                <option value="READ_ONLY">Read Only</option>
                <option value="ADMIN">Admin</option>
            </select>
            <button type="submit" class="btn-primary">Создать</button>
        </form>
    `;
    showModal(content);
    
    document.getElementById('create-api-key-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const name = document.getElementById('api-key-name-input').value;
        const type = document.getElementById('api-key-type-input').value;
        
        try {
            const response = await fetch(`${API_BASE}/api/admin/projects/${currentProjectId}/api-keys`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ name, type })
            });
            
            if (response.ok) {
                const data = await response.json();
                alert(`API ключ создан: ${data.key}\nСохраните его, он больше не будет показан!`);
                closeModal();
                loadApiKeys();
            }
        } catch (error) {
            alert('Ошибка: ' + error.message);
        }
    });
}

function showModal(content) {
    document.getElementById('modal-content').innerHTML = content;
    document.getElementById('modal-overlay').style.display = 'flex';
}

function closeModal() {
    document.getElementById('modal-overlay').style.display = 'none';
}

async function deleteFlag(key) {
    if (!confirm(`Удалить флаг ${key}?`)) return;
    
    try {
        const response = await fetch(`${API_BASE}/api/projects/${currentProjectId}/flags/${key}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (response.ok) {
            loadFlags();
        }
    } catch (error) {
        alert('Ошибка: ' + error.message);
    }
}

async function deleteExperiment(key) {
    if (!confirm(`Удалить эксперимент ${key}?`)) return;
    
    try {
        const response = await fetch(`${API_BASE}/api/projects/${currentProjectId}/experiments/${key}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (response.ok) {
            loadExperiments();
        }
    } catch (error) {
        alert('Ошибка: ' + error.message);
    }
}

