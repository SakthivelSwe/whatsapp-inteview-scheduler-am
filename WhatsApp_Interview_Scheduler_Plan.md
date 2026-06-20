# WhatsApp Bulk Interview Scheduler – Project Plan

## 📋 Project Overview

A **Spring Boot backend application** that reads candidate details from uploaded Excel files and sends templated WhatsApp messages (interview panel info, role, timing, GMeet link) to **100–1000 candidates** in a single click using the WhatsApp Business API.

---

## 🏗️ Architecture Flow

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  Excel       │    │  Parse       │    │  Store in    │    │  Apply       │    │  WhatsApp    │
│  Upload      │───▶│  (Apache POI)│───▶│  Database    │───▶│  Template    │───▶│  API Send    │
└──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘
                                                                                       │
                                                                                       ▼
                                                                              ┌──────────────┐
                                                                              │  Delivery    │
                                                                              │  Status Track│
                                                                              └──────────────┘
```

---

## 🛠️ Technology Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Backend Framework | Spring Boot 3.x (Java 17+) | REST APIs, business logic |
| Excel Parsing | Apache POI 5.x | Read .xlsx/.xls files |
| Database | PostgreSQL / MySQL | Store candidates, templates, message logs |
| WhatsApp Integration | Meta WhatsApp Business API / Twilio WhatsApp API | Send messages |
| Async Processing | Spring @Async + ThreadPoolExecutor or RabbitMQ | Handle bulk sending |
| API Documentation | SpringDoc OpenAPI (Swagger UI) | API testing & docs |
| Build Tool | Maven / Gradle | Dependency management |
| Validation | Spring Validation (Jakarta) | Input validation |
| Retry Mechanism | Spring Retry | Retry failed messages |

---

## 💰 Cost Clarification – Is This Completely Free?

### ✅ What's FREE (Development & Tools)

| Component | Cost |
|-----------|------|
| Spring Boot | Free (open source) |
| Apache POI (Excel parsing) | Free (open source) |
| PostgreSQL / MySQL | Free (open source) |
| Java 17+ (OpenJDK) | Free (open source) |
| Swagger/SpringDoc | Free (open source) |
| Development tools (IDE, Maven) | Free |
| Postman (API testing) | Free |

### ⚠️ What's PAID (WhatsApp Messaging – Per Message Cost)

| Provider | Free Tier | After Free Tier |
|----------|-----------|-----------------|
| **Meta WhatsApp Cloud API** | 1,000 conversations/month FREE | ~₹0.42–₹6.5 per message |
| **Twilio WhatsApp** | Sandbox FREE (testing only, not production) | ~₹0.42 per message |
| **Ultramsg** | No free tier | Starts ~₹1,100/month |

### 🔴 What About "Free" Unofficial Alternatives?

| Option | Risk |
|--------|------|
| Using personal WhatsApp + automation bots | **Account BAN** – WhatsApp actively detects and bans automated personal accounts |
| Unofficial APIs (wa-automate, Baileys, etc.) | **Account BAN** + violates WhatsApp Terms of Service |
| WhatsApp Web scraping | **Account BAN** + unreliable |

> ⛔ **WARNING:** There is NO legitimate way to send bulk WhatsApp messages for free in production. Unofficial tools WILL get your number permanently banned.

### 📌 Meta Cloud API Free Tier – Important Clarification

| Conversation Type | Who Initiates | Free Tier | Your Use Case? |
|-------------------|---------------|-----------|----------------|
| **Service** | Customer messages YOU first | ✅ 1,000 FREE every month (recurring, resets monthly) | ❌ No |
| **Utility** (interview info, updates) | YOU message customer first | ❌ PAID from message #1 | ✅ YES – This is your case |
| **Marketing** (promos, offers) | YOU message customer first | ❌ PAID from message #1 | ❌ No |
| **Authentication** (OTPs) | YOU message customer first | ❌ PAID from message #1 | ❌ No |

> ⚠️ **YOUR USE CASE = Business-Initiated (Utility):** Since YOU are sending interview details to candidates who haven't messaged you first, the 1,000 free conversations do NOT apply. You pay from the very first message.

> 📝 **The 1,000 free/month is ONLY for Service conversations** (when the candidate messages your WhatsApp Business number first, then you reply).

### 💰 Realistic Cost Estimate (Business-Initiated / Utility)

| Scale | Monthly Cost (India) | Monthly Cost (International) |
|-------|---------------------|------------------------------|
| 100 candidates/month | ~₹30 – ₹42 | ~$1.5 |
| 500 candidates/month | ~₹150 – ₹210 | ~$7.5 |
| 1,000 candidates/month | ~₹300 – ₹420 | ~$15 |
| 5,000 candidates/month | ~₹1,500 – ₹2,100 | ~$75 |
| 10,000 candidates/month | ~₹3,000 – ₹4,200 | ~$150 |

### 🆓 How to Make It Work for FREE (Workaround)

| Approach | How It Works | Limitation |
|----------|-------------|------------|
| **Ask candidates to message first** | Share a QR code/link, candidate sends "Hi" → triggers your automated reply (Service conversation = FREE) | Candidates must initiate; not guaranteed all will |
| **Twilio Sandbox** | Free sandbox for development/testing | Cannot be used in production |

### ✅ Bottom Line

| Aspect | Answer |
|--------|--------|
| **Development cost** | 🟢 Completely FREE |
| **WhatsApp messaging (your use case)** | 🔴 PAID from first message (~₹0.30–₹0.42 per candidate in India) |
| **1000 free/month from Meta** | 🟡 Only for SERVICE conversations (customer messages you first) – NOT for your case |
| **Cheapest option for 1000 candidates** | ~₹300–₹420/month (~$15/month) |
| **Server hosting** | 🟡 Free tier available (AWS/GCP/Render free tier) or ~₹500–₹2000/month for VPS |

---

## 📱 WhatsApp API Options (Free / Paid)

| Provider | Cost | Setup Time | Notes | Link |
|----------|------|-----------|-------|------|
| **Meta WhatsApp Cloud API** | Free: 1000 conversations/month, then ~$0.005–$0.08/msg | 2-3 days (business verification) | Official, most reliable | https://developers.facebook.com/docs/whatsapp/cloud-api |
| **Twilio WhatsApp API** | ~$0.005/msg + Twilio fees, free sandbox for testing | 1 day (sandbox), 2-3 days (production) | Easier setup, great docs | https://www.twilio.com/docs/whatsapp |
| **WATI.io** | Starts ~$49/month | 1 day | No-code dashboard + API | https://www.wati.io |
| **Ultramsg** | Starts ~$13/month | Few hours | Simple REST API, quick setup | https://ultramsg.com |
| **AiSensy** | Starts ~$20/month | 1 day | Indian market focused | https://aisensy.com |

### ✅ Recommendation
- **Development/Testing:** Start with **Twilio WhatsApp Sandbox** (free)
- **Production:** Move to **Meta WhatsApp Cloud API** (cheapest at scale)

---

## 📊 Excel File Format (Expected Input)

| Column | Field | Example | Required |
|--------|-------|---------|----------|
| A | Candidate Name | John Doe | ✅ |
| B | Email ID | john@email.com | ✅ |
| C | WhatsApp Number | +919876543210 | ✅ |
| D | Role | Java Developer | ✅ |
| E | Company Name | TechCorp | ✅ |
| F | Panel Date & Time | 2026-06-20 10:00 AM | ✅ |
| G | GMeet Link | https://meet.google.com/abc-xyz | ✅ |
| H | Interviewer Name | Mr. Smith | Optional |

---

## 📝 Message Template Example

```
Hello {{candidateName}},

Greetings from {{companyName}}! 🎉

We are pleased to inform you that you have been shortlisted for the role of *{{role}}*.

📅 Interview Details:
• Date & Time: {{panelTiming}}
• Mode: Google Meet (Online)
• Meeting Link: {{gmeetLink}}
• Interviewer: {{interviewerName}}

Please join the meeting 5 minutes before the scheduled time.

Best of luck! 🍀

Regards,
{{companyName}} HR Team
```

> ⚠️ **Note:** WhatsApp Business API requires pre-approved message templates for business-initiated messages. Submit this template for approval before production use.

---

## 📁 Project Structure

```
src/main/java/com/yourcompany/interviewscheduler/
│
├── InterviewSchedulerApplication.java          → Main Spring Boot Application
│
├── controller/
│   ├── CandidateController.java                → Upload Excel, list candidates
│   ├── TemplateController.java                 → CRUD for message templates
│   └── MessageController.java                  → Trigger bulk send, check status
│
├── service/
│   ├── ExcelParserService.java                 → Parse Excel using Apache POI
│   ├── TemplateService.java                    → Template CRUD & placeholder resolution
│   ├── WhatsAppService.java                    → WhatsApp API integration (send single msg)
│   └── BulkSendService.java                    → Async bulk sending with rate limiting
│
├── model/
│   ├── entity/
│   │   ├── Candidate.java                      → JPA Entity for candidate data
│   │   ├── MessageTemplate.java                → JPA Entity for templates
│   │   ├── MessageLog.java                     → JPA Entity for delivery tracking
│   │   └── Batch.java                          → JPA Entity for upload batches
│   └── dto/
│       ├── CandidateDTO.java                   → Data transfer object
│       ├── SendRequest.java                    → Bulk send request payload
│       └── SendResponse.java                   → API response wrapper
│
├── repository/
│   ├── CandidateRepository.java                → Spring Data JPA repository
│   ├── TemplateRepository.java
│   ├── MessageLogRepository.java
│   └── BatchRepository.java
│
├── config/
│   ├── AsyncConfig.java                        → Thread pool configuration
│   ├── WhatsAppConfig.java                     → API keys, base URLs
│   └── SwaggerConfig.java                      → OpenAPI documentation config
│
├── exception/
│   ├── GlobalExceptionHandler.java             → @ControllerAdvice
│   ├── ExcelParseException.java
│   └── WhatsAppSendException.java
│
└── util/
    ├── PhoneNumberValidator.java               → Validate & normalize phone numbers
    └── ExcelValidator.java                     → Validate Excel structure
```

---

## 🔌 API Endpoints

| Method | Endpoint | Description | Request Body |
|--------|----------|-------------|--------------|
| POST | `/api/candidates/upload` | Upload Excel file | Multipart file |
| GET | `/api/candidates?batchId={id}` | List candidates by batch | - |
| GET | `/api/candidates/{id}` | Get single candidate | - |
| DELETE | `/api/candidates/batch/{batchId}` | Delete a batch | - |
| POST | `/api/templates` | Create message template | JSON body |
| GET | `/api/templates` | List all templates | - |
| PUT | `/api/templates/{id}` | Update template | JSON body |
| DELETE | `/api/templates/{id}` | Delete template | - |
| POST | `/api/messages/send-all` | Trigger bulk send | `{batchId, templateId}` |
| POST | `/api/messages/send/{candidateId}` | Send to single candidate | `{templateId}` |
| GET | `/api/messages/status/{batchId}` | Check delivery status | - |
| GET | `/api/messages/stats/{batchId}` | Get send statistics | - |

---

## 📐 Database Schema

### candidates
```sql
CREATE TABLE candidates (
    id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    whatsapp_number VARCHAR(20) NOT NULL,
    role VARCHAR(255),
    company_name VARCHAR(255),
    panel_timing TIMESTAMP,
    gmeet_link VARCHAR(500),
    interviewer_name VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW()
);
```

### message_templates
```sql
CREATE TABLE message_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
```

### message_logs
```sql
CREATE TABLE message_logs (
    id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    candidate_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING, SENT, DELIVERED, FAILED
    error_message TEXT,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);
```

### batches
```sql
CREATE TABLE batches (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255),
    total_candidates INT,
    status VARCHAR(20) DEFAULT 'UPLOADED',  -- UPLOADED, SENDING, COMPLETED
    created_at TIMESTAMP DEFAULT NOW()
);
```

---

## 📅 Implementation Phases & Timeline

### Phase 1: Project Setup & Excel Parsing (Days 1–3)

| Day | Task | Deliverable |
|-----|------|-------------|
| 1 | Initialize Spring Boot project, configure dependencies (POI, JPA, PostgreSQL, Swagger) | Working project skeleton |
| 2 | Create JPA entities, repositories, database schema, Flyway/Liquibase migrations | Database ready |
| 3 | Build Excel upload endpoint with Apache POI parsing, validation, and DB persistence | `POST /api/candidates/upload` working |

### Phase 2: Template Engine & Message Builder (Days 4–5)

| Day | Task | Deliverable |
|-----|------|-------------|
| 4 | Create MessageTemplate entity, CRUD service & controller | Template management APIs |
| 5 | Build template resolver (replace placeholders with candidate data), unit tests | Dynamic message generation |

### Phase 3: WhatsApp Integration & Bulk Send (Days 6–9)

| Day | Task | Deliverable |
|-----|------|-------------|
| 6 | Set up Twilio/Meta WhatsApp sandbox, create WhatsAppService with WebClient | Single message send working |
| 7 | Build BulkSendService with @Async, ThreadPoolExecutor, rate limiting | Async bulk sending |
| 8 | Implement MessageLog tracking, status updates, delivery webhooks | Status tracking |
| 9 | Build send-all endpoint, status endpoint, integration testing | Full flow working |

### Phase 4: Error Handling, Rate Limiting & Polish (Days 10–12)

| Day | Task | Deliverable |
|-----|------|-------------|
| 10 | Add Spring Retry for failed messages, duplicate detection, phone number normalization | Robust error handling |
| 11 | Add rate limiting (respect WhatsApp API limits), batch statistics endpoint | Production-ready sending |
| 12 | Swagger documentation, README, Postman collection, final testing | Complete documentation |

### 📊 Total Estimated Time: **10–12 working days** (1 developer)

---

## ⚙️ Key Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Excel Parsing -->
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi-ooxml</artifactId>
        <version>5.2.5</version>
    </dependency>

    <!-- Database -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- WhatsApp via Twilio -->
    <dependency>
        <groupId>com.twilio.sdk</groupId>
        <artifactId>twilio</artifactId>
        <version>9.14.0</version>
    </dependency>

    <!-- API Documentation -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.3.0</version>
    </dependency>

    <!-- Utility -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Retry -->
    <dependency>
        <groupId>org.springframework.retry</groupId>
        <artifactId>spring-retry</artifactId>
    </dependency>
</dependencies>
```

---

## 🔒 Important Considerations

### WhatsApp Business API Rules
1. **Template Approval Required:** Business-initiated messages must use pre-approved templates (submit via Meta Business Manager or Twilio Console)
2. **24-hour Window:** After a user replies, you can send free-form messages for 24 hours
3. **Opt-in Required:** Candidates must have opted in to receive WhatsApp messages
4. **Rate Limits:** Meta allows ~80 messages/second; new accounts start with 250 messages/day, scaling up to 100K/day

### Phone Number Format
- Must be in international format: `+<country_code><number>` (e.g., +919876543210)
- Add validation/normalization in Excel parser

### Scalability
- For 1000+ candidates: Use message queue (RabbitMQ/Kafka) instead of simple @Async
- Implement circuit breaker pattern (Resilience4j) for API failures

---

## 🔗 Useful Links & Resources

| Resource | URL |
|----------|-----|
| Meta WhatsApp Business API Docs | https://developers.facebook.com/docs/whatsapp/cloud-api |
| Twilio WhatsApp Quickstart | https://www.twilio.com/docs/whatsapp/quickstart/java |
| Apache POI Documentation | https://poi.apache.org/components/spreadsheet/ |
| Spring Boot Official Docs | https://docs.spring.io/spring-boot/docs/current/reference/html/ |
| SpringDoc OpenAPI | https://springdoc.org/ |
| Meta Business Manager | https://business.facebook.com/ |
| Twilio Console | https://console.twilio.com/ |
| Google Meet API (for auto-generating links) | https://developers.google.com/meet/api |
| Spring Retry Guide | https://docs.spring.io/spring-retry/docs/api/current/ |
| Resilience4j (Circuit Breaker) | https://resilience4j.readme.io/docs |

---

## 🚀 Future Enhancements (Phase 2 – Optional)

1. **Frontend Dashboard** – React/Angular UI for uploading files and monitoring sends
2. **Email Integration** – Send interview details via email as backup
3. **Scheduling** – Schedule messages to be sent at a specific time
4. **Google Meet Auto-Generation** – Auto-create GMeet links via Google Calendar API
5. **Candidate Response Tracking** – Track if candidate confirmed attendance
6. **Multi-language Templates** – Support templates in multiple languages
7. **Analytics Dashboard** – Delivery rates, response rates, failure analysis
8. **Webhook Integration** – Real-time delivery status updates from WhatsApp

---

## ✅ Summary

| Aspect | Detail |
|--------|--------|
| **Tech Stack** | Spring Boot 3.x + Apache POI + PostgreSQL + Twilio/Meta WhatsApp API |
| **Timeline** | 10–12 working days |
| **Cost (Development)** | Free (open-source tools) |
| **Cost (WhatsApp)** | Free tier: 1000 msgs/month; Paid: ~$0.005–$0.08/msg |
| **Scalability** | Supports 100–1000+ candidates per batch |
| **Deployment** | AWS/GCP/Azure or any VPS with Java 17+ |

---

*Document Created: June 15, 2026*
*Version: 1.0*



