import {Component, EventEmitter, HostListener, Input, Output} from '@angular/core';
import {IconComponent} from '../icon/icon.component';
import {FocusTrapDirective} from '../focus-trap/focus-trap.directive';

/**
 * Right-side detail drawer with backdrop. Slide animation comes from styles.scss
 * (.drawer + @keyframes slide). Closes on backdrop click, on the X button, or on Esc.
 * While open, appFocusTrap keeps Tab inside the panel and returns focus to the trigger
 * on close.
 *
 * Slot content goes into .drawer__body. An optional footer can be slotted with
 * `<div drawerFoot>...</div>` and lands inside .panel__foot for consistent styling.
 */
@Component({
  selector: 'app-drawer',
  imports: [IconComponent, FocusTrapDirective],
  template: `
    @if (open) {
      <button type="button" class="drawer-backdrop" (click)="closed.emit()" aria-label="Close drawer"></button>
      <aside class="drawer" [class.drawer--wide]="wide" role="dialog" aria-modal="true" [attr.aria-label]="title" appFocusTrap>
        <header class="drawer__head">
          <div>
            <div class="drawer__title">{{ title }}</div>
            @if (subtitle) {
              <div class="fdrawer__sub">{{ subtitle }}</div>
            }
          </div>
          <button type="button" class="btn btn--ghost btn--sm" (click)="closed.emit()" aria-label="Close">
            <app-icon name="x" [size]="14"></app-icon>
          </button>
        </header>
        <div class="drawer__body">
          <ng-content></ng-content>
        </div>
        <ng-content select="[drawerFoot]"></ng-content>
      </aside>
    }
  `,
  styles: [`
    .drawer__title {
      font-weight: 600;
    }
    /* The backdrop is a <button> so assistive tech can activate it as "Close drawer";
       the focus trap keeps Tab inside the panel, so it is not part of the Tab cycle —
       Esc and the X button cover keyboard dismissal. Strip the default button chrome
       (border, background) so it stays the dim overlay it visually has to be. */
    button.drawer-backdrop {
      border: none;
      padding: 0;
      cursor: pointer;
    }
  `]
})
export class DrawerComponent {
  @Input() open = false;
  @Input() title = '';
  /** Muted line under the title (e.g. the entity being edited). Added for app-form-drawer. */
  @Input() subtitle = '';
  /** 560px variant (.drawer--wide) for denser forms — the Match form uses it. */
  @Input() wide = false;
  // `close` would shadow the native HTMLDialogElement.close event — use `closed` per the
  // @angular-eslint/no-output-native rule. Convention also matches the past-tense "the
  // user closed it" semantics of an emitted event.
  @Output() closed = new EventEmitter<void>();

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.open) this.closed.emit();
  }
}
