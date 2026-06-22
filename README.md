# 📱 WhatsApp Interview Scheduler

An end-to-end bulk WhatsApp interview invitation platform that lets HR teams upload an Excel sheet of candidates and send personalized interview invitations via WhatsApp — all from a modern web dashboard.

🌐 **Live Demo:** [https://whatsapp-inteview-scheduler.pages.dev](https://whatsapp-inteview-scheduler.pages.dev)

---

## ✨ Features

- 📤 **Bulk Excel Upload** — Upload candidate data in `.xlsx` format with auto-validation
- 💬 **WhatsApp Messaging** — Send personalized interview invitations via WhatsApp Web (free, no paid API required)
- 📋 **Template Management** — Create, edit, and preview message templates with live WhatsApp-style preview
- 📊 **Live Dashboard** — Real-time KPIs, batch status tracking, and per-message audit logs
- 🔁 **Retry Failed** — Automatically retry only failed sends without re-processing the entire batch
- 🔒 **Validation** — Per-row data validation with detailed error reporting before sending

---

## 🏗️ Architecture

The project consists of **three independently runnable services**:

```
whatsapp-inteview-scheduler-am/
├── interview-scheduler-ui/   # Angular 18 Frontend  (port 4200)
├── interview-scheduler/      # Spring Boot Backend   (port 8080)
└── wa-bridge/                # Node.js WhatsApp Bridge (port 3000)
```

```
┌─────────────────────┐       REST API        ┌─────────────────────┐
│  Angular 18 UI      │ ◄──────────────────► │  Spring Boot API    │
│  (Cloudflare Pages) │      localhost:8080    │   (Java 17)         │
└─────────────────────┘                        └──────────┬──────────┘
                                                           │ HTTP POST /send
                                                           ▼
                                               ┌─────────────────────┐
                                               │  WA Bridge (Node.js)│
                                               │  whatsapp-web.js    │
                                               │  (Unofficial API)   │
                                               └─────────────────────┘
```

---

## 🛠️ Tech Stack

| Layer        | Technology                                                         |
|--------------|--------------------------------------------------------------------|
| **Frontend** | Angular 18, Angular Material, TailwindCSS, SCSS, RxJS             |
| **Backend**  | Java 17, Spring Boot 3.3, Spring Data JPA, Spring WebFlux, Maven  |
| **Database** | H2 (dev / in-memory) · PostgreSQL (production)                    |
| **WA Bridge**| Node.js, Express, whatsapp-web.js, Puppeteer                      |
| **Hosting**  | Cloudflare Pages (UI) · Docker-ready (Backend + WA Bridge)        |

---

## 📋 Prerequisites

Make sure the following tools are installed before running the project locally:

| Tool              | Version     | Download Link                                          |
|-------------------|-------------|--------------------------------------------------------|
| **Node.js**       | 18+         | https://nodejs.org                                     |
| **Java JDK**      | 17+         | https://adoptium.net                                   |
| **Maven**         | 3.9+        | https://maven.apache.org/download.cgi                  |
| **Git**           | Latest      | https://git-scm.com                                    |

---

## 🚀 Getting Started (Local Setup)

### 1. Clone the Repository

```bash
git clone https://github.com/SakthivelSwe/whatsapp-inteview-scheduler-am.git
cd whatsapp-inteview-scheduler-am
```

---

### 2. Start the Spring Boot Backend

```bash
cd interview-scheduler
mvn spring-boot:run
```

The backend will start on **http://localhost:8080**

| URL                                    | Purpose                             |
|----------------------------------------|-------------------------------------|
| `http://localhost:8080/swagger-ui.html`| Swagger / OpenAPI interactive docs  |
| `http://localhost:8080/h2-console`     | H2 in-memory DB console (dev only)  |

> **H2 Console credentials:**
> - JDBC URL: `jdbc:h2:mem:interviewdb`
> - Username: `sa`
> - Password: *(leave blank)*

---

### 3. Start the Angular Frontend

Open a **new terminal window**:

```bash
cd interview-scheduler-ui
npm install
npm start
```

The UI will be available at **http://localhost:4200**

---

### 4. Start the WhatsApp Bridge (Optional — for real WhatsApp sending)

Open **another new terminal window**:

```bash
cd wa-bridge
npm install
npm start
```

The WA Bridge will start on **http://localhost:3000**

#### Link Your WhatsApp Number (one-time setup):

1. Open **http://localhost:3000/qr** in your browser
2. Open WhatsApp on your **sender phone** (use a dedicated/dummy number — never a personal account)
3. Go to **Settings → Linked Devices → Link a Device**
4. Scan the QR code shown on the page
5. The page will show **✅ Connected** when successfully linked

> **Session is saved** to `wa-bridge/.wwebjs_auth/` — you only need to scan once. Keep this folder secret.

---

## 📁 Excel File Format

Upload an `.xlsx` file with the following columns (headers are case/space-insensitive):

| Column Header    | Template Placeholder | Example                          |
|------------------|----------------------|----------------------------------|
| Phone Number     | *(system field)*     | `+919342627033` or `9342627033`  |
| Candidate Name   | `{{column_1}}`       | John Doe                         |
| Job Position     | `{{column_2}}`       | Angular Developer                |
| Interview Date   | `{{column_3}}`       | 09th June 2026                   |
| Interview Time   | `{{column_4}}`       | 11:30 AM                         |
| Meeting Link     | `{{column_5}}`       | https://meet.google.com/abc-xyz  |

> **Phone normalization:** Spaces/dashes are stripped; `+91` is auto-prefixed for 10-digit Indian numbers.

---

## 🖥️ Application Routes

| Route             | Description                                                       |
|-------------------|-------------------------------------------------------------------|
| `/dashboard`      | KPIs and recent batch summary                                     |
| `/upload`         | Drag-and-drop Excel uploader with validation report               |
| `/batches`        | List of all tracked batches                                       |
| `/batches/:id`    | Batch detail: send all, retry failed, live progress, logs         |
| `/templates`      | Create/edit/delete message templates with live WhatsApp preview   |

---

## 🔌 Backend API Endpoints

| Method   | Endpoint                              | Purpose                              |
|----------|---------------------------------------|--------------------------------------|
| `POST`   | `/api/candidates/upload`              | Upload Excel + validate rows         |
| `GET`    | `/api/candidates?batchId={id}`        | List candidates in a batch           |
| `GET`    | `/api/templates`                      | List all templates                   |
| `POST`   | `/api/templates`                      | Create a new template                |
| `PUT`    | `/api/templates/{id}`                 | Update a template                    |
| `DELETE` | `/api/templates/{id}`                 | Delete a template                    |
| `POST`   | `/api/messages/send-all`              | Trigger bulk send for a batch        |
| `POST`   | `/api/messages/retry-failed`          | Retry only FAILED rows               |
| `GET`    | `/api/messages/status/{batchId}`      | Get batch status summary             |
| `GET`    | `/api/messages/logs/{batchId}`        | Get per-message audit log            |

---

## 🐳 Running with Docker

### Backend

```bash
cd interview-scheduler
docker build -t interview-scheduler .
docker run -p 8080:8080 interview-scheduler
```

### WA Bridge

```bash
cd wa-bridge
docker build -t wa-bridge .
docker run -p 3000:3000 wa-bridge
```

---

## ⚙️ WhatsApp Provider Configuration

Set the provider in `interview-scheduler/src/main/resources/application.yml`:

```yaml
whatsapp:
  provider: waweb         # Options: mock | waweb | meta | twilio
  rate-limit:
    delay-seconds: 1.5    # Delay between messages (1–2 seconds recommended)
    max-concurrent: 20
```

| Provider | Description                                                       |
|----------|-------------------------------------------------------------------|
| `mock`   | Logs messages locally — no real WhatsApp needed (default for dev) |
| `waweb`  | Uses the local WA Bridge (whatsapp-web.js — free, unofficial)     |
| `meta`   | Meta WhatsApp Cloud API (set `META_PHONE_NUMBER_ID`, `META_ACCESS_TOKEN`) |
| `twilio` | Twilio WhatsApp (set `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM`) |

---

## 🔗 Deployed URL

| Service  | URL                                                                 |
|----------|---------------------------------------------------------------------|
| **UI**   | [https://whatsapp-inteview-scheduler.pages.dev](https://whatsapp-inteview-scheduler.pages.dev) |

> **Note:** The deployed UI connects to a configured backend. For full local functionality (actual WhatsApp sending), run all three services locally as described above.

---

## 📂 Project Structure

```
whatsapp-inteview-scheduler-am/
│
├── interview-scheduler-ui/          # Angular 18 Frontend
│   ├── src/
│   │   ├── app/                     # Components, services, routes
│   │   └── styles.scss              # Design tokens & global styles
│   ├── angular.json
│   └── package.json
│
├── interview-scheduler/             # Spring Boot Backend
│   ├── src/main/java/com/dummby/
│   │   ├── config/                  # CORS, caching, async config
│   │   ├── controller/              # REST controllers
│   │   ├── model/                   # JPA entities
│   │   ├── repository/              # Spring Data repositories
│   │   ├── service/                 # Business logic, WhatsApp providers
│   │   └── util/                    # Excel parser, sample generator
│   ├── Dockerfile
│   └── pom.xml
│
├── wa-bridge/                       # Node.js WhatsApp Web Bridge
│   ├── server.js                    # Express server + whatsapp-web.js
│   ├── Dockerfile
│   └── package.json
│
├── sample-candidates.xlsx           # Sample Excel for testing
└── README.md
```

---

## 🧪 Quick Test (No Real WhatsApp Needed)

1. Start the backend: `mvn spring-boot:run` (uses `mock` provider by default)
2. Start the frontend: `npm start` in `interview-scheduler-ui/`
3. Open **http://localhost:4200**
4. Upload `sample-candidates.xlsx` from the project root
5. Click **Send All** — messages are logged to the backend console as `[MOCK-WHATSAPP]`
6. Check batch status and audit logs from the UI

---

## ⚠️ Anti-Ban Tips (WA Bridge)

- Keep delay between messages **≥ 5 seconds**
- Use a **dedicated/dummy sender number** — never a personal account
- Each message is personalized (name, role, time, link) — avoids spam detection
- For high-volume production use, switch to **Meta WhatsApp Cloud API**
- Don't run two bridges with the same number simultaneously

---

## 📄 License

This project is for internal use by **TVM Infotech Pvt. Ltd.**

---

*Built with ❤️ for streamlining HR interview scheduling via WhatsApp.*
