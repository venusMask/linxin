// app.js - 邻信管理控制台
console.log('邻信控制台已加载');
let currentUser = null;
let token = localStorage.getItem('token');
const contextPath = '/lxa';

// 初始化
document.addEventListener('DOMContentLoaded', () => {
    if (token) {
        verifyTokenAndInit();
    } else {
        showSection('auth');
    }
});

function showSection(name) {
    const sections = ['auth', 'tokens', 'usage', 'profile'];
    sections.forEach(s => {
        const el = document.getElementById(`${s}-section`);
        if (el) {
            if (s === name) {
                el.classList.remove('hidden');
            } else {
                el.classList.add('hidden');
            }
        }
        
        // 更新侧边栏导航高亮
        const nav = document.getElementById(`nav-${s}`);
        if (nav) {
            if (s === name) {
                nav.classList.add('active');
            } else {
                nav.classList.remove('active');
            }
        }
    });

    if (name === 'tokens') loadTokens();
    if (name === 'usage') loadUsageStats();
    if (name === 'profile') renderProfile();
}

function toggleAuth(type) {
    if (type === 'login') {
        document.getElementById('login-form').classList.remove('hidden');
        document.getElementById('register-form').classList.add('hidden');
    } else {
        document.getElementById('login-form').classList.add('hidden');
        document.getElementById('register-form').classList.remove('hidden');
    }
}

// --- 统计逻辑 ---
let dailyChart = null;
let intentChart = null;

async function loadUsageStats() {
    const days = document.getElementById('usage-days').value;
    try {
        const data = await apiFetch(`/ai/usage?days=${days}`);
        renderDailyChart(data.daily);
        renderIntentChart(data.intents);
    } catch (err) { console.error(err); }
}

function renderDailyChart(items) {
    const ctx = document.getElementById('dailyChart').getContext('2d');
    if (dailyChart) dailyChart.destroy();
    
    dailyChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: items.map(i => i.date),
            datasets: [{
                label: 'Token 消耗总量',
                data: items.map(i => i.totalTokens),
                borderColor: '#10b981',
                backgroundColor: 'rgba(16, 185, 129, 0.1)',
                fill: true,
                tension: 0.3,
                pointRadius: 4,
                pointBackgroundColor: '#10b981'
            }]
        },
        options: { 
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false }
            },
            scales: {
                y: { beginAtZero: true, grid: { color: '#f3f4f6' } },
                x: { grid: { display: false } }
            }
        }
    });
}

function renderIntentChart(items) {
    const ctx = document.getElementById('intentChart').getContext('2d');
    if (intentChart) intentChart.destroy();
    
    intentChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: items.map(i => i.intent || '默认对话'),
            datasets: [{
                data: items.map(i => i.totalTokens),
                backgroundColor: ['#10b981', '#3b82f6', '#8b5cf6', '#f59e0b', '#ef4444'],
                borderWidth: 0
            }]
        },
        options: { 
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { position: 'bottom', labels: { boxWidth: 12, usePointStyle: true } }
            }
        }
    });
}

// --- API 封装 ---
async function apiFetch(url, options = {}) {
    const fullUrl = url.startsWith('http') ? url : `${contextPath}${url}`;
    const headers = {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': `Bearer ${token}` } : {})
    };
    try {
        const response = await fetch(fullUrl, { ...options, headers });
        const contentType = response.headers.get('content-type');
        
        if (!response.ok) {
            if (response.status === 401) {
                logout();
                throw new Error('会话过期，请重新登录');
            }
            if (contentType && contentType.includes('application/json')) {
                const errResult = await response.json();
                throw new Error(errResult.message || `请求失败: ${response.status}`);
            }
            throw new Error(`系统错误: ${response.status}`);
        }

        const result = await response.json();
        if (result.code !== 200 && result.code !== 0) {
            throw new Error(result.message || '操作失败');
        }
        return result.data;
    } catch (err) {
        console.error('API Error:', err);
        throw err;
    }
}

// --- 认证逻辑 ---
async function handleLogin(e) {
    e.preventDefault();
    const formData = new FormData(e.target);
    const data = Object.fromEntries(formData);
    try {
        const res = await apiFetch('/auth/login', {
            method: 'POST',
            body: JSON.stringify(data)
        });
        token = res.token;
        localStorage.setItem('token', token);
        verifyTokenAndInit();
    } catch (err) {
        alert(err.message);
    }
}

async function handleRegister(e) {
    e.preventDefault();
    const data = {
        username: document.getElementById('reg-username').value,
        password: document.getElementById('reg-password').value,
        nickname: document.getElementById('reg-nickname').value,
        email: document.getElementById('reg-email').value,
        verificationCode: document.getElementById('reg-code').value
    };
    try {
        await apiFetch('/auth/register', {
            method: 'POST',
            body: JSON.stringify(data)
        });
        alert('注册成功，请登录');
        toggleAuth('login');
    } catch (err) {
        alert(err.message);
    }
}

async function sendEmailCode() {
    const email = document.getElementById('reg-email').value;
    if (!email) return alert('请输入邮箱');
    try {
        await apiFetch('/auth/email/send-code', {
            method: 'POST',
            body: JSON.stringify({ email })
        });
        alert('验证码已发送');
    } catch (err) {
        alert(err.message);
    }
}

function logout() {
    token = null;
    currentUser = null;
    localStorage.removeItem('token');
    
    // 恢复登录页布局
    document.getElementById('app-body').classList.add('auth-page');
    document.getElementById('sidebar').classList.add('hidden');
    
    showSection('auth');
}

async function verifyTokenAndInit() {
    try {
        currentUser = await apiFetch('/auth/userinfo');
        
        // 切换到管理端布局
        document.getElementById('app-body').classList.remove('auth-page');
        document.getElementById('sidebar').classList.remove('hidden');
        
        showSection('tokens'); 
    } catch (err) {
        logout();
    }
}

// --- Token 管理 ---
async function loadTokens() {
    try {
        const tokens = await apiFetch('/api/agent/tokens');
        const tbody = document.getElementById('tokens-tbody');
        tbody.innerHTML = '';
        const now = new Date();
        
        tokens.forEach(t => {
            const tr = document.createElement('tr');
            const expireDate = t.expireTime ? new Date(t.expireTime) : null;
            const isExpired = expireDate && expireDate < now;
            
            const statusHtml = isExpired 
                ? '<mark class="status" style="background: #fee2e2; color: #dc2626;">已过期</mark>' 
                : '<mark class="status" style="background: #dcfce7; color: #16a34a;">启用中</mark>';
            
            const maskedToken = `${t.token.substring(0, 10)}...`;

            tr.innerHTML = `
                <td style="font-weight: 500;">${t.agentName}</td>
                <td>
                    <code style="font-size: 0.85rem; background: #f3f4f6; padding: 0.2rem 0.4rem; border-radius: 4px;">${maskedToken}</code>
                    <a href="#" onclick="copyText('${t.token}', 'Token前缀')" title="点击复制" style="text-decoration: none; margin-left: 4px;">
                        <i class="far fa-copy"></i>
                    </a>
                </td>
                <td>${statusHtml}</td>
                <td style="color: #6b7280; font-size: 0.9rem;">${expireDate ? expireDate.toLocaleDateString() : '永久有效'}</td>
                <td style="text-align: right;">
                    <button class="outline secondary" onclick="revokeToken(${t.id})" style="padding: 0.25rem 0.75rem; font-size: 0.8rem; margin: 0;">撤销</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) { console.error(err); }
}

function copyText(text, label = '内容') {
    navigator.clipboard.writeText(text).then(() => {
        alert(`${label}已复制到剪贴板`);
    });
}

function showGenerateTokenModal() {
    document.getElementById('token-modal').showModal();
}

function closeTokenModal() {
    document.getElementById('token-modal').close();
}

async function handleGenerateToken(e) {
    e.preventDefault();
    const formData = new FormData(e.target);
    const data = {
        agentName: formData.get('agentName'),
        expireDays: formData.get('expireDays') ? parseInt(formData.get('expireDays'), 10) : null,
        scopes: formData.getAll('scope').join(',') || 'none'
    };
    
    try {
        const res = await apiFetch('/api/agent/tokens/generate', {
            method: 'POST',
            body: JSON.stringify(data)
        });
        copyText(res.token, '完整Token');
        alert(`生成成功！\n\n您的完整Token为：\n${res.token}\n\n请务必妥善保存，关闭后将无法再次查看全文。`);
        closeTokenModal();
        loadTokens();
    } catch (err) { alert(err.message); }
}

async function revokeToken(id) {
    if (!confirm('确定要撤销此令牌吗？撤销后，正在使用该令牌的 Agent 将无法访问您的数据。')) return;
    try {
        await apiFetch(`/api/agent/tokens/${id}`, { method: 'DELETE' });
        loadTokens();
    } catch (err) { alert(err.message); }
}

// --- 个人资料 ---
function renderProfile() {
    if (!currentUser) return;
    document.getElementById('profile-username').value = currentUser.username || '';
    document.getElementById('profile-nickname').value = currentUser.nickname || '';
    document.getElementById('profile-email').value = currentUser.email || '未绑定';
    document.getElementById('profile-signature').value = currentUser.signature || '';
    document.getElementById('profile-gender').value = currentUser.gender || 0;
}

async function handleUpdateProfile(e) {
    e.preventDefault();
    const formData = new FormData(e.target);
    const data = Object.fromEntries(formData);
    data.gender = parseInt(data.gender, 10);
    
    const btn = document.getElementById('btn-save-profile');
    btn.setAttribute('aria-busy', 'true');
    btn.disabled = true;

    try {
        await apiFetch('/auth/profile', {
            method: 'PUT',
            body: JSON.stringify(data)
        });
        alert('个人资料保存成功');
        // 重新获取用户信息以更新全局状态
        currentUser = await apiFetch('/auth/userinfo');
        renderProfile();
    } catch (err) {
        alert(err.message);
    } finally {
        btn.setAttribute('aria-busy', 'false');
        btn.disabled = false;
    }
}

function showChangeEmailModal() {
    document.getElementById('email-modal').showModal();
}

function closeEmailModal() {
    document.getElementById('email-modal').close();
}

async function sendChangeEmailCode() {
    const email = document.getElementById('email-new-addr').value;
    if (!email) return alert('请输入新邮箱地址');
    
    const btn = document.getElementById('btn-send-email-code');
    btn.disabled = true;
    
    try {
        await apiFetch('/auth/email/send-code', {
            method: 'POST',
            body: JSON.stringify({ email, type: 'change_email' })
        });
        alert('验证码已发送至新邮箱');
        
        let countdown = 60;
        const timer = setInterval(() => {
            btn.textContent = `${countdown}s`;
            if (countdown <= 0) {
                clearInterval(timer);
                btn.textContent = '获取验证码';
                btn.disabled = false;
            }
            countdown--;
        }, 1000);
    } catch (err) {
        alert(err.message);
        btn.disabled = false;
    }
}

async function handleUpdateEmail(e) {
    e.preventDefault();
    const data = {
        password: document.getElementById('email-old-pwd').value,
        newEmail: document.getElementById('email-new-addr').value,
        code: document.getElementById('email-new-code').value
    };
    
    try {
        await apiFetch('/auth/email', {
            method: 'PUT',
            body: JSON.stringify(data)
        });
        alert('邮箱修改成功');
        closeEmailModal();
        // 重新获取用户信息以更新显示
        currentUser = await apiFetch('/auth/userinfo');
        renderProfile();
    } catch (err) {
        alert(err.message);
    }
}
