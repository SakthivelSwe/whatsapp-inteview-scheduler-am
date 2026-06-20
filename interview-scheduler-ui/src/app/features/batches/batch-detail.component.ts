import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription, interval, switchMap } from 'rxjs';
import { ApiService } from '../../core/api.service';
import {
  BatchStatusResponse,
  Candidate,
  MessageLog,
  MessageTemplate
} from '../../core/models';
import { ToastService } from '../../core/toast.service';
import { StatusChipComponent } from '../../shared/components/status-chip.component';
import { ConfirmService } from '../../shared/components/confirm.dialog';

type Tab = 'overview' | 'candidates' | 'failed' | 'logs';

@Component({
  selector: 'app-batch-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, DatePipe, StatusChipComponent],
  templateUrl: './batch-detail.component.html',
  styleUrls: ['./batch-detail.component.scss']
})
export class BatchDetailComponent implements OnInit, OnDestroy {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);
  private toast = inject(ToastService);
  private confirm = inject(ConfirmService);

  batchId = '';
  status: BatchStatusResponse | null = null;
  candidates: Candidate[] = [];
  logs: MessageLog[] = [];
  templates: MessageTemplate[] = [];
  selectedTemplate = '';
  previewBody = '';

  loading = true;
  sending = false;
  retrying = false;
  pausing = false;
  resuming = false;
  activeTab: Tab = 'overview';

  private poll?: Subscription;

  ngOnInit(): void {
    this.batchId = this.route.snapshot.paramMap.get('id') ?? '';
    this.refreshAll();
    this.api.listTemplates().subscribe({
      next: (ts) => {
        this.templates = ts;
        const def = ts.find((t) => t.active) ?? ts[0];
        if (def) {
          this.selectedTemplate = def.name;
          this.previewBody = def.body;
        }
      }
    });
  }

  ngOnDestroy(): void {
    this.poll?.unsubscribe();
  }

  refreshAll() {
    this.loading = true;
    this.api.getStatus(this.batchId).subscribe({
      next: (s) => {
        this.status = s;
        this.loading = false;
        if (s.status === 'SENDING') this.startPolling();
      },
      error: () => {
        this.loading = false;
        this.toast.error('Failed to load batch status');
      }
    });
    this.api.listCandidates(this.batchId).subscribe({
      next: (c) => (this.candidates = c)
    });
    this.api.getLogs(this.batchId).subscribe({
      next: (l) => (this.logs = l)
    });
  }

  startPolling() {
    this.poll?.unsubscribe();
    this.poll = interval(2500)
      .pipe(switchMap(() => this.api.getStatus(this.batchId)))
      .subscribe({
        next: (s) => {
          this.status = s;
          if (s.status !== 'SENDING') {
            this.poll?.unsubscribe();
            this.api.listCandidates(this.batchId).subscribe((c) => (this.candidates = c));
            this.api.getLogs(this.batchId).subscribe((l) => (this.logs = l));
            if (s.status === 'PAUSED') {
              this.toast.info('Batch paused.');
            } else {
              this.toast.success(`Send complete — ${s.successfullySent} sent, ${s.failed} failed`);
            }
          }
        }
      });
  }

  onTemplateChange() {
    const t = this.templates.find((x) => x.name === this.selectedTemplate);
    this.previewBody = t?.body ?? '';
  }

  sendAll() {
    this.confirm.ask({
      eyebrow: 'Bulk dispatch',
      title: 'Send WhatsApp invitations?',
      message: 'Every PENDING candidate in this batch will receive a personalized message. Sending runs in the background — you can pause or stop anytime.',
      confirmText: 'Send all',
      icon: 'send'
    }).subscribe((ok) => {
      if (!ok) return;
      this.sending = true;
      this.api.sendAll(this.batchId, this.selectedTemplate).subscribe({
        next: (s) => {
          this.sending = false;
          this.status = s;
          this.toast.success('Dispatch started');
          this.startPolling();
        },
        error: (err) => {
          this.sending = false;
          this.toast.error(err?.error?.message ?? 'Failed to start dispatch');
        }
      });
    });
  }

  retryFailed() {
    this.confirm.ask({
      eyebrow: 'Recovery',
      title: 'Retry failed candidates?',
      message: 'Only the candidates whose previous send failed will be re-attempted. Successful ones are skipped.',
      confirmText: 'Retry failed',
      icon: 'refresh'
    }).subscribe((ok) => {
      if (!ok) return;
      this.retrying = true;
      this.api.retryFailed(this.batchId, this.selectedTemplate).subscribe({
        next: (s) => {
          this.retrying = false;
          this.status = s;
          this.toast.success('Retry started');
          this.startPolling();
        },
        error: (err) => {
          this.retrying = false;
          this.toast.error(err?.error?.message ?? 'No failed records to retry');
        }
      });
    });
  }

  pause() {
    this.confirm.ask({
      eyebrow: 'Hold sending',
      title: 'Pause this batch?',
      message: 'The current message finishes, then sending stops. Remaining candidates stay PENDING — resume whenever you are ready.',
      confirmText: 'Pause batch',
      variant: 'warning',
      icon: 'pause_circle'
    }).subscribe((ok) => {
      if (!ok) return;
      this.pausing = true;
      this.api.pauseBatch(this.batchId).subscribe({
        next: (s) => {
          this.pausing = false;
          this.status = s;
          this.toast.info('Pause requested. Will stop after current message.');
        },
        error: (err) => {
          this.pausing = false;
          this.toast.error(err?.error?.message ?? 'Failed to pause');
        }
      });
    });
  }

  resume() {
    this.confirm.ask({
      eyebrow: 'Continue',
      title: 'Resume sending?',
      message: 'Picks up exactly where it left off and continues with the remaining candidates.',
      confirmText: 'Resume batch',
      variant: 'success',
      icon: 'play_circle'
    }).subscribe((ok) => {
      if (!ok) return;
      this.resuming = true;
      this.api.resumeBatch(this.batchId, this.selectedTemplate).subscribe({
        next: (s) => {
          this.resuming = false;
          this.status = s;
          this.toast.success('Resumed.');
          this.startPolling();
        },
        error: (err) => {
          this.resuming = false;
          this.toast.error(err?.error?.message ?? 'Failed to resume');
        }
      });
    });
  }

  /* ---------- helpers ---------- */
  setTab(t: Tab) { this.activeTab = t; }

  progress(): number {
    if (!this.status || !this.status.totalRecords) return 0;
    const done = this.status.successfullySent + this.status.failed + this.status.invalid;
    return Math.round((done / this.status.totalRecords) * 100);
  }

  statusVariant(s?: string) {
    if (s === 'SENT' || s === 'DELIVERED' || s === 'COMPLETED') return 'success';
    if (s === 'FAILED') return 'danger';
    if (s === 'INVALID' || s === 'PAUSED') return 'warning';
    if (s === 'SENDING') return 'info';
    return 'muted';
  }

  statusIcon(s?: string) {
    if (s === 'SENT' || s === 'DELIVERED' || s === 'COMPLETED') return 'check_circle';
    if (s === 'FAILED') return 'error';
    if (s === 'INVALID') return 'warning';
    if (s === 'SENDING') return 'sync';
    return 'schedule';
  }

  copyId() {
    navigator.clipboard.writeText(this.batchId);
    this.toast.info('Batch ID copied');
  }

  /* trackBy helpers — prevent ng list re-renders when polling refreshes data */
  trackById = (_: number, x: { id?: string; candidateId?: string }) => x.id ?? x.candidateId ?? _;
  trackByRow = (_: number, x: { rowNumber?: number }) => x.rowNumber ?? _;
}

