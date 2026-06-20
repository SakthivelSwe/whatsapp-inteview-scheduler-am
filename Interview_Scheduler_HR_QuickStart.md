# 🚀 Interview Scheduler — Quick Start Guide for HR

> **Send personalized WhatsApp interview invitations to 100 candidates with one click.**
> No coding required. Just upload an Excel file and click Send.

---

## ✅ What You Need (One-Time Setup)

| Item | Why |
|---|---|
| 📱 A **dedicated WhatsApp number** (a spare SIM, NOT your personal one) | Will be the sender on all invitations |
| 💻 The Interview Scheduler running on your laptop | (Already set up by IT) |
| 📊 An **Excel file** with candidate details | Format shown below |

---

## 📋 Excel File Format (6 Required Columns)

| Phone Number | Candidate Name | Job Position | Interview Date | Interview Time | Meeting Link |
|---|---|---|---|---|---|
| 9342627033 | Godson Robin Raja S | Angular Developer | 09th June 2026 | 11:30 AM | https://meet.google.com/xwe-ivrc-pet |
| 9876543210 | Priya Sharma | Java Developer | 10th June 2026 | 2:00 PM | https://meet.google.com/abc-defg-hij |

> 💡 Phone numbers can be 10 digits (system auto-adds +91) or with country code.

---

## 🪜 5 Simple Steps to Send

### **Step 1 — Open the App**

Open your browser and go to: **http://localhost:4200**

---

### **Step 2 — Connect WhatsApp (one-time only)**

1. Click **WhatsApp** in the left sidebar
2. You'll see a **QR code** on the page
3. On your **sender phone** (the dedicated SIM), open WhatsApp
4. Tap **⋮ Menu → Settings → Linked Devices → Link a Device**
5. **Scan the QR code** with the phone camera
6. ✅ Page will show "**Connected**" with your name

> Once linked, you don't need to scan again unless you click **Disconnect**.

---

### **Step 3 — Upload Your Excel File**

1. Click **Upload** in the left sidebar
2. **Drag your Excel file** into the upload box (or click to browse)
3. The system will show:
   - ✅ Number of valid rows
   - ⚠️ Any invalid rows (with reasons like "missing phone")
4. Click **Open Batch**

---

### **Step 4 — Send the Messages**

1. On the batch page, click the big **Send All** button
2. Confirm when prompted
3. The progress bar starts moving
4. Each message is sent **about every 90 seconds** (random 54–126 sec)
5. You can do other work — the system keeps sending in the background

> ⏱️ **Expected time:** ~2.5 hours for 100 candidates (this slow pace is intentional — protects your number from being blocked).

---

### **Step 5 — Check Results**

Watch the live dashboard:

| What you'll see | Meaning |
|---|---|
| **Successfully Sent** counter | Messages delivered ✅ |
| **Failed** counter | Couldn't deliver (bad number, etc.) |
| **Pending** counter | Still in queue |
| **Logs** tab | Exact text sent to each candidate |
| **Failed** tab | List of failures + reasons |

If any failed → click **Retry Failed** to re-send only those.

---

## ⏯️ Pause & Resume (NEW)

Need to stop sending mid-way?

| Button | What it does |
|---|---|
| ⏸️ **Pause** | Stops after the current message. Remaining candidates stay queued. |
| ▶️ **Resume** | Picks up exactly where you left off. |

Use this if:
- You spot a mistake in the template
- You need to free up the phone
- You want to pause overnight and resume tomorrow

---

## 🔌 Disconnect WhatsApp (NEW)

To unlink the current sender phone (e.g., to switch to a different number):

1. Go to **WhatsApp** page
2. Click **🔗 Disconnect this device**
3. Confirm
4. A new QR code appears
5. Scan with the new sender phone

> The previous WhatsApp account will see *"This device was logged out"* in its Linked Devices list.

---

## 📝 The Message Template

Each candidate receives this (with their own name, role, time, link):

> Hello **[Candidate Name]**,
>
> Greetings from TVM Infotech Pvt. Ltd.!
>
> We are pleased to inform you that your interview for the **[Role]**
> position has been scheduled.
>
> Interview Details:
> 📅 Date: **[Date]**
> ⏰ Time: **[Time]**
> 💻 Mode: Online (Google Meet)
> 🔗 Meeting Link: **[GMeet URL]**
>
> *(...full template continues...)*
>
> ✅ Action Required:
> Please reply "CONFIRMED" to accept.

You can edit the template via the **Templates** page in the sidebar.

---

## ⚠️ Safety Rules — IMPORTANT

| Rule | Why |
|---|---|
| ✅ Use a **dedicated SIM** as the sender | Personal number ban = lose all personal chats |
| ✅ Send only to candidates **who actually applied** | Random outreach gets reported as spam |
| ✅ Tell candidates to **save your number** | Saved contacts almost never report you |
| ❌ Don't send more than **150 messages per day** | System auto-stops at this limit |
| ❌ Don't reduce the 90-second delay | Faster = higher risk of WhatsApp blocking |
| ❌ Don't run on two computers at once | Confuses WhatsApp |

---

## 🆘 Need Help?

| Problem | Try this |
|---|---|
| Page shows "Not connected" | Restart the app, scan QR again |
| Some messages failed | Click **Retry Failed** on the batch page |
| WhatsApp number got banned | Use a fresh SIM, scan new QR |
| Anything else | Contact IT |

---

## 📞 At a Glance

```
   ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐
   │ Connect WhatsApp │ -> │ Upload Excel     │ -> │ Click "Send All" │
   │   (scan QR)      │    │ (drag & drop)    │    │  Wait ~2.5 hours │
   └──────────────────┘    └──────────────────┘    └──────────────────┘
                                                            ↓
                                                  100 candidates receive
                                                  personalized WhatsApp ✅
```

---

> **TVM Infotech Pvt. Ltd. — HR Department**
> Quick Start v1.0 · June 2026

