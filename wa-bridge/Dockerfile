FROM node:20-slim

# Install necessary libraries for Puppeteer/Chromium to run in Docker
RUN apt-get update && apt-get install -y \
    wget \
    gnupg \
    libnss3 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libcups2 \
    libdrm2 \
    libxkbcommon0 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxrandr2 \
    libgbm1 \
    libasound2 \
    libcairo2 \
    libpango-1.0-0 \
    libpangocairo-1.0-0 \
    libgtk-3-0 \
    libx11-xcb1 \
    libxss1 \
    libxtst6 \
    fonts-liberation \
    --no-install-recommends \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY package*.json ./
RUN npm install --omit=dev

COPY . .

EXPOSE 3000

# ── Memory-safe defaults for Render free-tier (512 MB) ──────────────
# Cap Node.js V8 heap at 300 MB — leaves ~200 MB for Chromium itself.
ENV NODE_OPTIONS="--max-old-space-size=300"

# Tell Puppeteer NOT to download a second Chromium; use the one npm installed.
ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=false
ENV PUPPETEER_EXECUTABLE_PATH=""

CMD ["npm", "start"]
