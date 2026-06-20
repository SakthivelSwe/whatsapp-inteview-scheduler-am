import { Injectable, signal } from '@angular/core';

/**
 * Tracks the count of in-flight HTTP requests so a global top-of-page
 * progress bar can show whenever any API call is happening. Using
 * Angular signals so the bar reacts instantly without manual change
 * detection.
 */
@Injectable({ providedIn: 'root' })
export class LoadingService {
  private count = 0;
  /** True while at least one HTTP request is pending. */
  readonly active = signal(false);

  start(): void {
    this.count++;
    if (!this.active()) this.active.set(true);
  }

  stop(): void {
    this.count = Math.max(0, this.count - 1);
    if (this.count === 0 && this.active()) this.active.set(false);
  }
}

