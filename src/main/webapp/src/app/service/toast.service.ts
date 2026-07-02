import {Injectable, signal} from '@angular/core';

export interface Toast {
  message: string;
  kind: 'info' | 'error';
}

/**
 * One toast at a time, auto-dismissed. Rendered by ToastComponent (mounted once in
 * AppComponent); shown for HTTP errors by the httpErrorInterceptor, but any code can
 * call show() — e.g. for success confirmations.
 */
@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private static readonly HIDE_AFTER_MS = 4500;

  private readonly toastSignal = signal<Toast | null>(null);
  readonly toast = this.toastSignal.asReadonly();

  private hideTimer: ReturnType<typeof setTimeout> | null = null;

  show(message: string, kind: Toast['kind'] = 'info'): void {
    this.toastSignal.set({message, kind});
    if (this.hideTimer) {
      clearTimeout(this.hideTimer);
    }
    this.hideTimer = setTimeout(() => this.toastSignal.set(null), ToastService.HIDE_AFTER_MS);
  }
}
