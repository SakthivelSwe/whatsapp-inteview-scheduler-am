import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Subscription, interval, startWith, switchMap, catchError, of } from 'rxjs';

import { ApiService, WhatsAppStatus } from '../../core/api.service';
import { LoadingService } from '../../core/loading.service';

interface NavItem { label: string; path: string; icon: string; }

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet, MatProgressBarModule],
  templateUrl: './main-layout.component.html',
  styleUrls: ['./main-layout.component.scss']
})
export class MainLayoutComponent implements OnInit, OnDestroy {
  readonly loading = inject(LoadingService);
  private api = inject(ApiService);
  private sub?: Subscription;

  waStatus?: WhatsAppStatus;

  readonly nav: NavItem[] = [
    { label: 'Overview',   path: '/dashboard',  icon: 'dashboard' },
    { label: 'Upload',     path: '/upload',     icon: 'upload_file' },
    { label: 'Batches',    path: '/batches',    icon: 'inventory_2' },
    { label: 'Templates',  path: '/templates',  icon: 'description' },
    { label: 'Connection', path: '/whatsapp',   icon: 'link' }
  ];

  ngOnInit() {
    this.sub = interval(10000)
      .pipe(
        startWith(0),
        switchMap(() => this.api.getWhatsAppStatus().pipe(catchError(() => of(undefined))))
      )
      .subscribe((s) => (this.waStatus = s));
  }

  ngOnDestroy() { this.sub?.unsubscribe(); }

  get statusVariant(): 'success' | 'warning' | 'danger' | 'muted' {
    if (!this.waStatus) return 'muted';
    if (this.waStatus.ready) return 'success';
    if (this.waStatus.hasQr) return 'warning';
    return 'danger';
  }

  get statusLabel(): string {
    if (!this.waStatus) return 'Connecting…';
    if (this.waStatus.ready) return this.waStatus.info?.pushname || 'WhatsApp linked';
    if (this.waStatus.hasQr) return 'Scan QR to link';
    return 'WhatsApp offline';
  }

  trackByPath = (_: number, item: NavItem) => item.path;
}

