# WhatsApp Web Bridge (Free)

Tiny Node.js service that lets the Spring Boot Interview Scheduler send WhatsApp
messages from **any WhatsApp number you scan with the QR** without any paid API.
Use a dedicated/dummy sender number — never link a personal account.

It uses [`whatsapp-web.js`](https://github.com/pedroslopez/whatsapp-web.js)
which automates WhatsApp Web behind the scenes. **This is unofficial** — keep
delays between messages high (5–30 s) to reduce ban risk and never send spam.

## Setup (one-time)

```powershell
cd "C:\AI projects\dummby-project\wa-bridge"
npm install
npm start
```

Then open <http://localhost:3000/qr> in your browser and scan the QR with the
WhatsApp number you want to send from:

1. Open WhatsApp on the **sender phone** (any number — use a dedicated/dummy one)
2. **Settings → Linked Devices → Link a Device**
3. Scan the QR shown on the page
4. The page will say **✅ Connected**

Session is persisted to `./.wwebjs_auth/` — you only scan once. Keep this
folder secret; it is the device login.

## Endpoints

| Method | Path                          | Purpose                          |
| ------ | ----------------------------- | -------------------------------- |
| GET    | `/status`                     | `{ ready, hasQr, info }`         |
| GET    | `/qr`                         | HTML page with QR (auto-refresh) |
| GET    | `/qr.png`                     | Raw QR PNG                       |
| POST   | `/send`  `{ to, message }`    | Send a WhatsApp text message     |

`to` accepts `9342627033`, `+919342627033`, or `919342627033` (any format —
the bridge normalises and prefixes `91` for 10-digit numbers).

## Safety / Anti-ban tips

- Keep delay between messages ≥ **5 seconds** (configurable in Spring Boot via
  `whatsapp.rate-limit.delay-seconds`).
- Do not send identical text 100 times — the template already personalises
  candidate name, role, time, link, so each message is unique. Good.
- Don't run two bridges with the same number at once.
- New numbers are more likely to be banned. Numbers older than 6 months are
  safer.
- For production / high volume, switch to **Meta WhatsApp Cloud API**.

