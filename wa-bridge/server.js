/**
 * WhatsApp Web Bridge
 * --------------------
 * A tiny Express service that wraps whatsapp-web.js so the Spring Boot
 * Interview Scheduler can send WhatsApp messages from ANY WhatsApp number
 * (you scan a QR with the phone whose number should be the sender) for FREE.
 *
 * First run:
 *   1. npm install
 *   2. npm start
 *   3. Open http://localhost:3000/qr  in a browser
 *   4. Scan the QR with WhatsApp on the company phone
 *      (WhatsApp -> Settings -> Linked Devices -> Link a Device)
 *   5. Session is saved to ./.wwebjs_auth so you don't scan again
 *
 * Endpoints:
 *   GET  /status                 -> { ready, hasQr, info }
 *   GET  /qr                     -> HTML page that shows the QR (auto-refresh)
 *   GET  /qr.png                 -> raw QR image (PNG)
 *   POST /send { to, message }   -> { success, id, error }
 *
 * IMPORTANT:
 *   - This uses an UNOFFICIAL method (WhatsApp Web automation).
 *   - WhatsApp may ban numbers that send too fast or look automated.
 *   - Keep per-message delay >= 1-2 seconds. For best safety: 5-30 seconds.
 *   - Do not run this on more than one machine for the same number.
 */

const express = require('express');
const qrcode = require('qrcode');
const fs = require('fs');
const path = require('path');
const { Client, LocalAuth } = require('whatsapp-web.js');

const PORT = process.env.PORT || 3000;
const AUTH_PATH = path.join(__dirname, '.wwebjs_auth');

const app = express();
app.use(express.json({ limit: '1mb' }));

let lastQr = null;          // latest QR string (refreshes ~every 20s until scanned)
let isReady = false;        // true once WhatsApp Web is authenticated
let clientInfo = null;      // wid, pushname after ready
let lastState = 'STARTING'; // human-readable lifecycle state for the UI
let initStartedAt = Date.now();
let client = null;

/**
 * Build a fresh whatsapp-web.js client. We pin the WhatsApp Web version via a
 * remote cache — this is the single most common fix for the "bridge starts but
 * never shows a QR" problem, which happens when WhatsApp ships a new web build
 * that the bundled selectors don't recognise.
 */
function buildClient() {
  return new Client({
    authStrategy: new LocalAuth({ dataPath: AUTH_PATH }),
    takeoverOnConflict: true,        // grab the session if another tab/instance holds it
    takeoverTimeoutMs: 10000,
    qrMaxRetries: 0,                 // keep regenerating QR forever until scanned
    webVersionCache: {
      type: 'remote',
      remotePath:
        'https://raw.githubusercontent.com/wppconnect-team/wa-version/main/html/2.3000.1023204200.html'
    },
    puppeteer: {
      headless: true,
      args: [
        '--no-sandbox',
        '--disable-setuid-sandbox',
        '--disable-dev-shm-usage',
        '--disable-accelerated-2d-canvas',
        '--no-first-run',
        '--no-zygote',
        '--disable-gpu'
      ]
    }
  });
}

function wireEvents(c) {
  c.on('qr', (qr) => {
    lastQr = qr;
    lastState = 'QR_READY';
    console.log('[wa-bridge] ✅ QR generated. Open http://localhost:' + PORT + '/qr to scan.');
  });

  c.on('loading_screen', (percent, message) => {
    lastState = 'LOADING ' + percent + '%';
    console.log('[wa-bridge] loading', percent + '%', message || '');
  });

  c.on('authenticated', () => {
    lastState = 'AUTHENTICATED';
    lastQr = null;
    console.log('[wa-bridge] Authenticated. Waiting for ready...');
  });

  c.on('change_state', (state) => {
    lastState = String(state);
    console.log('[wa-bridge] state ->', state);
  });

  c.on('ready', () => {
    isReady = true;
    lastQr = null;
    lastState = 'READY';
    clientInfo = {
      wid: c.info && c.info.wid && c.info.wid._serialized,
      pushname: c.info && c.info.pushname,
      platform: c.info && c.info.platform
    };
    console.log('[wa-bridge] 🎉 WhatsApp Web ready as:', clientInfo);
  });

  c.on('disconnected', (reason) => {
    console.warn('[wa-bridge] Disconnected:', reason);
    isReady = false;
    clientInfo = null;
    lastState = 'DISCONNECTED';
    // Auto-recover so the UI gets a fresh QR without a manual restart.
    safeReinit(1500);
  });

  c.on('auth_failure', (msg) => {
    console.error('[wa-bridge] Auth failure:', msg);
    isReady = false;
    lastState = 'AUTH_FAILURE';
  });
}

function startClient() {
  initStartedAt = Date.now();
  lastState = 'INITIALIZING';
  client = buildClient();
  wireEvents(client);
  client.initialize().catch((err) => {
    lastState = 'INIT_FAILED';
    console.error('[wa-bridge] initialize() failed:', err && err.message);
    // Retry once after a short delay
    safeReinit(3000);
  });
}

/** Destroy the current client (best-effort) and start a fresh one. */
let reinitTimer = null;
function safeReinit(delayMs) {
  if (reinitTimer) return; // already scheduled
  reinitTimer = setTimeout(async () => {
    reinitTimer = null;
    try { if (client) await client.destroy(); } catch (_) {}
    isReady = false;
    lastQr = null;
    clientInfo = null;
    startClient();
  }, delayMs || 1000);
}

/** Wipe the saved session folder so a brand-new QR is forced. */
function wipeAuth() {
  try {
    if (fs.existsSync(AUTH_PATH)) {
      fs.rmSync(AUTH_PATH, { recursive: true, force: true });
      console.log('[wa-bridge] 🧹 Cleared saved session at', AUTH_PATH);
    }
  } catch (e) {
    console.error('[wa-bridge] Could not clear auth folder:', e.message);
  }
}

/**
 * Watchdog: if after 75 seconds we are neither READY nor have a QR, the client
 * is stuck (corrupt session / web-version mismatch). Wipe the session and
 * re-initialise so the user always eventually gets a scannable QR.
 */
setInterval(() => {
  const stalledMs = Date.now() - initStartedAt;
  if (!isReady && !lastQr && stalledMs > 75000) {
    console.warn('[wa-bridge] ⚠ Stuck with no QR for ' + Math.round(stalledMs / 1000) +
      's — wiping session and restarting.');
    wipeAuth();
    safeReinit(500);
  }
}, 15000);

startClient();

// ---------- Helpers ----------
function normalizeNumber(raw) {
  if (!raw) return null;
  let s = String(raw).replace(/[^0-9]/g, '');
  if (!s) return null;
  // Assume +91 for 10-digit Indian numbers
  if (s.length === 10) s = '91' + s;
  return s;
}

// ---------- Routes ----------
app.get('/status', (req, res) => {
  res.json({
    ready: isReady,
    hasQr: !!lastQr,
    state: lastState,
    info: clientInfo
  });
});

app.get('/qr.png', async (req, res) => {
  if (isReady) return res.status(204).end();
  if (!lastQr) return res.status(202).send('QR not generated yet, refresh in a moment.');
  try {
    const buf = await qrcode.toBuffer(lastQr, { type: 'png', width: 320, margin: 1 });
    res.set('Content-Type', 'image/png');
    res.set('Cache-Control', 'no-store');
    res.send(buf);
  } catch (e) {
    res.status(500).send('QR render error: ' + e.message);
  }
});

app.get('/qr', (req, res) => {
  res.set('Content-Type', 'text/html; charset=utf-8');
  res.send(`<!doctype html>
<html><head><meta charset="utf-8"><title>WhatsApp Bridge - Link Device</title>
<meta http-equiv="refresh" content="5">
<style>
body{font-family:'Segoe UI',sans-serif;background:#0f172a;color:#e2e8f0;display:flex;align-items:center;justify-content:center;min-height:100vh;margin:0}
.card{background:#1e293b;padding:32px;border-radius:16px;box-shadow:0 10px 30px rgba(0,0,0,.4);max-width:420px;text-align:center}
h1{margin:0 0 8px;font-size:20px;color:#fff}
p{color:#94a3b8;font-size:13px;line-height:1.6;margin:8px 0}
img{background:#fff;padding:12px;border-radius:12px;margin:16px 0}
.ok{color:#34d399;font-size:16px;font-weight:600}
ol{text-align:left;color:#cbd5e1;font-size:13px;line-height:1.7;padding-left:20px}
code{background:#0f172a;padding:2px 6px;border-radius:4px;color:#fbbf24}
</style></head>
<body>
<div class="card">
  <h1>📱 Link WhatsApp Web</h1>
  ${isReady
    ? `<p class="ok">✅ Connected</p><p>Logged in as <b>${clientInfo?.pushname || ''}</b></p><p>${clientInfo?.wid || ''}</p>`
    : `<img src="/qr.png?ts=${Date.now()}" width="320" height="320" alt="QR"/>
       <ol>
         <li>Open WhatsApp on the sender phone (any WhatsApp number)</li>
         <li>Tap <b>Settings → Linked Devices</b></li>
         <li>Tap <b>Link a Device</b> and scan this QR</li>
       </ol>
       <p>Page refreshes every 5s.</p>`}
</div>
</body></html>`);
});

app.get('/', (req, res) => res.redirect('/qr'));

app.post('/send', async (req, res) => {
  if (!isReady) {
    return res.status(503).json({ success: false, error: 'WhatsApp not connected. Visit /qr to link a device.' });
  }
  const { to, message } = req.body || {};
  const num = normalizeNumber(to);
  if (!num) return res.status(400).json({ success: false, error: 'Invalid "to" number' });
  if (!message) return res.status(400).json({ success: false, error: 'Missing "message"' });

  try {
    const chatId = num + '@c.us';
    // Optional: verify number is on WhatsApp before sending
    const isOnWa = await client.isRegisteredUser(chatId);
    if (!isOnWa) {
      return res.status(404).json({ success: false, error: 'Number not registered on WhatsApp: +' + num });
    }
    const sent = await client.sendMessage(chatId, message);
    res.json({ success: true, id: sent.id && sent.id._serialized });
  } catch (e) {
    console.error('[wa-bridge] send error:', e);
    res.status(500).json({ success: false, error: e.message });
  }
});

/**
 * Disconnect / unlink the current WhatsApp session.
 * Logs out the linked device (so the QR will re-appear) and reinitialises
 * the client so a new device can be linked without restarting the process.
 */
app.post('/logout', async (req, res) => {
  try {
    if (isReady && client) {
      try { await client.logout(); } catch (_) {}
      console.log('[wa-bridge] Logged out by request.');
    }
    isReady = false;
    clientInfo = null;
    lastQr = null;
    lastState = 'LOGGING_OUT';
    safeReinit(800);
    res.json({ success: true });
  } catch (e) {
    console.error('[wa-bridge] logout error:', e);
    res.status(500).json({ success: false, error: e.message });
  }
});

/**
 * Hard reset: wipe the saved session entirely and force a brand-new QR.
 * Use this when the QR is stuck / corrupt and a normal logout doesn't help.
 */
app.post('/reset', async (req, res) => {
  try {
    console.log('[wa-bridge] 🔄 Hard reset requested.');
    try { if (client) await client.destroy(); } catch (_) {}
    isReady = false;
    clientInfo = null;
    lastQr = null;
    lastState = 'RESETTING';
    wipeAuth();
    safeReinit(800);
    res.json({ success: true, message: 'Session wiped. A fresh QR will appear shortly.' });
  } catch (e) {
    console.error('[wa-bridge] reset error:', e);
    res.status(500).json({ success: false, error: e.message });
  }
});

app.listen(PORT, () => {
  console.log(`[wa-bridge] HTTP listening on http://localhost:${PORT}`);
  console.log(`[wa-bridge] Open http://localhost:${PORT}/qr to link your WhatsApp.`);
});

