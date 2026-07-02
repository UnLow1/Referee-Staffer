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
      toastService.show(messageFor(error), 'error');
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
  return `Request failed (HTTP ${error.status})`;
}
