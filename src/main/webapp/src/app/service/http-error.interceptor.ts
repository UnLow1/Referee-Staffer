import {HttpErrorResponse, HttpInterceptorFn} from '@angular/common/http';
import {inject} from '@angular/core';
import {catchError, throwError} from 'rxjs';
import {ToastService} from './toast.service';

/**
 * Global HTTP error handler: every failed request surfaces as an error toast, so a
 * failed save/load is never silent. The error is rethrown afterwards — components
 * still run their own `error:` callbacks (loading-flag resets etc.).
 */
export const httpErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const toastService = inject(ToastService);
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Requests made with responseType 'blob' (PDF download) get their ProblemDetail
      // wrapped in a Blob — read it asynchronously; the toast can arrive a tick late.
      if (error.error instanceof Blob && error.error.type.includes('json')) {
        error.error.text().then(
          text => toastService.show(messageFromProblemJson(error, text), 'error'),
          () => toastService.show(fallbackMessage(error), 'error'));
      } else {
        toastService.show(messageFor(error), 'error');
      }
      return throwError(() => error);
    })
  );
};

function messageFor(error: HttpErrorResponse): string {
  if (error.status === 0) {
    return 'Cannot reach the server';
  }
  // RestExceptionHandler responds with RFC 7807 ProblemDetail — `detail` carries the
  // human-readable domain message (not found / staffing conflict / import failure).
  const detail = error.error?.detail;
  if (typeof detail === 'string' && detail.length > 0) {
    return detail;
  }
  return fallbackMessage(error);
}

function messageFromProblemJson(error: HttpErrorResponse, text: string): string {
  try {
    const detail = JSON.parse(text)?.detail;
    if (typeof detail === 'string' && detail.length > 0) {
      return detail;
    }
  } catch {
    // fall through — not valid JSON after all
  }
  return fallbackMessage(error);
}

function fallbackMessage(error: HttpErrorResponse): string {
  return `Request failed (HTTP ${error.status})`;
}
