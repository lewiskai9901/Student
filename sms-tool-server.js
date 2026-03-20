const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');
const url = require('url');
const os = require('os');

const PORT = 3333;
const DATA_DIR = path.join(__dirname, 'sms-tool-data');
const ADMIN_PASSWORD = 'admin888';  // 管理后台密码，按需修改

// Ensure data directory exists
if (!fs.existsSync(DATA_DIR)) fs.mkdirSync(DATA_DIR);

// ---- Per-device data persistence ----
function dataFile(deviceId) {
  // Sanitize deviceId to prevent path traversal
  const safe = deviceId.replace(/[^a-zA-Z0-9_-]/g, '');
  return path.join(DATA_DIR, safe + '.json');
}

function readData(deviceId) {
  try {
    const f = dataFile(deviceId);
    if (fs.existsSync(f)) return JSON.parse(fs.readFileSync(f, 'utf-8'));
  } catch(e) {}
  return [];
}

function writeData(deviceId, data) {
  fs.writeFileSync(dataFile(deviceId), JSON.stringify(data, null, 2), 'utf-8');
}

function memoFile(deviceId) {
  const safe = deviceId.replace(/[^a-zA-Z0-9_-]/g, '');
  return path.join(DATA_DIR, safe + '_memo.txt');
}

function readMemo(deviceId) {
  try {
    const f = memoFile(deviceId);
    if (fs.existsSync(f)) return fs.readFileSync(f, 'utf-8');
  } catch(e) {}
  return '';
}

function writeMemo(deviceId, text) {
  fs.writeFileSync(memoFile(deviceId), text, 'utf-8');
}

function metaFile(deviceId) {
  const safe = deviceId.replace(/[^a-zA-Z0-9_-]/g, '');
  return path.join(DATA_DIR, safe + '_meta.json');
}

function readMeta(deviceId) {
  try {
    const f = metaFile(deviceId);
    if (fs.existsSync(f)) return JSON.parse(fs.readFileSync(f, 'utf-8'));
  } catch(e) {}
  return null;
}

function updateMeta(deviceId, req) {
  const meta = readMeta(deviceId) || { firstSeen: Date.now() };
  meta.lastSeen = Date.now();
  meta.ip = req.headers['x-forwarded-for'] || req.socket.remoteAddress || '';
  meta.userAgent = req.headers['user-agent'] || '';
  fs.writeFileSync(metaFile(deviceId), JSON.stringify(meta, null, 2), 'utf-8');
  return meta;
}

function getClientIp(req) {
  return req.headers['x-forwarded-for'] || req.socket.remoteAddress || '';
}

function checkAdminAuth(req, res) {
  const pwd = req.headers['x-admin-password'] || '';
  if (pwd !== ADMIN_PASSWORD) {
    res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8' });
    res.end('{"error":"密码错误"}');
    return false;
  }
  return true;
}

// ---- Get LAN IPs ----
function getLanIps() {
  const ips = [];
  const nets = os.networkInterfaces();
  for (const name of Object.keys(nets)) {
    for (const net of nets[name]) {
      if (net.family === 'IPv4' && !net.internal) ips.push(net.address);
    }
  }
  return ips.length ? ips : ['localhost'];
}

// ---- Server ----
const server = http.createServer((req, res) => {
  const parsed = url.parse(req.url, true);

  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') { res.writeHead(204); res.end(); return; }

  // ---- API: Get data (per device) ----
  if (parsed.pathname === '/api/data' && req.method === 'GET') {
    const deviceId = parsed.query.device || 'default';
    const data = readData(deviceId);
    updateMeta(deviceId, req);
    res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
    res.end(JSON.stringify(data));
    return;
  }

  // ---- API: Save data (per device) ----
  if (parsed.pathname === '/api/data' && req.method === 'POST') {
    const deviceId = parsed.query.device || 'default';
    let body = '';
    req.on('data', chunk => body += chunk);
    req.on('end', () => {
      try {
        const data = JSON.parse(body);
        writeData(deviceId, data);
        updateMeta(deviceId, req);
        res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end('{"ok":true}');
      } catch(e) {
        res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end('{"error":"Invalid JSON"}');
      }
    });
    return;
  }

  // ---- API: Check if device ID exists ----
  if (parsed.pathname === '/api/check-id' && req.method === 'GET') {
    const deviceId = parsed.query.device || '';
    const safe = deviceId.replace(/[^a-zA-Z0-9_-]/g, '');
    if (!safe) {
      res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end('{"error":"缺少 device 参数"}');
      return;
    }
    const exists = fs.existsSync(dataFile(safe)) || fs.existsSync(memoFile(safe));
    res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
    res.end(JSON.stringify({ exists }));
    return;
  }

  // ---- API: Get memo ----
  if (parsed.pathname === '/api/memo' && req.method === 'GET') {
    const deviceId = parsed.query.device || 'default';
    const memo = readMemo(deviceId);
    res.writeHead(200, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end(memo);
    return;
  }

  // ---- API: Save memo ----
  if (parsed.pathname === '/api/memo' && req.method === 'POST') {
    const deviceId = parsed.query.device || 'default';
    let body = '';
    req.on('data', chunk => body += chunk);
    req.on('end', () => {
      writeMemo(deviceId, body);
      res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end('{"ok":true}');
    });
    return;
  }

  // ---- Proxy ----
  if (parsed.pathname === '/proxy') {
    const targetUrl = parsed.query.url;
    if (!targetUrl) {
      res.writeHead(400, { 'Content-Type': 'text/plain; charset=utf-8' });
      res.end('缺少 url 参数');
      return;
    }

    const mod = targetUrl.startsWith('https') ? https : http;
    const proxyReq = mod.get(targetUrl, { timeout: 15000 }, (proxyRes) => {
      const chunks = [];
      proxyRes.on('data', c => chunks.push(c));
      proxyRes.on('end', () => {
        const body = Buffer.concat(chunks).toString('utf-8');
        res.writeHead(200, { 'Content-Type': 'text/plain; charset=utf-8' });
        res.end(body);
      });
    });

    proxyReq.on('error', (err) => {
      res.writeHead(502, { 'Content-Type': 'text/plain; charset=utf-8' });
      res.end('代理请求失败: ' + err.message);
    });

    proxyReq.on('timeout', () => {
      proxyReq.destroy();
      res.writeHead(504, { 'Content-Type': 'text/plain; charset=utf-8' });
      res.end('代理请求超时');
    });

    return;
  }

  // ---- Admin API: Verify password ----
  if (parsed.pathname === '/api/admin/login' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => body += chunk);
    req.on('end', () => {
      try {
        const { password } = JSON.parse(body);
        if (password === ADMIN_PASSWORD) {
          res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
          res.end('{"ok":true}');
        } else {
          res.writeHead(401, { 'Content-Type': 'application/json; charset=utf-8' });
          res.end('{"error":"密码错误"}');
        }
      } catch(e) {
        res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end('{"error":"Invalid JSON"}');
      }
    });
    return;
  }

  // ---- Admin API: List all devices ----
  if (parsed.pathname === '/api/admin/devices' && req.method === 'GET') {
    if (!checkAdminAuth(req, res)) return;
    try {
      const files = fs.readdirSync(DATA_DIR);
      const deviceMap = {};
      for (const f of files) {
        // Skip meta and memo files in the main match
        if (f.endsWith('_meta.json') || f.endsWith('_memo.txt')) continue;
        const match = f.match(/^(.+)\.json$/);
        if (match) {
          const id = match[1];
          if (!deviceMap[id]) deviceMap[id] = {};
          try {
            const data = JSON.parse(fs.readFileSync(path.join(DATA_DIR, f), 'utf-8'));
            const stat = fs.statSync(path.join(DATA_DIR, f));
            deviceMap[id].count = data.length;
            deviceMap[id].done = data.filter(r => r.status === 'done').length;
            deviceMap[id].error = data.filter(r => r.status === 'error').length;
            deviceMap[id].pending = data.filter(r => !r.status || r.status === '' || r.status === 'fetched').length;
            deviceMap[id].updatedAt = stat.mtimeMs;
          } catch(e) {
            deviceMap[id].count = 0;
            deviceMap[id].done = 0;
            deviceMap[id].error = 0;
            deviceMap[id].pending = 0;
            deviceMap[id].updatedAt = 0;
          }
        }
      }
      // Load memo and meta for each device
      for (const id of Object.keys(deviceMap)) {
        // Memo
        try {
          const mf = memoFile(id);
          if (fs.existsSync(mf)) {
            const memo = fs.readFileSync(mf, 'utf-8');
            deviceMap[id].memo = memo.length > 100 ? memo.substring(0, 100) + '...' : memo;
          }
        } catch(e) {}
        // Meta (device info + IP)
        try {
          const meta = readMeta(id);
          if (meta) {
            deviceMap[id].ip = meta.ip || '';
            deviceMap[id].userAgent = meta.userAgent || '';
            deviceMap[id].firstSeen = meta.firstSeen || 0;
            deviceMap[id].lastSeen = meta.lastSeen || 0;
          }
        } catch(e) {}
      }
      const devices = Object.keys(deviceMap).map(id => ({
        id,
        count: deviceMap[id].count || 0,
        done: deviceMap[id].done || 0,
        error: deviceMap[id].error || 0,
        pending: deviceMap[id].pending || 0,
        memo: deviceMap[id].memo || '',
        updatedAt: deviceMap[id].updatedAt || 0,
        ip: deviceMap[id].ip || '',
        userAgent: deviceMap[id].userAgent || '',
        firstSeen: deviceMap[id].firstSeen || 0,
        lastSeen: deviceMap[id].lastSeen || 0,
      }));
      devices.sort((a, b) => b.updatedAt - a.updatedAt);
      res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify(devices));
    } catch(e) {
      res.writeHead(500, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify({ error: e.message }));
    }
    return;
  }

  // ---- Admin API: Delete device ----
  if (parsed.pathname === '/api/admin/device' && req.method === 'DELETE') {
    if (!checkAdminAuth(req, res)) return;
    const deviceId = parsed.query.device || '';
    const safe = deviceId.replace(/[^a-zA-Z0-9_-]/g, '');
    if (!safe) {
      res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end('{"error":"缺少 device 参数"}');
      return;
    }
    let deleted = 0;
    const df = dataFile(safe);
    const mf = memoFile(safe);
    const mtf = metaFile(safe);
    if (fs.existsSync(df)) { fs.unlinkSync(df); deleted++; }
    if (fs.existsSync(mf)) { fs.unlinkSync(mf); deleted++; }
    if (fs.existsSync(mtf)) { fs.unlinkSync(mtf); deleted++; }
    res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
    res.end(JSON.stringify({ ok: true, deleted }));
    return;
  }

  // ---- Serve HTML ----
  if (parsed.pathname === '/' || parsed.pathname === '/index.html') {
    const htmlPath = path.join(__dirname, 'sms-code-tool.html');
    fs.readFile(htmlPath, (err, data) => {
      if (err) { res.writeHead(500); res.end('读取文件失败'); return; }
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end(data);
    });
    return;
  }

  // ---- Serve Admin HTML ----
  if (parsed.pathname === '/admin' || parsed.pathname === '/admin.html') {
    const htmlPath = path.join(__dirname, 'sms-admin.html');
    fs.readFile(htmlPath, (err, data) => {
      if (err) { res.writeHead(500); res.end('读取文件失败'); return; }
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end(data);
    });
    return;
  }

  res.writeHead(404);
  res.end('Not Found');
});

server.listen(PORT, '0.0.0.0', () => {
  const ips = getLanIps();
  console.log('========================================');
  console.log('  短信验证码工具已启动');
  console.log('----------------------------------------');
  console.log(`  本机访问:  http://localhost:${PORT}`);
  ips.forEach(ip => console.log(`  局域网:    http://${ip}:${PORT}`));
  console.log('----------------------------------------');
  console.log('  每台设备数据独立，互不干扰');
  console.log('  数据目录: sms-tool-data/');
  console.log('========================================');
});
