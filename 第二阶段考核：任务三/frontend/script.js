// API配置
const API_BASE = 'http://localhost:8080/api';

// 全局变量
let token = null;
let currentPet = null;
let currentBattleId = null;
let currentLevelId = null;
let selectedPetType = null;
let currentRoundSeq = 0;
let isInBattle = false;
let pendingReward = false;
let currentSkills = [];
let hpPotionCount = 0;
let ppPotionCount = 0;

// ========== 工具函数 ==========
function showMessage(elementId, text, isError = true) {
    const el = document.getElementById(elementId);
    if (el) {
        el.textContent = text;
        el.style.color = isError ? '#d9534f' : '#2d5a27';
        setTimeout(() => {
            if (el.textContent === text) el.textContent = '';
        }, 3000);
    }
}

async function request(method, url, body = null, needAuth = true) {
    const headers = {
        'Content-Type': 'application/json'
    };
    if (needAuth && token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const options = {
        method,
        headers
    };
    if (body) {
        options.body = JSON.stringify(body);
    }

    try {
        const response = await fetch(`${API_BASE}${url}`, options);
        const data = await response.json();
        if (data.code !== 200) {
            throw new Error(data.message || '请求失败');
        }
        return data;
    } catch (error) {
        console.error('请求错误:', error);
        throw error;
    }
}

// ========== 页面切换 ==========
function showPage(pageId) {
    document.querySelectorAll('.page').forEach(page => {
        page.classList.remove('active');
    });
    document.getElementById(`page-${pageId}`).classList.add('active');
}

// ========== 登录注册 ==========
function initAuth() {
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const tab = btn.dataset.tab;
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            document.querySelectorAll('.form-container').forEach(form => form.classList.remove('active'));
            document.getElementById(`${tab}-form`).classList.add('active');
        });
    });

    document.getElementById('register-btn').addEventListener('click', async () => {
        const username = document.getElementById('reg-username').value.trim();
        const email = document.getElementById('reg-email').value.trim();
        const phone = document.getElementById('reg-phone').value.trim();
        const password = document.getElementById('reg-password').value;
        const confirm = document.getElementById('reg-confirm').value;

        if (!username || !email || !phone || !password) {
            showMessage('login-message', '请填写所有字段');
            return;
        }
        if (password !== confirm) {
            showMessage('login-message', '两次密码不一致');
            return;
        }

        try {
            await request('POST', '/user/register', {
                username, email, phone, password, confirmPassword: confirm
            }, false);
            showMessage('login-message', '注册成功，请登录', false);
            document.querySelector('.tab-btn[data-tab="login"]').click();
        } catch (error) {
            showMessage('login-message', error.message);
        }
    });

    document.getElementById('login-btn').addEventListener('click', async () => {
        const account = document.getElementById('login-account').value.trim();
        const password = document.getElementById('login-password').value;

        if (!account || !password) {
            showMessage('login-message', '请填写账号和密码');
            return;
        }

        try {
            const data = await request('POST', '/user/login', { account, password }, false);
            token = data.token;
            localStorage.setItem('token', token);
            showMessage('login-message', '登录成功', false);
            await checkPetAndNavigate();
        } catch (error) {
            showMessage('login-message', error.message);
        }
    });
}

async function checkPetAndNavigate() {
    try {
        const data = await request('GET', '/pet/my');
        currentPet = data.data;
        await loadGameData();
        showPage('game');
    } catch (error) {
        showPage('create-pet');
        initPetSelection();
    }
}

// ========== 创建精灵 ==========
function initPetSelection() {
    selectedPetType = null;
    document.querySelectorAll('.pet-option').forEach(opt => {
        opt.classList.remove('selected');
        opt.addEventListener('click', () => {
            document.querySelectorAll('.pet-option').forEach(o => o.classList.remove('selected'));
            opt.classList.add('selected');
            selectedPetType = opt.dataset.type;
            document.getElementById('create-pet-btn').disabled = false;
        });
    });

    document.getElementById('create-pet-btn').onclick = async () => {
        if (!selectedPetType) return;

        let name = '';
        if (selectedPetType === 'WATER') name = '水蓝蓝';
        else if (selectedPetType === 'FIRE') name = '火花';
        else name = '喵喵';

        try {
            const data = await request('POST', '/pet/create', { name, petType: selectedPetType });
            currentPet = data.data;
            await loadGameData();
            showPage('game');
        } catch (error) {
            showMessage('create-message', error.message);
        }
    };
}

// ========== 更新精灵信息显示 ==========
async function updatePetInfo() {
    const petData = await request('GET', '/pet/my');
    currentPet = petData.data;

    const needExp = 100 + (currentPet.level - 1) * 50;
    const expPercent = (currentPet.exp / needExp) * 100;

    document.getElementById('pet-info').innerHTML = `
        <div><strong>${currentPet.name}</strong> Lv.${currentPet.level}</div>
        <div>❤️HP: ${currentPet.currentHp}/${currentPet.hp}</div>
        <div>⚔️攻击: ${currentPet.attack}</div>
        <div>🛡️防御: ${currentPet.defense}</div>
        <div class="pet-exp">经验: ${currentPet.exp}/${needExp}</div>
        <div class="exp-bar"><div class="exp-fill" style="width: ${expPercent}%;"></div></div>
    `;

    // 更新药剂数量显示
    hpPotionCount = currentPet.hpPotionCount || 0;
    ppPotionCount = currentPet.ppPotionCount || 0;
    const potionCountDiv = document.getElementById('potion-count');
    if (potionCountDiv) {
        potionCountDiv.innerHTML = `❤️HP药剂: ${hpPotionCount} 瓶 | 💙PP药剂: ${ppPotionCount} 瓶`;
    }
}

// ========== 游戏主界面 ==========
async function loadGameData() {
    await updatePetInfo();
    await loadLevelList();

    document.getElementById('logout-btn').onclick = () => {
        token = null;
        localStorage.removeItem('token');
        currentPet = null;
        currentBattleId = null;
        currentRoundSeq = 0;
        isInBattle = false;
        pendingReward = false;
        currentSkills = [];
        showPage('login');
    };

    document.getElementById('strategy-btn').onclick = async () => {
        if (!currentLevelId) {
            document.getElementById('strategy-text').innerHTML = '请先选择一个关卡';
            return;
        }
        try {
            const data = await request('GET', `/ai/strategy?levelId=${currentLevelId}`);
            document.getElementById('strategy-text').innerHTML = data.data;
        } catch (error) {
            document.getElementById('strategy-text').innerHTML = '获取策略失败';
        }
    };

    updateSkillButtons();
}

async function loadLevelList() {
    const data = await request('GET', '/level/list');
    const levels = data.data;
    const currentLevelNum = currentPet.currentLevel;

    const container = document.getElementById('level-list');
    container.innerHTML = '';

    for (const level of levels) {
        const div = document.createElement('div');
        div.className = 'level-item';
        if (level.levelNum > currentLevelNum) {
            div.classList.add('locked');
            div.innerHTML = `<span>第${level.levelNum}关 ${level.monsterName}</span><span>🔒未解锁</span>`;
        } else {
            div.innerHTML = `<span>第${level.levelNum}关 ${level.monsterName}</span><span>⚔️挑战</span>`;
            if (!isInBattle && !pendingReward) {
                div.onclick = () => selectLevel(level);
            }
        }
        container.appendChild(div);
    }
}

function selectLevel(level) {
    if (isInBattle) {
        document.getElementById('battle-logs').innerHTML = `<div>⚠️ 战斗中无法切换关卡！请先结束当前战斗。</div>`;
        return;
    }

    if (pendingReward) {
        document.getElementById('battle-logs').innerHTML = `<div>⚠️ 请先领取奖励！</div>`;
        return;
    }

    currentLevelId = level.id;
    currentBattleId = null;
    currentRoundSeq = 0;

    document.querySelectorAll('.level-item').forEach(item => {
        item.classList.remove('selected');
    });

    const levelItems = document.querySelectorAll('.level-item');
    for (let i = 0; i < levelItems.length; i++) {
        const item = levelItems[i];
        const match = item.querySelector('span:first-child').textContent.match(/\d+/);
        if (match) {
            const levelNum = parseInt(match[0]);
            if (levelNum === level.levelNum) {
                item.classList.add('selected');
                break;
            }
        }
    }

    document.getElementById('start-battle-btn').style.display = 'block';
    document.getElementById('start-battle-btn').innerHTML = '开始战斗';
    document.getElementById('battle-actions').style.display = 'none';
    document.getElementById('potion-actions').style.display = 'none';
    document.getElementById('potion-count').style.display = 'none';
    document.getElementById('claim-reward-btn').style.display = 'none';
    document.getElementById('battle-logs').innerHTML = `<div>准备挑战：${level.monsterName}</div>`;
    document.getElementById('battle-pet-name').innerHTML = '';
    document.getElementById('battle-pet-hp').innerHTML = '';
    document.getElementById('battle-monster-name').innerHTML = '';
    document.getElementById('battle-monster-hp').innerHTML = '';

    document.getElementById('start-battle-btn').onclick = async () => {
        await startBattle(level.id);
    };
}

async function startBattle(levelId) {
    try {
        const data = await request('POST', `/battle/start?levelId=${levelId}`);
        currentBattleId = data.data.battleId;
        currentRoundSeq = 0;
        isInBattle = true;
        pendingReward = false;

        // 更新药剂数量
        if (data.data.hpPotionCount !== undefined) {
            hpPotionCount = data.data.hpPotionCount;
            ppPotionCount = data.data.ppPotionCount;
            const potionCountDiv = document.getElementById('potion-count');
            if (potionCountDiv) {
                potionCountDiv.innerHTML = `❤️HP药剂: ${hpPotionCount} 瓶 | 💙PP药剂: ${ppPotionCount} 瓶`;
            }
        }

        // 初始化技能PP
        currentSkills = [];
        if (currentPet.petType === 'WATER') {
            currentSkills = [
                { id: 1, name: '水炮', type: '攻击', desc: '造成伤害', maxPp: -1, currentPp: -1 },
                { id: 2, name: '冥想', type: '状态', desc: '攻击力提升100%，可叠加', maxPp: 3, currentPp: 3 },
                { id: 3, name: '水幕', type: '防御', desc: '本回合受到伤害减少90%', maxPp: 3, currentPp: 3 }
            ];
        } else if (currentPet.petType === 'FIRE') {
            currentSkills = [
                { id: 4, name: '火花', type: '攻击', desc: '造成伤害', maxPp: -1, currentPp: -1 },
                { id: 5, name: '斗志', type: '状态', desc: '攻击力提升100%，可叠加', maxPp: 3, currentPp: 3 },
                { id: 6, name: '火焰护盾', type: '防御', desc: '减伤50%，反伤50%', maxPp: 3, currentPp: 3 }
            ];
        } else if (currentPet.petType === 'GRASS') {
            currentSkills = [
                { id: 7, name: '藤鞭', type: '攻击', desc: '造成伤害', maxPp: -1, currentPp: -1 },
                { id: 8, name: '硬化', type: '状态', desc: '防御力提升50%，可叠加', maxPp: 3, currentPp: 3 },
                { id: 9, name: '光合作用', type: '防御', desc: '减伤90%，回复30%生命', maxPp: 3, currentPp: 3 }
            ];
        }

        updateBattleUI(data.data);
        document.getElementById('start-battle-btn').style.display = 'none';
        document.getElementById('battle-actions').style.display = 'flex';
        document.getElementById('potion-actions').style.display = 'flex';
        document.getElementById('potion-count').style.display = 'block';
        updateSkillButtonsWithPP();
        await loadLevelList();
    } catch (error) {
        document.getElementById('battle-logs').innerHTML = `<div>开始战斗失败: ${error.message}</div>`;
    }
}

function updateBattleUI(battle) {
    document.getElementById('battle-pet-name').innerHTML = battle.petName;
    document.getElementById('battle-pet-hp').innerHTML = `HP: ${battle.petCurrentHp}/${battle.petMaxHp}`;
    document.getElementById('battle-monster-name').innerHTML = battle.monsterName;
    document.getElementById('battle-monster-hp').innerHTML = `HP: ${battle.monsterCurrentHp}/${battle.monsterMaxHp}`;

    // 更新药剂数量
    if (battle.hpPotionCount !== undefined) {
        hpPotionCount = battle.hpPotionCount;
        ppPotionCount = battle.ppPotionCount;
        const potionCountDiv = document.getElementById('potion-count');
        if (potionCountDiv) {
            potionCountDiv.innerHTML = `❤️HP药剂: ${hpPotionCount} 瓶 | 💙PP药剂: ${ppPotionCount} 瓶`;
        }
    }

    const logsDiv = document.getElementById('battle-logs');
    logsDiv.innerHTML = '';
    if (battle.logs) {
        battle.logs.forEach(log => {
            const div = document.createElement('div');
            div.textContent = log;
            logsDiv.appendChild(div);
        });
        logsDiv.scrollTop = logsDiv.scrollHeight;
    }

    if (battle.status === 'VICTORY') {
        isInBattle = false;
        pendingReward = true;
        document.getElementById('battle-actions').style.display = 'none';
        document.getElementById('potion-actions').style.display = 'none';
        document.getElementById('potion-count').style.display = 'none';
        document.getElementById('claim-reward-btn').style.display = 'block';
        document.getElementById('start-battle-btn').style.display = 'none';
        logsDiv.innerHTML += `<div>🎉 战斗胜利！点击领取奖励</div>`;
        document.getElementById('claim-reward-btn').onclick = async () => {
            await claimReward();
        };
        loadLevelList();
    } else if (battle.status === 'DEFEAT' || battle.status === 'SURRENDER') {
        isInBattle = false;
        pendingReward = false;
        document.getElementById('battle-actions').style.display = 'none';
        document.getElementById('potion-actions').style.display = 'none';
        document.getElementById('potion-count').style.display = 'none';
        document.getElementById('claim-reward-btn').style.display = 'none';
        document.getElementById('start-battle-btn').style.display = 'block';
        document.getElementById('start-battle-btn').innerHTML = '重新挑战';
        logsDiv.innerHTML += `<div>💀 战斗失败，点击重新挑战</div>`;
        currentBattleId = null;
        currentRoundSeq = 0;
        currentSkills = [];
        loadLevelList();
    }
}

// 更新技能按钮（无PP版本，用于非战斗状态）
function updateSkillButtons() {
    if (!currentPet) return;

    let skills = [];
    if (currentPet.petType === 'WATER') {
        skills = [
            { id: 1, name: '水炮', type: '攻击', desc: '造成伤害' },
            { id: 2, name: '冥想', type: '状态', desc: '攻击力提升100%，可叠加' },
            { id: 3, name: '水幕', type: '防御', desc: '本回合受到伤害减少90%' }
        ];
    } else if (currentPet.petType === 'FIRE') {
        skills = [
            { id: 4, name: '火花', type: '攻击', desc: '造成伤害' },
            { id: 5, name: '斗志', type: '状态', desc: '攻击力提升100%，可叠加' },
            { id: 6, name: '火焰护盾', type: '防御', desc: '减伤50%，反伤50%' }
        ];
    } else if (currentPet.petType === 'GRASS') {
        skills = [
            { id: 7, name: '藤鞭', type: '攻击', desc: '造成伤害' },
            { id: 8, name: '硬化', type: '状态', desc: '防御力提升50%，可叠加' },
            { id: 9, name: '光合作用', type: '防御', desc: '减伤90%，回复30%生命' }
        ];
    }

    document.getElementById('skill1-btn').innerHTML = `${skills[0].name}<span style="font-size:10px;display:block;">【${skills[0].type}】${skills[0].desc}</span>`;
    document.getElementById('skill2-btn').innerHTML = `${skills[1].name}<span style="font-size:10px;display:block;">【${skills[1].type}】${skills[1].desc}</span>`;
    document.getElementById('skill3-btn').innerHTML = `${skills[2].name}<span style="font-size:10px;display:block;">【${skills[2].type}】${skills[2].desc}</span>`;

    document.getElementById('skill1-btn').onclick = () => executeAction(skills[0].id);
    document.getElementById('skill2-btn').onclick = () => executeAction(skills[1].id);
    document.getElementById('skill3-btn').onclick = () => executeAction(skills[2].id);
}

// 更新技能按钮（带PP版本，用于战斗中）
function updateSkillButtonsWithPP() {
    if (!currentSkills || currentSkills.length === 0) return;

    for (let i = 0; i < currentSkills.length; i++) {
        const skill = currentSkills[i];
        const btn = document.getElementById(`skill${i+1}-btn`);
        if (skill.maxPp === -1) {
            btn.innerHTML = `${skill.name}<span style="font-size:10px;display:block;">【${skill.type}】${skill.desc}</span>`;
        } else {
            btn.innerHTML = `${skill.name}<span style="font-size:10px;display:block;">【${skill.type}】${skill.desc} (${skill.currentPp}/${skill.maxPp})</span>`;
        }
    }
}

async function executeAction(skillId) {
    if (!currentBattleId) return;

    const skillIndex = currentSkills.findIndex(s => s.id === skillId);
    if (skillIndex !== -1 && currentSkills[skillIndex].maxPp !== -1) {
        if (currentSkills[skillIndex].currentPp <= 0) {
            const logsDiv = document.getElementById('battle-logs');
            const div = document.createElement('div');
            div.textContent = `⚠️ ${currentSkills[skillIndex].name}使用次数已用完！`;
            div.style.color = '#ff0';
            logsDiv.appendChild(div);
            return;
        }
        currentSkills[skillIndex].currentPp--;
        updateSkillButtonsWithPP();
    }

    currentRoundSeq++;

    try {
        const data = await request('POST', `/battle/action?battleId=${currentBattleId}`, {
            actionType: 'SKILL',
            skillId: skillId,
            roundSeq: currentRoundSeq
        });
        updateBattleUI(data.data);

        if (data.data.status === 'VICTORY') {
            await loadLevelList();
            try {
                const summaryData = await request('GET', `/ai/summary?battleId=${currentBattleId}`);
                const logsDiv = document.getElementById('battle-logs');
                const div = document.createElement('div');
                div.textContent = `【AI战报】${summaryData.data}`;
                div.style.color = '#ff0';
                logsDiv.appendChild(div);
            } catch(e) {}
        }
    } catch (error) {
        if (skillIndex !== -1 && currentSkills[skillIndex].maxPp !== -1) {
            currentSkills[skillIndex].currentPp++;
            updateSkillButtonsWithPP();
        }
        const logsDiv = document.getElementById('battle-logs');
        const div = document.createElement('div');
        div.textContent = `操作失败: ${error.message}`;
        div.style.color = '#f00';
        logsDiv.appendChild(div);
    }
}

async function claimReward() {
    if (!currentBattleId) return;
    try {
        const data = await request('POST', `/level/claim-reward?battleId=${currentBattleId}`);
        const logsDiv = document.getElementById('battle-logs');
        let rewardMsg = `<div>🎉 获得${data.expGained}经验！${data.isFirstClear ? '首次通关！' : ''}</div>`;
        if (data.potionReward) {
            rewardMsg += `<div>📦 获得${data.potionReward} x1</div>`;
        }
        logsDiv.innerHTML = rewardMsg;
        document.getElementById('claim-reward-btn').style.display = 'none';
        document.getElementById('start-battle-btn').style.display = 'block';
        document.getElementById('start-battle-btn').innerHTML = '选择关卡';
        document.getElementById('battle-actions').style.display = 'none';
        document.getElementById('potion-actions').style.display = 'none';
        document.getElementById('potion-count').style.display = 'none';
        document.getElementById('battle-pet-name').innerHTML = '';
        document.getElementById('battle-pet-hp').innerHTML = '';
        document.getElementById('battle-monster-name').innerHTML = '';
        document.getElementById('battle-monster-hp').innerHTML = '';

        currentBattleId = null;
        currentRoundSeq = 0;
        pendingReward = false;
        currentSkills = [];

        await updatePetInfo();
        await loadLevelList();

        document.getElementById('start-battle-btn').onclick = () => {
            document.getElementById('battle-logs').innerHTML = `<div>请点击左侧关卡开始挑战</div>`;
        };
    } catch (error) {
        document.getElementById('battle-logs').innerHTML += `<div>领取奖励失败: ${error.message}</div>`;
    }
}

// 药剂按钮事件
document.getElementById('hp-potion-btn').onclick = async () => {
    if (!currentBattleId) {
        alert('不在战斗中');
        return;
    }
    if (hpPotionCount <= 0) {
        alert('背包中没有HP药剂');
        return;
    }
    await usePotion(true, false);
};

document.getElementById('pp-potion-btn').onclick = async () => {
    if (!currentBattleId) {
        alert('不在战斗中');
        return;
    }
    if (ppPotionCount <= 0) {
        alert('背包中没有PP药剂');
        return;
    }
    await usePotion(false, true);
};

async function usePotion(useHp, usePp) {
    const seq = currentRoundSeq;

    try {
        const data = await request('POST', `/battle/action?battleId=${currentBattleId}`, {
            actionType: 'SKILL',
            skillId: 1,
            roundSeq: seq + 1,
            useHpPotion: useHp,
            usePpPotion: usePp
        });

        if (data.data.hpPotionCount !== undefined) {
            hpPotionCount = data.data.hpPotionCount;
            ppPotionCount = data.data.ppPotionCount;
            const potionCountDiv = document.getElementById('potion-count');
            if (potionCountDiv) {
                potionCountDiv.innerHTML = `❤️HP药剂: ${hpPotionCount} 瓶 | 💙PP药剂: ${ppPotionCount} 瓶`;
            }
        }

        updateBattleUI(data.data);
        currentRoundSeq = seq + 1;

        if (data.data.skills) {
            for (let i = 0; i < data.data.skills.length && i < currentSkills.length; i++) {
                currentSkills[i].currentPp = data.data.skills[i].currentPp;
            }
            updateSkillButtonsWithPP();
        }
    } catch (error) {
        alert(error.message);
    }
}

document.getElementById('surrender-btn').onclick = async () => {
    if (!currentBattleId) return;
    try {
        const data = await request('POST', `/battle/surrender?battleId=${currentBattleId}`);
        updateBattleUI(data.data);
        currentSkills = [];
    } catch (error) {
        alert(error.message);
    }
};

// ========== 初始化 ==========
function init() {
    localStorage.removeItem('token');
    token = null;
    showPage('login');
    initAuth();
}

init();