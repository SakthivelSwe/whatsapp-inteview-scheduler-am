import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

type Variant = 'success' | 'danger' | 'warning' | 'info' | 'muted' | 'primary' | 'accent';

@Component({
  selector: 'app-status-chip',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="pill" [ngClass]="pillClass">
      <span class="material-symbols-rounded" *ngIf="icon" style="font-size:13px;">{{ icon }}</span>
      <ng-content />
    </span>
  `
})
export class StatusChipComponent {
  @Input() variant: Variant = 'muted';
  @Input() icon?: string;

  get pillClass(): string {
    switch (this.variant) {
      case 'success': return 'pill-success';
      case 'danger':  return 'pill-danger';
      case 'warning': return 'pill-warning';
      case 'info':    return 'pill-info';
      case 'accent':
      case 'primary': return 'pill-accent';
      default:        return '';
    }
  }
}
