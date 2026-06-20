# Interview Scheduler – Frontend (Angular)

Enterprise-grade Angular 18 UI for the WhatsApp Interview Scheduler backend.

## Stack
- Angular 18 (standalone components, lazy routes)
- Angular Material (Material 3 / Material 2 themes API) with a custom enterprise palette
- SCSS with a design-token system (CSS variables)
- Plus Jakarta Sans typography + Material Symbols Rounded icons
- RxJS for live polling of batch status

## Prerequisites
- Node 18+ (Node 25 works, but Angular CLI prints a non-LTS warning – harmless)
- Backend running on `http://localhost:8080` (see the sibling `interview-scheduler` project)

## Run

```powershell
cd interview-scheduler-ui
npm install
npm start
```

Then open **http://localhost:4200**.

## Build

```powershell
npm run build
```

Outputs to `dist/interview-scheduler-ui/`.

## App Map

| Route | Purpose |
|-------|---------|
| `/dashboard` | KPIs + recent batches |
| `/upload` | Drag-and-drop Excel uploader, schema reference, validation report |
| `/batches` | List of all known (locally tracked) batches |
| `/batches/:id` | Hero card, KPIs, **Send All / Retry Failed**, live progress polling, Template / Candidates / Failed / Logs tabs |
| `/templates` | CRUD for message templates with WhatsApp-style live preview |

## Backend Connection

`src/environments/environment.development.ts` points at `http://localhost:8080/api`. The backend's `WebConfig` already allows CORS from `localhost:4200`.

Production builds use `/api` (relative) – serve the UI behind the same host or via a reverse proxy.

## Design System

Design tokens live in `src/styles.scss` (`:root` CSS vars). Reusable patterns:

- `.surface-card` — elevated white card with subtle border
- `.kpi-grid` / `.kpi` — KPI metric tiles with semantic icon variants
- `.chip` (+ `chip-success|danger|warning|info|muted`) — semantic chips
- `.empty-state` — consistent empty states
- `app-status-chip` standalone component for status pills

Update the palette by editing the `:root { --c-* }` block.

