import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';
import { LoadingService } from './loading.service';

/**
 * HTTP interceptor that increments / decrements a global "active request"
 * counter. Skips the polling status calls (very frequent) so the progress
 * bar doesn't blink every 2.5 seconds during a bulk send.
 */
export const loadingInterceptor: HttpInterceptorFn = (req, next) => {
  const loading = inject(LoadingService);

  // Skip frequent background polls
  const isPoll = req.url.includes('/api/messages/status/')
              || req.url.includes('/api/whatsapp/status');

  if (!isPoll) loading.start();
  return next(req).pipe(
    finalize(() => { if (!isPoll) loading.stop(); })
  );
};

