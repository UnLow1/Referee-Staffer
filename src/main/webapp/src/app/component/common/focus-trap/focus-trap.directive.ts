import {AfterViewInit, Directive, ElementRef, OnDestroy, inject} from '@angular/core';

/** Interactive elements a focus trap cycles through. Disabled controls are skipped. */
const FOCUSABLE_SELECTOR = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])'
].join(', ');

/**
 * Overlays can stack (form drawer + its discard guard), so every active trap registers
 * here and only the topmost one handles Tab. Module-level on purpose: both overlay
 * primitives share one stack without needing an injectable service.
 */
const trapStack: FocusTrapDirective[] = [];

/**
 * Traps Tab / Shift+Tab inside the host element and returns focus to the previously
 * focused element (the trigger) when the host is destroyed. Designed for the two overlay
 * primitives (app-drawer, app-confirm-dialog), whose panels live inside `@if (open)`
 * blocks — creation activates the trap, removal deactivates it, so the directive needs
 * no `open` input of its own.
 *
 * On activation focus moves to the first focusable element of the panel. The focusable
 * backdrop button is a sibling of the panel, so it drops out of the Tab cycle while a
 * trap is active — Esc and the panel's own buttons cover keyboard dismissal.
 */
@Directive({
  selector: '[appFocusTrap]'
})
export class FocusTrapDirective implements AfterViewInit, OnDestroy {
  private readonly container: HTMLElement = inject(ElementRef).nativeElement;
  private previouslyFocused: HTMLElement | null = null;
  private readonly onKeydown = (event: KeyboardEvent): void => this.trapTab(event);

  ngAfterViewInit(): void {
    this.previouslyFocused = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    trapStack.push(this);
    document.addEventListener('keydown', this.onKeydown);

    const target = this.focusables()[0] ?? this.container;
    if (target === this.container) this.container.tabIndex = -1;
    target.focus();
  }

  ngOnDestroy(): void {
    const index = trapStack.indexOf(this);
    if (index >= 0) trapStack.splice(index, 1);
    document.removeEventListener('keydown', this.onKeydown);
    // When a stacked guard closes together with its drawer, the guard's saved element is
    // already detached — skipping it lets the drawer's trap restore focus to the trigger.
    if (this.previouslyFocused?.isConnected) this.previouslyFocused.focus();
  }

  private trapTab(event: KeyboardEvent): void {
    if (event.key !== 'Tab' || trapStack[trapStack.length - 1] !== this) return;

    const focusables = this.focusables();
    if (focusables.length === 0) {
      event.preventDefault();
      return;
    }

    const first = focusables[0];
    const last = focusables[focusables.length - 1];
    const active = document.activeElement;
    const activeInside = active instanceof HTMLElement && this.container.contains(active);

    if (event.shiftKey && (!activeInside || active === first)) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && (!activeInside || active === last)) {
      event.preventDefault();
      first.focus();
    }
  }

  private focusables(): HTMLElement[] {
    return Array.from(this.container.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR));
  }
}
