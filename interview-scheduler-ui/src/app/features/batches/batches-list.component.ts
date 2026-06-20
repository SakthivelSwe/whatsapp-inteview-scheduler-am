import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { Batch } from '../../core/models';
import { StatusChipComponent } from '../../shared/components/status-chip.component';
import { ToastService } from '../../core/toast.service';
import { ConfirmService } from '../../shared/components/confirm.dialog';

@Component({
  selector: 'app-batches-list',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe, StatusChipComponent],
  templateUrl: './batches-list.component.html',
  styleUrls: ['./batches-list.component.scss']
})
export class BatchesListComponent implements OnInit {
  private api = inject(ApiService);
  private router = inject(Router);
  private toast = inject(ToastService);
  private confirm = inject(ConfirmService);

  batches: Batch[] = [];
  loading = true;

  ngOnInit(): void {
    this.load();
  }

  load() {
    this.loading = true;
    // Prefer server-side list; fall back to localStorage on failure.
    this.api.listBatches().pipe(
      catchError(() => of(this.api.listKnownBatches()))
    ).subscribe((bs) => {
      this.batches = bs;
      this.loading = false;
    });
  }

  open(b: Batch) {
    this.router.navigate(['/batches', b.id]);
  }

  remove(b: Batch, e: Event) {
    e.stopPropagation();
    this.confirm.ask({
      eyebrow: 'Batch cleanup',
      title: `Delete "${b.fileName}"?`,
      message: 'This permanently removes the batch and all its candidates from the database. Sent message logs are kept for audit.',
      confirmText: 'Delete batch',
      variant: 'danger',
      icon: 'delete'
    }).subscribe((ok) => {
      if (!ok) return;
      this.api.deleteBatch(b.id).pipe(
        catchError((err) => {
          this.toast.error(err?.error?.error ?? 'Delete failed');
          return of(null);
        })
      ).subscribe((res) => {
        if (res === null) return;
        this.api.forgetBatch(b.id);
        this.toast.success('Batch deleted');
        this.load();
      });
    });
  }

  chipVariant(s: Batch['status']) {
    switch (s) {
      case 'COMPLETED': return 'success';
      case 'SENDING':   return 'info';
      case 'PAUSED':    return 'warning';
      case 'FAILED':    return 'danger';
      default:          return 'muted';
    }
  }
}
