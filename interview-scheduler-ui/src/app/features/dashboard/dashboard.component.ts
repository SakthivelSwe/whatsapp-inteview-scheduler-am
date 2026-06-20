import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { Batch } from '../../core/models';
import { StatusChipComponent } from '../../shared/components/status-chip.component';
import { catchError, of } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe, StatusChipComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  private api = inject(ApiService);
  private router = inject(Router);

  batches: Batch[] = [];
  stats = { total: 0, sending: 0, completed: 0, uploaded: 0 };
  loading = true;
  readonly today = new Date().toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' });

  ngOnInit(): void {
    // Prefer the backend list; fall back to localStorage if API is offline.
    this.api.listBatches().pipe(
      catchError(() => of(this.api.listKnownBatches()))
    ).subscribe((bs) => {
      this.batches = bs;
      this.stats.total     = bs.length;
      this.stats.uploaded  = bs.filter((b) => b.status === 'UPLOADED').length;
      this.stats.sending   = bs.filter((b) => b.status === 'SENDING' || b.status === 'PAUSED').length;
      this.stats.completed = bs.filter((b) => b.status === 'COMPLETED').length;
      this.loading = false;
    });
  }

  openBatch(b: Batch) {
    this.router.navigate(['/batches', b.id]);
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
