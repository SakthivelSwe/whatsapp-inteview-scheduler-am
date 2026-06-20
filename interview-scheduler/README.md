# WhatsApp Interview Scheduler – Backend

Spring Boot backend to upload an Excel of candidates and send templated WhatsApp interview invitations in bulk. Implements the **Bulk WhatsApp Automation with Dynamic Templates** Requirement Document (RD).

## Tech Stack
- Java 17, Spring Boot 3.3
- Spring Data JPA + H2 (dev) / PostgreSQL (prod)
- Apache POI for Excel parsing
- Spring WebFlux WebClient for WhatsApp HTTP calls
- Spring Retry, @Async for resilient bulk send
- SpringDoc OpenAPI (Swagger UI)

## Run

```powershell
cd interview-scheduler
mvn spring-boot:run
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 console: http://localhost:8080/h2-console  (JDBC URL: `jdbc:h2:mem:interviewdb`, user `sa`, no password)

## WhatsApp Provider

Set in `application.yml` → `whatsapp.provider`:
- `mock` (default) – logs messages locally, no real API needed
- `meta`  – Meta WhatsApp Cloud API (set `META_PHONE_NUMBER_ID`, `META_ACCESS_TOKEN`)
- `twilio` – Twilio WhatsApp (set `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM`)

## Excel Format (per RD §3)

First sheet, first row = header. **6 mandatory columns** (headers are case/space-insensitive):

| Expected Header | Template Placeholder | Example |
|---|---|---|
| Phone Number   | (system field)  | `+919342627033` or `9342627033` |
| Candidate Name | `{{column_1}}`  | Godson Robin Raja S |
| Job Position   | `{{column_2}}`  | Angular Developer |
| Interview Date | `{{column_3}}`  | 09th June 2026 |
| Interview Time | `{{column_4}}`  | 11:30 AM |
| Meeting Link   | `{{column_5}}`  | https://meet.google.com/xwe-ivrc-pet |

**Phone normalization (RD §2.B):** spaces/dashes stripped; `+91` auto-prefixed if the value is a 10-digit Indian number.

**Validation (RD §2.B):** rows missing Phone Number, Candidate Name, or Meeting Link are flagged `INVALID` (persisted with a `validationError`) and skipped at send time. The upload response returns the per-row error list.

**Custom Column Mapping (RD §2.A — optional):** if the user's headers don't match, send a JSON map in the multipart field `columnMapping`, e.g.
```json
{"Mobile": "phoneNumber", "Full Name": "candidateName"}
```

## API Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/candidates/upload` (multipart `file`, optional `columnMapping`) | Upload Excel + validate |
| GET  | `/api/candidates?batchId={id}` | List candidates of batch |
| GET  | `/api/templates` | List templates |
| POST | `/api/templates` | Create template |
| PUT  | `/api/templates/{id}` | Update template |
| DELETE | `/api/templates/{id}` | Delete template |
| POST | `/api/messages/send-all` `{ "batchId":"…","templateName":"…" }` | Trigger bulk send (skips INVALID) |
| POST | `/api/messages/retry-failed` `{ "batchId":"…" }` | **RD §5** – retry only the FAILED rows |
| GET  | `/api/messages/status/{batchId}` | Summary report (total, sent, failed, invalid) + list of failed phone numbers |
| GET  | `/api/messages/logs/{batchId}` | Per-message audit log |

## Default Template (RD §4 — TVM Infotech)

Auto-seeded on startup as `tvm_interview_invite`. Excerpt:

```
Hello {{column_1}},

Greetings from TVM Infotech Pvt. Ltd.!

We are pleased to inform you that your interview for the {{column_2}} position
(Angular / React / Java / Full Stack / HR) has been scheduled.

Interview Details:
📅 Date: {{column_3}}
⏰ Time: {{column_4}}
💻 Mode: Online (Google Meet)
🔗 Meeting Link: {{column_5}}
...
✅ Action Required:
If you agree to the above terms and wish to participate, kindly reply "CONFIRMED".

Best regards,
HR – TVM Infotech Pvt. Ltd.
```

Placeholders: `{{column_1}}..{{column_5}}` (plus friendly aliases `{{candidateName}}`, `{{jobPosition}}`, `{{interviewDate}}`, `{{interviewTime}}`, `{{meetingLink}}`).

## Rate Limiting (RD §2.B)

Configurable in `application.yml`:
```yaml
whatsapp:
  rate-limit:
    delay-seconds: 1.5     # 1–2 seconds recommended
    max-concurrent: 20
```

## Quick Test (mock provider, no real WhatsApp needed)

1. Generate a sample Excel: run `com.dummby.interviewscheduler.util.SampleExcelGenerator` → creates `sample-candidates.xlsx` (4 rows; 1 intentionally invalid).
2. `mvn spring-boot:run`
3. `POST /api/candidates/upload` with the file → response shows `validCandidates=3, invalidCandidates=1, errors=[...]`.
4. `POST /api/messages/send-all` `{"batchId":"<id>"}` → console prints `[MOCK-WHATSAPP]` for each sent row.
5. `GET /api/messages/status/{batchId}` → full summary report incl. `failedRecords[]`.
6. `POST /api/messages/retry-failed` `{"batchId":"<id>"}` → re-attempts FAILED rows only.

## Production Notes

- For Meta Cloud API, business-initiated messages **must** use a pre-approved template. `MetaWhatsAppProvider` currently sends plain text (works inside the 24-hour customer window only). Switch the payload to `"type":"template"` for production interview invites.
- Tune `whatsapp.rate-limit.delay-seconds` to stay within provider quotas.
- Switch to PostgreSQL by overriding `spring.datasource.*` properties.
