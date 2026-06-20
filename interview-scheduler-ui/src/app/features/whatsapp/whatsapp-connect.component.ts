import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { Subscription, interval, startWith, switchMap } from 'rxjs';

import { ApiService, WhatsAppStatus } from '../../core/api.service';
import { ToastService } from '../../core/toast.service';
import { ConfirmService } from '../../shared/components/confirm.dialog';

@Component({
  selector: 'app-whatsapp-connect',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './whatsapp-connect.component.html',
  styleUrls: ['./whatsapp-connect.component.scss']
})
export class WhatsAppConnectComponent implements OnInit, OnDestroy {
  private api = inject(ApiService);
  private toast = inject(ToastService);
  private confirm = inject(ConfirmService);
  private sub?: Subscription;

  status?: WhatsAppStatus;
  loading = true;
  disconnecting = false;
  resetting = false;
  qrCacheBust = Date.now();
  private lastQrSignature = '';

  ngOnInit(): void {
    this.sub = interval(3500)
      .pipe(
        startWith(0),
        switchMap(() => this.api.getWhatsAppStatus())
      )
      .subscribe({
        next: (s) => {
          // Only bump the cache-buster when the QR actually changes —
          // otherwise the <img> tag reloads from the server every 3.5 sec
          // and the QR flickers/disappears constantly.
          const sig = `${s.hasQr ? '1' : '0'}|${s.qrImageUrl ?? ''}|${s.ready ? 'R' : 'N'}`;
          if (sig !== this.lastQrSignature) {
            this.qrCacheBust = Date.now();
            this.lastQrSignature = sig;
          }
          this.status = s;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        }
      });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  get qrImage(): string | null {
    // Only show the <img> when the bridge actually HAS a QR — otherwise the
    // /qr.png endpoint returns 202/empty and the image renders as broken.
    if (!this.status?.hasQr || !this.status?.qrImageUrl) return null;
    return `${this.status.qrImageUrl}?ts=${this.qrCacheBust}`;
  }

  /** True when the bridge is reachable but the QR hasn't appeared yet. */
  get qrPending(): boolean {
    return !!this.status && !this.status.ready && !this.status.hasQr && !this.status.error;
  }

  bigDotClass(): string {
    if (!this.status) return 'big-dot muted';
    if (this.status.ready) return 'big-dot ok';
    if (this.status.hasQr) return 'big-dot warn';
    if (this.status.error) return 'big-dot err';
    return 'big-dot muted';
  }

  bigStatusLabel(): string {
    if (!this.status) return 'Checking…';
    if (this.status.ready) return 'Connected';
    if (this.status.hasQr) return 'Awaiting scan';
    if (this.status.error) return 'Unreachable';
    return 'Initialising';
  }

  resetQr(): void {
    this.confirm.ask({
      eyebrow: 'Bridge maintenance',
      title: 'Refresh the QR code?',
      message: 'Wipes the current WhatsApp session on the bridge and generates a brand-new QR. Use this if the code is stuck or not showing.',
      confirmText: 'Reset QR',
      variant: 'warning',
      icon: 'refresh'
    }).subscribe((ok) => {
      if (!ok) return;
      this.resetting = true;
      this.api.resetWhatsApp().subscribe({
        next: (r) => {
          this.resetting = false;
          if (r.success) {
            this.toast.success('Bridge reset — a fresh QR will appear in a few seconds.');
            if (this.status) {
              this.status = { ...this.status, ready: false, hasQr: false, info: undefined };
            }
          } else {
            this.toast.error(r.error || 'Failed to reset the bridge.');
          }
        },
        error: (err) => {
          this.resetting = false;
          this.toast.error(err?.error?.message || 'Failed to reset the bridge.');
        }
      });
    });
  }

  disconnect(): void {
    this.confirm.ask({
      eyebrow: 'Unlink device',
      title: 'Disconnect WhatsApp?',
      message: 'The linked device will be logged out. You will need to scan a fresh QR to send again.',
      confirmText: 'Disconnect',
      variant: 'danger',
      icon: 'link_off'
    }).subscribe((ok) => {
      if (!ok) return;
      this.disconnecting = true;
      this.api.disconnectWhatsApp().subscribe({
        next: (r) => {
          this.disconnecting = false;
          if (r.success) {
            this.toast.success('Disconnected. A new QR will appear shortly.');
            if (this.status) {
              this.status = { ...this.status, ready: false, hasQr: false, info: undefined };
            }
          } else {
            this.toast.error(r.error || 'Failed to disconnect.');
          }
        },
        error: (err) => {
          this.disconnecting = false;
          this.toast.error(err?.error?.message || 'Failed to disconnect.');
        }
      });
    });
  }
}

