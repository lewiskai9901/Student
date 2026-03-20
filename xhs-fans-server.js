const http = require('http');
const puppeteer = require('puppeteer');
const path = require('path');
const fs = require('fs');
const crypto = require('crypto');

const PORT = process.env.PORT || 3457;
const DATA_DIR = path.join(__dirname, '.xhs-data');

if (!fs.existsSync(DATA_DIR)) fs.mkdirSync(DATA_DIR, { recursive: true });

// Find Chrome executable: env var > system Chrome > Puppeteer bundled
function findChromePath() {
  if (process.env.CHROME_PATH && fs.existsSync(process.env.CHROME_PATH)) return process.env.CHROME_PATH;
  const candidates = process.platform === 'win32'
    ? ['C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
       'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe']
    : ['/usr/bin/google-chrome-stable', '/usr/bin/google-chrome',
       '/usr/bin/chromium-browser', '/usr/bin/chromium',
       '/snap/bin/chromium'];
  for (const p of candidates) { if (fs.existsSync(p)) return p; }
  return null; // fallback to Puppeteer bundled Chromium
}

// ========== Data Store ==========
// { code: { code, nickname, cookies[], createdAt, lastUsed, loginExpired } }
const users = new Map();

function loadUsers() {
  const f = path.join(DATA_DIR, 'users.json');
  if (!fs.existsSync(f)) return;
  try {
    const arr = JSON.parse(fs.readFileSync(f, 'utf-8'));
    for (const u of arr) users.set(u.code, u);
    console.log(`  已加载 ${users.size} 个用户`);
  } catch (e) { console.error('加载用户数据失败:', e.message); }
}

function saveUsers() {
  fs.writeFileSync(path.join(DATA_DIR, 'users.json'), JSON.stringify([...users.values()], null, 2));
}

function getUser(code) { return users.get(code); }

function setUser(code, data) {
  const existing = users.get(code) || { code, createdAt: Date.now() };
  Object.assign(existing, data, { lastUsed: Date.now() });
  users.set(code, existing);
  saveUsers();
  return existing;
}

function deleteUser(code) {
  users.delete(code);
  saveUsers();
}

// ========== Browser ==========
let browser = null;

async function ensureBrowser() {
  if (browser && browser.isConnected()) return;
  console.log('启动浏览器...');
  const args = ['--no-sandbox', '--disable-setuid-sandbox', '--disable-dev-shm-usage',
    '--disable-gpu', '--disable-software-rasterizer'];
  const chromePath = findChromePath();

  // Strategy 1: Use system Chrome if found
  if (chromePath) {
    console.log(`  尝试系统浏览器: ${chromePath}`);
    try {
      browser = await puppeteer.launch({ headless: 'new', executablePath: chromePath, args });
      console.log(`浏览器已启动 (系统: ${chromePath})`);
      return;
    } catch (e) {
      console.warn(`  系统浏览器启动失败: ${e.message}`);
    }
  }

  // Strategy 2: Use Puppeteer bundled Chromium
  console.log('  尝试 Puppeteer 内置 Chromium...');
  try {
    browser = await puppeteer.launch({ headless: 'new', args });
    console.log('浏览器已启动 (Puppeteer 内置 Chromium)');
    return;
  } catch (e) {
    console.error(`  Puppeteer 内置 Chromium 启动失败: ${e.message}`);
    console.error('\n========================================');
    console.error('  浏览器启动失败！请手动安装依赖:');
    console.error('  Ubuntu/Debian:');
    console.error('    apt-get install -y libnss3 libatk1.0-0 libatk-bridge2.0-0 \\');
    console.error('      libcups2 libdrm2 libxkbcommon0 libxcomposite1 \\');
    console.error('      libxdamage1 libxrandr2 libgbm1 libpango-1.0-0 \\');
    console.error('      libcairo2 libasound2 libxshmfence1 fonts-liberation');
    console.error('  CentOS/RHEL:');
    console.error('    yum install -y nss atk at-spi2-atk cups-libs libdrm \\');
    console.error('      libxkbcommon libXcomposite libXdamage libXrandr \\');
    console.error('      mesa-libgbm pango cairo alsa-lib libxshmfence');
    console.error('  然后重启服务: pm2 restart xhs-fans');
    console.error('========================================\n');
    throw new Error('浏览器启动失败，请安装系统依赖后重试');
  }
}

async function createPage(cookies) {
  await ensureBrowser();
  const page = await browser.newPage();
  await page.setViewport({ width: 1280, height: 800 });
  await page.setUserAgent('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36');
  if (cookies && cookies.length) await page.setCookie(...cookies);
  return page;
}

// ========== Login ==========
const loginProcesses = new Map(); // { loginToken: { page, code, qrImage } }

async function startLogin(code) {
  await ensureBrowser();
  const loginToken = crypto.randomBytes(12).toString('hex');
  console.log(`[登录] code=${code} token=${loginToken.substring(0, 8)}...`);

  const page = await browser.newPage();
  await page.setViewport({ width: 500, height: 600 });
  await page.setUserAgent('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36');

  const info = { page, code, qrImage: null, createdAt: Date.now() };
  loginProcesses.set(loginToken, info);

  try {
    // Step 1: Load page (use domcontentloaded - faster and more reliable)
    console.log('[登录] Step1: 加载页面...');
    await page.goto('https://www.xiaohongshu.com/explore', {
      waitUntil: 'domcontentloaded', timeout: 30000
    });

    const pageTitle = await page.title().catch(() => '');
    const pageUrl = page.url();
    console.log(`[登录] Step1 完成: title="${pageTitle}" url=${pageUrl}`);

    // Wait for SPA JavaScript to render (longer wait for slow servers)
    console.log('[登录] 等待页面渲染...');
    await new Promise(r => setTimeout(r, 8000));

    // Step 2: Check page content
    const bodyText = await page.evaluate(() => document.body?.innerText?.substring(0, 200) || '').catch(() => '');
    console.log(`[登录] Step2 页面内容: "${bodyText.substring(0, 80)}..."`);

    // Step 3: Click login button
    console.log('[登录] Step3: 查找登录按钮...');
    const clicked = await page.evaluate(() => {
      // Try multiple selectors
      const selectors = ['button', 'a', 'div', 'span'];
      const keywords = ['登录', '登 录', 'Log in', 'Sign in'];
      for (const sel of selectors) {
        for (const el of document.querySelectorAll(sel)) {
          const text = el.textContent.trim();
          if (keywords.some(k => text === k) && el.offsetWidth > 0 && el.offsetHeight > 0) {
            el.click();
            return `clicked: "${text}" (${sel})`;
          }
        }
      }
      // Try clicking elements with login-related class
      for (const el of document.querySelectorAll('[class*="login"], [class*="Login"], [class*="sign"]')) {
        if (el.offsetWidth > 0 && el.offsetHeight > 0 && el.textContent.trim().length < 20) {
          el.click();
          return `clicked class: "${el.className}" text="${el.textContent.trim()}"`;
        }
      }
      return 'not found';
    }).catch(() => 'context destroyed (navigation)');
    console.log(`[登录] Step3: ${clicked}`);

    // Wait for navigation/modal to settle
    await new Promise(r => setTimeout(r, 3000));
    // If page navigated, wait for it to load
    await page.waitForNavigation({ waitUntil: 'domcontentloaded', timeout: 5000 }).catch(() => {});
    await new Promise(r => setTimeout(r, 2000));

    // Step 4: Capture QR
    console.log('[登录] Step4: 截取二维码...');
    info.qrImage = await captureQr(page);
    console.log(`[登录] Step4: QR ${info.qrImage ? `已获取 (${Math.round(info.qrImage.length / 1024)}KB)` : '未找到'}`);

  } catch (e) {
    console.error(`[登录] 错误: ${e.message}`);
    // Always try to capture a screenshot on error for debugging
    try {
      info.qrImage = await page.screenshot({ encoding: 'base64', fullPage: false });
      console.log(`[登录] 错误截图已获取 (${Math.round((info.qrImage || '').length / 1024)}KB)`);
    } catch (e2) {
      console.error(`[登录] 截图也失败: ${e2.message}`);
    }
  }
  return { loginToken, hasQr: !!info.qrImage };
}

async function captureQr(page) {
  // Strategy 1: QR code specific elements
  const qrSelectors = [
    '[class*="qrcode"]', '[class*="qr-code"]', '[class*="QRCode"]', '[class*="qr_code"]',
    'canvas', 'img[src*="qrcode"]', 'img[src*="qr"]', 'img[src*="data:image"]',
    '[class*="code-img"]', '[class*="codeImg"]'
  ];
  for (const sel of qrSelectors) {
    const el = await page.$(sel);
    if (el) {
      const box = await el.boundingBox();
      if (box && box.width > 50 && box.height > 50) {
        console.log(`[QR] 找到元素: ${sel} (${Math.round(box.width)}x${Math.round(box.height)})`);
        return await el.screenshot({ encoding: 'base64' });
      }
    }
  }

  // Strategy 2: Login modal / dialog
  const modalSelectors = [
    '[class*="login-modal"]', '[class*="loginModal"]', '[class*="login-container"]',
    '[class*="modal"]', '[class*="dialog"]', '[class*="popup"]',
    '[class*="login"]', '[class*="Login"]', '[role="dialog"]'
  ];
  for (const sel of modalSelectors) {
    const el = await page.$(sel);
    if (el) {
      const box = await el.boundingBox();
      if (box && box.width > 150 && box.height > 150) {
        console.log(`[QR] 找到弹窗: ${sel} (${Math.round(box.width)}x${Math.round(box.height)})`);
        return await el.screenshot({ encoding: 'base64' });
      }
    }
  }

  // Strategy 3: Full page screenshot (always works)
  console.log('[QR] 未找到二维码元素，截取整个页面');
  return await page.screenshot({ encoding: 'base64', fullPage: false });
}

async function checkLogin(loginToken) {
  const info = loginProcesses.get(loginToken);
  if (!info) return { status: 'expired' };

  // Timeout after 10 minutes (longer for SMS verification)
  if (Date.now() - info.createdAt > 10 * 60 * 1000) {
    await info.page.close().catch(() => {});
    loginProcesses.delete(loginToken);
    return { status: 'expired' };
  }

  try {
    // Wait for any in-progress navigation to settle
    await info.page.waitForNavigation({ waitUntil: 'domcontentloaded', timeout: 3000 }).catch(() => {});

    // Detect page state
    const pageState = await info.page.evaluate(() => {
      const t = document.body?.innerText || '';
      // Check if fully logged in
      const isLoggedIn = t.includes('我') && !t.includes('登录') && !t.includes('验证');
      // Check if SMS verification is needed
      const needsSms = t.includes('验证码') || t.includes('短信') || t.includes('输入验证码')
        || !!document.querySelector('input[placeholder*="验证码"], input[placeholder*="code"], input[type="tel"][maxlength="6"], input[type="tel"][maxlength="4"]');
      // Check if there's a phone number hint
      const phoneHint = t.match(/(\d{3}\*+\d{2,4})/)?.[1] || '';
      return { isLoggedIn, needsSms, phoneHint, bodySnippet: t.substring(0, 150) };
    }).catch(() => ({ isLoggedIn: false, needsSms: false, phoneHint: '', bodySnippet: 'context destroyed' }));

    console.log(`[登录检查] state=${JSON.stringify(pageState)}`);

    if (pageState.isLoggedIn) {
      const cookies = await info.page.cookies();
      const nickname = await info.page.evaluate(() => {
        const el = document.querySelector('[class*="nickname"], [class*="user-name"]');
        return el ? el.textContent.trim() : '';
      }) || '用户';

      setUser(info.code, { nickname, cookies, loginExpired: false });

      await info.page.close().catch(() => {});
      loginProcesses.delete(loginToken);

      console.log(`[登录] 成功! code=${info.code} 昵称=${nickname} cookies=${cookies.length}条`);
      return { status: 'success', nickname };
    }

    if (pageState.needsSms) {
      // Take screenshot of verification page
      const screenshot = await info.page.screenshot({ encoding: 'base64', fullPage: false });
      return {
        status: 'need_sms_code',
        phoneHint: pageState.phoneHint,
        screenshot: screenshot ? `data:image/png;base64,${screenshot}` : null
      };
    }

    // Still waiting for QR scan - refresh QR
    const qr = await captureQr(info.page);
    if (qr) info.qrImage = qr;
    return { status: 'waiting' };
  } catch (e) {
    console.error(`[登录检查] 错误: ${e.message}`);
    return { status: 'error', error: e.message };
  }
}

async function submitSmsCode(loginToken, smsCode) {
  const info = loginProcesses.get(loginToken);
  if (!info) return { status: 'expired' };

  try {
    console.log(`[验证码] 提交验证码: ${smsCode}`);

    // Find and fill the SMS code input
    const filled = await info.page.evaluate((code) => {
      // Try various input selectors
      const selectors = [
        'input[placeholder*="验证码"]', 'input[placeholder*="code"]',
        'input[type="tel"][maxlength="6"]', 'input[type="tel"][maxlength="4"]',
        'input[type="number"]', 'input[type="tel"]',
        'input[class*="code"]', 'input[class*="verify"]', 'input[class*="sms"]'
      ];
      for (const sel of selectors) {
        const input = document.querySelector(sel);
        if (input && input.offsetWidth > 0) {
          // Use native input setter to trigger React/Vue change events
          const nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
          nativeInputValueSetter.call(input, code);
          input.dispatchEvent(new Event('input', { bubbles: true }));
          input.dispatchEvent(new Event('change', { bubbles: true }));
          return `filled: ${sel}`;
        }
      }
      return 'input not found';
    }, smsCode);

    console.log(`[验证码] ${filled}`);

    // Wait a moment for the UI to update
    await new Promise(r => setTimeout(r, 500));

    // Try to click submit/confirm button
    const clicked = await info.page.evaluate(() => {
      const keywords = ['确定', '确认', '提交', '验证', '登录', '下一步'];
      for (const el of document.querySelectorAll('button, [role="button"], a, div[class*="btn"], span[class*="btn"]')) {
        const text = el.textContent.trim();
        if (keywords.some(k => text.includes(k)) && el.offsetWidth > 0 && el.offsetHeight > 0) {
          el.click();
          return `clicked: "${text}"`;
        }
      }
      // Try submit by pressing Enter on the input
      const input = document.querySelector('input[placeholder*="验证码"], input[type="tel"]');
      if (input) {
        input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', code: 'Enter', bubbles: true }));
        return 'pressed Enter on input';
      }
      return 'button not found';
    });

    console.log(`[验证码] ${clicked}`);

    // Wait for page to process (and possible navigation)
    await new Promise(r => setTimeout(r, 3000));
    await info.page.waitForNavigation({ waitUntil: 'domcontentloaded', timeout: 5000 }).catch(() => {});
    await new Promise(r => setTimeout(r, 2000));

    // Check result
    const result = await info.page.evaluate(() => {
      const t = document.body?.innerText || '';
      const isLoggedIn = t.includes('我') && !t.includes('登录') && !t.includes('验证');
      const hasError = t.includes('错误') || t.includes('失败') || t.includes('无效') || t.includes('重新');
      return { isLoggedIn, hasError, bodySnippet: t.substring(0, 150) };
    }).catch(() => ({ isLoggedIn: false, hasError: false, bodySnippet: 'context destroyed' }));

    console.log(`[验证码] 结果: ${JSON.stringify(result)}`);

    if (result.isLoggedIn) {
      const cookies = await info.page.cookies();
      const nickname = await info.page.evaluate(() => {
        const el = document.querySelector('[class*="nickname"], [class*="user-name"]');
        return el ? el.textContent.trim() : '';
      }) || '用户';

      setUser(info.code, { nickname, cookies, loginExpired: false });
      await info.page.close().catch(() => {});
      loginProcesses.delete(loginToken);

      console.log(`[验证码] 登录成功! 昵称=${nickname}`);
      return { status: 'success', nickname };
    }

    if (result.hasError) {
      return { status: 'sms_error', error: '验证码错误，请重试' };
    }

    // Not yet logged in, might need more time
    return { status: 'sms_submitted', message: '验证码已提交，等待验证...' };
  } catch (e) {
    console.error(`[验证码] 错误: ${e.message}`);
    return { status: 'error', error: e.message };
  }
}

function getQrImage(loginToken) {
  return loginProcesses.get(loginToken)?.qrImage || null;
}

// ========== Query ==========
const busyCodes = new Set();

async function queryRedId(redId, user) {
  const page = await createPage(user.cookies);
  try {
    await page.goto(`https://www.xiaohongshu.com/search_result?keyword=${encodeURIComponent(redId)}&type=user`, {
      waitUntil: 'domcontentloaded', timeout: 30000,
    });
    await new Promise(r => setTimeout(r, 5000));

    // Click 用户 tab
    await page.evaluate(() => {
      for (const t of document.querySelectorAll('[class*="tab"], [class*="filter"]'))
        if (t.textContent.trim() === '用户') { t.click(); return; }
    });
    await new Promise(r => setTimeout(r, 2000));

    // Check login
    const needsLogin = await page.evaluate(() => {
      const t = document.body.innerText || '';
      return t.includes('登录探索更多内容') || (t.includes('登录') && !t.includes('粉丝') && !t.includes('关注'));
    });
    if (needsLogin) {
      // Mark expired
      setUser(user.code, { loginExpired: true });
      throw new Error('LOGIN_EXPIRED');
    }

    // Extract
    const userList = await page.evaluate((kw) => {
      const results = [];
      function extractFans(text) {
        const p1 = text.match(/粉丝\s*[·:：\-・\s]+\s*(\d[\d,.]*万?)/);
        if (p1) return p1[1];
        const p2 = text.match(/(\d[\d,.]*万?)\s*粉丝/g);
        if (p2) for (const m of p2) { const n = m.match(/(\d[\d,.]*万?)\s*粉丝/); if (n && n[1] !== kw) return n[1]; }
        return null;
      }
      for (const link of document.querySelectorAll('a[href*="/user/profile/"]')) {
        const href = link.getAttribute('href') || '';
        const um = href.match(/\/user\/profile\/([0-9a-f]{24})/i);
        if (!um) continue;
        let card = link, cardText = '', fans = null;
        for (let l = 0; l < 8; l++) {
          if (!card.parentElement) break;
          card = card.parentElement;
          cardText = card.textContent || '';
          if (cardText.includes('粉丝')) { fans = extractFans(cardText); if (fans) break; }
          if (cardText.length > 500) break;
        }
        let nickname = '';
        const nameEl = link.querySelector('[class*="name"], [class*="nick"], [class*="title"]');
        if (nameEl) nickname = nameEl.textContent.trim();
        else { const raw = link.textContent.trim(); nickname = raw.split(/小红书号|粉丝|关注|获赞|赞藏/)[0].trim() || raw.substring(0, 20); }
        nickname = nickname.substring(0, 30);
        if (nickname.length > 25 || /备案|许可/.test(nickname) || nickname === '我') continue;
        results.push({ userId: um[1], nickname, fans, _cs: cardText.substring(0, 200), _lt: link.textContent.trim().substring(0, 80) });
      }
      const seen = new Set();
      return results.filter(r => { if (seen.has(r.userId)) return false; seen.add(r.userId); return true; });
    }, redId);

    let match = userList.find(x => (x._cs && x._cs.includes(redId)) || (x._lt && x._lt.includes(redId)));
    if (!match) match = userList.find(x => x.nickname && x.fans);
    if (!match && userList.length > 0) match = userList[0];

    if (match && match.fans == null && ((match._cs && match._cs.includes(redId)) || (match._lt && match._lt.includes(redId)))) {
      match.fans = '0';
    }

    if (match) return { success: true, redId, nickname: match.nickname, fans: match.fans || null, userId: match.userId };
    return { success: false, redId, error: '未找到用户' };
  } finally {
    await page.close().catch(() => {});
  }
}

// ========== Serve Static ==========
function serveFile(res, filename, contentType) {
  try {
    const data = fs.readFileSync(path.join(__dirname, filename), 'utf-8');
    res.writeHead(200, { 'Content-Type': contentType });
    res.end(data);
  } catch (e) { res.writeHead(404); res.end('Not found'); }
}

// ========== Admin HTML (inline) ==========
function serveAdmin(res) {
  const html = `<!DOCTYPE html><html lang="zh-CN"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
<title>后台管理 - 小红书粉丝查询</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}body{font-family:-apple-system,sans-serif;background:#f5f5f5;color:#333;padding:20px}
.container{max-width:960px;margin:0 auto}
h1{font-size:18px;font-weight:600;margin-bottom:16px;color:#333;display:flex;align-items:center;gap:8px}
h1 a{font-size:13px;color:#999;text-decoration:none;font-weight:400}h1 a:hover{color:#ff2442}
.card{background:#fff;border-radius:8px;padding:16px;box-shadow:0 1px 3px rgba(0,0,0,.06);margin-bottom:14px}
.stats{display:flex;gap:20px;margin-bottom:16px;font-size:13px;color:#666}
.stats b{color:#ff2442;font-size:16px}
table{width:100%;border-collapse:collapse;font-size:13px}
th{background:#fafafa;text-align:left;padding:10px 12px;font-weight:500;border-bottom:2px solid #eee;white-space:nowrap}
td{padding:10px 12px;border-bottom:1px solid #f0f0f0;vertical-align:top}
tr:hover td{background:#fafafa}
.code{font-family:monospace;font-weight:600;color:#ff2442;font-size:14px}
.tag{display:inline-block;padding:2px 8px;border-radius:10px;font-size:11px;font-weight:500}
.tag-ok{background:#f6ffed;color:#52c41a}.tag-exp{background:#fff2e8;color:#fa8c16}.tag-no{background:#f5f5f5;color:#bbb}
.cookie-cell{max-width:300px}
.cookie-val{font-family:monospace;font-size:11px;color:#999;max-height:60px;overflow:auto;word-break:break-all;white-space:pre-wrap;background:#fafafa;padding:4px 6px;border-radius:4px;margin-top:4px;cursor:pointer}
.cookie-val:hover{color:#333}
.actions button{padding:4px 10px;font-size:12px;border:1px solid #ddd;border-radius:4px;background:#fff;cursor:pointer;margin-right:4px}
.actions button:hover{border-color:#ff2442;color:#ff2442}
.actions button.del:hover{border-color:#ff4d4f;color:#ff4d4f}
.time{font-size:11px;color:#bbb}
.empty{text-align:center;padding:40px;color:#ccc}
</style></head><body>
<div class="container">
<h1>后台管理 <a href="/">← 返回查询</a></h1>
<div class="card">
<div class="stats" id="stats"></div>
<table><thead><tr><th>编码</th><th>昵称</th><th>状态</th><th>Cookies</th><th>创建时间</th><th>最后使用</th><th>操作</th></tr></thead>
<tbody id="tbody"></tbody></table>
<div class="empty" id="empty" style="display:none">暂无用户数据</div>
</div></div>
<script>
const API=location.origin;
async function load(){
  const r=await fetch(API+'/api/admin/users');const data=await r.json();
  const s=document.getElementById('stats');
  const total=data.length,active=data.filter(u=>!u.loginExpired&&u.cookies?.length).length,expired=data.filter(u=>u.loginExpired).length;
  s.innerHTML='总用户: <b>'+total+'</b> | 有效: <b>'+active+'</b> | 过期: <b>'+expired+'</b>';
  const tb=document.getElementById('tbody');tb.innerHTML='';
  if(!data.length){document.getElementById('empty').style.display='';return}
  document.getElementById('empty').style.display='none';
  for(const u of data){
    const st=!u.cookies?.length?'tag-no':u.loginExpired?'tag-exp':'tag-ok';
    const stText=!u.cookies?.length?'未登录':u.loginExpired?'已过期':'有效';
    const ck=u.cookies||[];
    const keyC=ck.filter(c=>['a1','web_session','webId','gid'].includes(c.name));
    const ckText=keyC.map(c=>c.name+'='+c.value).join('\\n')||'(无)';
    const allCkText=ck.map(c=>c.name+'='+c.value+' (domain='+c.domain+' exp='+(c.expires?new Date(c.expires*1000).toLocaleString():'session')+')').join('\\n');
    const tr=document.createElement('tr');
    tr.innerHTML=
      '<td class="code">'+u.code+'</td>'+
      '<td>'+(u.nickname||'-')+'</td>'+
      '<td><span class="tag '+st+'">'+stText+'</span></td>'+
      '<td class="cookie-cell"><div>'+ck.length+'条</div><div class="cookie-val" title="点击查看全部" onclick="this.textContent=this.dataset.full===\\'1\\'?this.dataset.short:this.dataset.all;this.dataset.full=this.dataset.full===\\'1\\'?\\'0\\':\\'1\\'" data-short="'+esc(ckText)+'" data-all="'+esc(allCkText)+'" data-full="0">'+esc(ckText)+'</div></td>'+
      '<td class="time">'+(u.createdAt?new Date(u.createdAt).toLocaleString():'-')+'</td>'+
      '<td class="time">'+(u.lastUsed?new Date(u.lastUsed).toLocaleString():'-')+'</td>'+
      '<td class="actions"><button onclick="resetCookie(\\''+u.code+'\\')">清除Cookie</button><button class="del" onclick="delUser(\\''+u.code+'\\')">删除</button></td>';
    tb.appendChild(tr);
  }
}
function esc(s){return s.replace(/&/g,'&amp;').replace(/"/g,'&quot;').replace(/</g,'&lt;')}
async function resetCookie(code){if(!confirm('清除 '+code+' 的Cookie？'))return;await fetch(API+'/api/admin/reset-cookie?code='+code);load()}
async function delUser(code){if(!confirm('删除用户 '+code+'？'))return;await fetch(API+'/api/admin/delete?code='+code);load()}
load();setInterval(load,15000);
</script></body></html>`;
  res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
  res.end(html);
}

// ========== HTTP Router ==========
function json(res, code, data) {
  res.writeHead(code, { 'Content-Type': 'application/json; charset=utf-8' });
  res.end(JSON.stringify(data));
}

const server = http.createServer(async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', '*');
  if (req.method === 'OPTIONS') { res.writeHead(204); res.end(); return; }

  const url = new URL(req.url, `http://localhost:${PORT}`);
  const p = url.pathname;

  // -- Pages --
  if (p === '/' || p === '/index.html') return serveFile(res, 'xhs-fans-checker.html', 'text/html; charset=utf-8');
  if (p === '/admin') return serveAdmin(res);

  // -- Auth: check code --
  if (p === '/api/auth/check') {
    const code = url.searchParams.get('code')?.trim();
    if (!code) return json(res, 400, { error: '缺少编码' });
    const user = getUser(code);
    if (!user || !user.cookies?.length) return json(res, 200, { exists: false });
    return json(res, 200, { exists: true, nickname: user.nickname, loginExpired: !!user.loginExpired });
  }

  // -- Login: start --
  if (p === '/api/login/start') {
    const code = url.searchParams.get('code')?.trim();
    if (!code) return json(res, 400, { error: '缺少编码' });
    try {
      const result = await startLogin(code);
      const qr = getQrImage(result.loginToken);
      json(res, 200, { loginToken: result.loginToken, qrImage: qr ? `data:image/png;base64,${qr}` : null });
    } catch (e) { json(res, 500, { error: e.message }); }
    return;
  }

  // -- Login: poll --
  if (p === '/api/login/check') {
    const token = url.searchParams.get('token');
    if (!token) return json(res, 400, { error: '缺少token' });
    try {
      const result = await checkLogin(token);
      if (result.status === 'waiting') {
        const qr = getQrImage(token);
        result.qrImage = qr ? `data:image/png;base64,${qr}` : null;
      }
      json(res, 200, result);
    } catch (e) { json(res, 500, { error: e.message }); }
    return;
  }

  // -- Login: submit SMS code --
  if (p === '/api/login/submit-code') {
    const token = url.searchParams.get('token');
    const smsCode = url.searchParams.get('code')?.trim();
    if (!token) return json(res, 400, { error: '缺少token' });
    if (!smsCode) return json(res, 400, { error: '缺少验证码' });
    try {
      const result = await submitSmsCode(token, smsCode);
      json(res, 200, result);
    } catch (e) { json(res, 500, { error: e.message }); }
    return;
  }

  // -- Query --
  if (p === '/api/query') {
    const redId = url.searchParams.get('id')?.trim();
    const code = url.searchParams.get('code')?.trim();
    if (!redId) return json(res, 400, { error: '缺少id' });
    if (!code) return json(res, 401, { error: '缺少编码', needLogin: true });

    const user = getUser(code);
    if (!user || !user.cookies?.length) return json(res, 401, { error: '请先登录', needLogin: true });
    if (user.loginExpired) return json(res, 401, { error: '登录已过期，请重新扫码登录', needLogin: true });

    if (busyCodes.has(code)) return json(res, 429, { error: '正在查询中，请稍后' });
    busyCodes.add(code);

    const ts = Date.now();
    console.log(`[${new Date().toLocaleTimeString()}] 查询: ${redId} (${code})`);
    try {
      const result = await queryRedId(redId, user);
      console.log(`  -> ${result.nickname || '-'} 粉丝:${result.fans || '-'} (${Date.now() - ts}ms)`);
      json(res, 200, result);
    } catch (e) {
      if (e.message === 'LOGIN_EXPIRED') {
        json(res, 401, { error: '登录已过期，请重新扫码登录', needLogin: true });
      } else {
        json(res, 500, { error: e.message });
      }
    } finally { busyCodes.delete(code); }
    return;
  }

  // -- Status --
  if (p === '/api/status') {
    return json(res, 200, { running: true, browserConnected: browser?.isConnected(), totalUsers: users.size });
  }

  // -- Admin APIs --
  if (p === '/api/admin/users') {
    const arr = [...users.values()].sort((a, b) => (b.lastUsed || 0) - (a.lastUsed || 0));
    return json(res, 200, arr);
  }
  if (p === '/api/admin/reset-cookie') {
    const code = url.searchParams.get('code');
    if (code && users.has(code)) { setUser(code, { cookies: [], loginExpired: true }); }
    return json(res, 200, { ok: true });
  }
  if (p === '/api/admin/delete') {
    const code = url.searchParams.get('code');
    if (code) deleteUser(code);
    return json(res, 200, { ok: true });
  }

  res.writeHead(404); res.end('Not found');
});

// ========== Start ==========
(async () => {
  try {
    loadUsers();
    await ensureBrowser();
    server.listen(PORT, '0.0.0.0', () => {
      console.log(`\n  服务已启动: http://0.0.0.0:${PORT}`);
      console.log(`  后台管理: http://localhost:${PORT}/admin`);
      console.log(`  用户数: ${users.size}\n`);
    });
  } catch (e) { console.error('启动失败:', e.message); process.exit(1); }
})();

process.on('SIGINT', async () => {
  console.log('\n关闭...');
  for (const [, info] of loginProcesses) await info.page.close().catch(() => {});
  if (browser) await browser.close();
  process.exit(0);
});
