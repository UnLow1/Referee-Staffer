import {HttpErrorResponse, HttpInterceptorFn} from '@angular/common/http';
import {inject} from '@angular/core';
import {catchError, from, mergeMap, throwError} from 'rxjs';
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
      // Requests made with responseType 'blob' (file downloads) deliver the error body
      // as a Blob, so a ProblemDetail sent by the backend would otherwise be lost and
      // the toast would fall back to the generic message. Read JSON-typed Blobs first.
      if (error.error instanceof Blob && error.error.type.includes('json')) {
        return from(error.error.text().catch(() => '')).pipe(
          mergeMap(text => {
            toastService.show(messageFor(error, parseJson(text)), 'error');
            return throwError(() => error);
          })
        );
      }
      toastService.show(messageFor(error, error.error), 'error');
      return throwError(() => error);
    })
  );
};

function parseJson(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

function messageFor(error: HttpErrorResponse, body: unknown): string {
  if (error.status === 0) {
    return 'Cannot reach the server';
  }
  // RestExceptionHandler responds with RFC 7807 ProblemDetail — `detail` carries the
  // human-readable domain message (not found / staffing conflict / import failure).
  const detail = (body as {detail?: unknown} | null | undefined)?.detail;
  if (typeof detail === 'string' && detail.length > 0) {
    return detail;
  }
  return `Request failed (HTTP ${error.status})`;
}
