import { CommonModule } from '@angular/common';
import { Component, Inject, Injectable, inject } from '@angular/core';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogRef
} from '@angular/material/dialog';
import { Observable, map } from 'rxjs';

export type ConfirmVariant = 'default' | 'danger' | 'warning' | 'success';

export interface ConfirmOptions {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  variant?: ConfirmVariant;
  icon?: string;
  eyebrow?: string;
}

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="cd" [attr.data-variant]="data.variant || 'default'">
      <div class="cd-head">
        <div class="cd-icon">
          <span class="material-symbols-rounded">{{ data.icon || iconForVariant() }}</span>
        </div>
        <div class="cd-text">
          <div class="cd-eyebrow" *ngIf="data.eyebrow">{{ data.eyebrow }}</div>
          <h2 class="cd-title">{{ data.title }}</h2>
          <p class="cd-msg">{{ data.message }}</p>
        </div>
        <button type="button" class="cd-close" (click)="ref.close(false)" aria-label="Close">
          <span class="material-symbols-rounded">close</span>
        </button>
      </div>
      <div class="cd-foot">
        <button type="button" class="cd-btn cd-btn-ghost" (click)="ref.close(false)">
          {{ data.cancelText || 'Cancel' }}
        </button>
        <button type="button" class="cd-btn cd-btn-solid" (click)="ref.close(true)" autofocus>
          {{ data.confirmText || 'Confirm' }}
        </button>
      </div>
    </div>
  `,
  styles: [`
    :host { display: block; }
    .cd {
      width: 420px;
      max-width: 92vw;
      background: #fff;
      color: #18181b;
      font-family: 'Inter', system-ui, sans-serif;
    }
    .cd-head {
      display: grid;
      grid-template-columns: 40px 1fr auto;
      gap: 14px;
      padding: 20px 20px 14px;
    }
    .cd-icon {
      width: 36px; height: 36px;
      border-radius: 8px;
      display: grid; place-items: center;
      background: rgba(79,70,229,0.10);
      color: #4f46e5;
      flex-shrink: 0;
    }
    .cd[data-variant='danger']  .cd-icon { background: #fee2e2; color: #dc2626; }
    .cd[data-variant='warning'] .cd-icon { background: #fef3c7; color: #d97706; }
    .cd[data-variant='success'] .cd-icon { background: #dcfce7; color: #16a34a; }
    .cd-icon .material-symbols-rounded { font-size: 20px; }

    .cd-text { min-width: 0; padding-top: 2px; }
    .cd-eyebrow {
      font-size: 11px; font-weight: 600;
      text-transform: uppercase; letter-spacing: 0.06em;
      color: #52525b;
      margin-bottom: 4px;
    }
    .cd-title {
      margin: 0;
      font-size: 16px; font-weight: 600;
      letter-spacing: -0.01em;
      color: #18181b;
      line-height: 1.3;
    }
    .cd-msg {
      margin: 6px 0 0;
      color: #52525b;
      font-size: 13.5px;
      line-height: 1.55;
    }

    .cd-close {
      width: 28px; height: 28px;
      display: grid; place-items: center;
      border: 0;
      border-radius: 6px;
      background: transparent;
      color: #a1a1aa; cursor: pointer;
      transition: background .12s ease, color .12s ease;
    }
    .cd-close:hover { background: #f4f4f5; color: #18181b; }
    .cd-close .material-symbols-rounded { font-size: 18px; }

    .cd-foot {
      display: flex;
      justify-content: flex-end;
      gap: 8px;
      padding: 14px 20px 20px;
      border-top: 1px solid #f4f4f5;
      margin-top: 8px;
    }
    .cd-btn {
      display: inline-flex; align-items: center; justify-content: center;
      height: 34px; padding: 0 14px;
      border-radius: 6px;
      font-family: inherit;
      font-size: 13px; font-weight: 500;
      letter-spacing: -0.005em;
      cursor: pointer;
      border: 1px solid transparent;
      transition: background .12s ease, border-color .12s ease;
    }
    .cd-btn-ghost {
      background: #fff;
      color: #18181b;
      border-color: #d4d4d8;
      box-shadow: 0 1px 2px rgba(16,24,40,0.04);
    }
    .cd-btn-ghost:hover { background: #f4f4f5; }

    .cd-btn-solid {
      background: #4f46e5;
      color: #fff;
      border-color: #4f46e5;
      box-shadow: 0 1px 2px rgba(79,70,229,0.18);
    }
    .cd-btn-solid:hover { background: #4338ca; border-color: #4338ca; }
    .cd[data-variant='danger']  .cd-btn-solid { background: #dc2626; border-color: #dc2626; box-shadow: 0 1px 2px rgba(220,38,38,0.18); }
    .cd[data-variant='danger']  .cd-btn-solid:hover { background: #b91c1c; border-color: #b91c1c; }
    .cd[data-variant='warning'] .cd-btn-solid { background: #d97706; border-color: #d97706; box-shadow: 0 1px 2px rgba(217,119,6,0.18); }
    .cd[data-variant='warning'] .cd-btn-solid:hover { background: #b45309; border-color: #b45309; }
    .cd[data-variant='success'] .cd-btn-solid { background: #16a34a; border-color: #16a34a; box-shadow: 0 1px 2px rgba(22,163,74,0.18); }
    .cd[data-variant='success'] .cd-btn-solid:hover { background: #15803d; border-color: #15803d; }

    @media (max-width: 520px) {
      .cd-head { grid-template-columns: 36px 1fr auto; padding: 16px 16px 12px; }
      .cd-foot { padding: 12px 16px 16px; }
      .cd-foot .cd-btn { flex: 1; }
    }
  `]
})
export class ConfirmDialogComponent {
  constructor(
    public ref: MatDialogRef<ConfirmDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public data: ConfirmOptions
  ) {}

  iconForVariant(): string {
    switch (this.data.variant) {
      case 'danger':  return 'delete_forever';
      case 'warning': return 'warning';
      case 'success': return 'check_circle';
      default:        return 'help';
    }
  }
}

@Injectable({ providedIn: 'root' })
export class ConfirmService {
  private dialog = inject(MatDialog);

  ask(opts: ConfirmOptions): Observable<boolean> {
    return this.dialog
      .open<ConfirmDialogComponent, ConfirmOptions, boolean>(ConfirmDialogComponent, {
        data: opts,
        autoFocus: false,
        restoreFocus: true,
        panelClass: 'cd-panel',
        backdropClass: 'cd-backdrop'
      })
      .afterClosed()
      .pipe(map((r) => r === true));
  }
}
