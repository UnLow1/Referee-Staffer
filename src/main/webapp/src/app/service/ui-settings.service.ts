import {Injectable, signal} from '@angular/core';

/**
 * UI preferences shared between the shell and screens, each persisted to localStorage
 * so a refresh keeps the choice. The `admin.hidden` key predates this service (it
 * mirrors the legacy header's DevTools flag) and is kept for compatibility with
 * existing bookmarks of the trick.
 */
@Injectable({
  providedIn: 'root'
})
export class UiSettingsService {
  private readonly darkSignal = signal(localStorage.getItem('theme') === 'dark');
  /** Admin nav gate — visible only when `admin.hidden` is explicitly 'false'. */
  private readonly adminSignal = signal(localStorage.getItem('admin.hidden') === 'false');
  /** "How the staffer scores assignments" panel on the Staffer screen. Off by default. */
  private readonly explainerSignal = signal(localStorage.getItem('staffer.explainer') === 'true');

  readonly dark = this.darkSignal.asReadonly();
  readonly adminVisible = this.adminSignal.asReadonly();
  readonly explainerVisible = this.explainerSignal.asReadonly();

  constructor() {
    this.applyTheme();
  }

  toggleDark(): void {
    this.darkSignal.update(v => !v);
    localStorage.setItem('theme', this.dark() ? 'dark' : 'light');
    this.applyTheme();
  }

  toggleAdmin(): void {
    this.adminSignal.update(v => !v);
    localStorage.setItem('admin.hidden', this.adminVisible() ? 'false' : 'true');
  }

  toggleExplainer(): void {
    this.explainerSignal.update(v => !v);
    localStorage.setItem('staffer.explainer', String(this.explainerVisible()));
  }

  /** Dark theme is activated by `data-theme="dark"` on <html> — see _tokens.scss. */
  private applyTheme(): void {
    document.documentElement.setAttribute('data-theme', this.dark() ? 'dark' : 'light');
  }
}
