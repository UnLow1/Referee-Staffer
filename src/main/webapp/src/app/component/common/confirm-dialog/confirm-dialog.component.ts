import {Component, EventEmitter, HostListener, Input, Output} from '@angular/core';
import {IconComponent} from '../icon/icon.component';
import {FocusTrapDirective} from '../focus-trap/focus-trap.directive';
import {ModalData} from '../../../model/modalData';

/**
 * Centered confirm / info modal — the only non-drawer overlay in the app. Backs both
 * the destructive-delete flow and the form drawer's discard-changes guard.
 *
 * The body slot is projected so callers can emphasise the entity name
 * (<strong>…</strong>); when nothing is projected the plain `data.message` renders.
 * Esc, the backdrop, and the cancel button all emit `cancelled` — the caller owns
 * the open state, mirroring the app-drawer contract.
 */
@Component({
  selector: 'app-confirm-dialog',
  imports: [IconComponent, FocusTrapDirective],
  template: `
    @if (open) {
      <button type="button" class="modal-backdrop" (click)="cancelled.emit()" aria-label="Cancel"></button>
      <div class="modal" role="alertdialog" aria-modal="true" [attr.aria-label]="data.header" appFocusTrap>
        <div class="modal__head">
          <span class="modal__icon"
                [class.modal__icon--warn]="data.tone === 'warn'"
                [class.modal__icon--accent]="data.tone === 'accent'">
            <app-icon [name]="data.icon ?? 'trash'" [size]="17"></app-icon>
          </span>
          <div class="modal__title">{{ data.header }}</div>
        </div>
        <div class="modal__body">
          <ng-content>{{ data.message }}</ng-content>
        </div>
        <div class="modal__foot">
          <button type="button" class="btn" (click)="cancelled.emit()">{{ data.cancelLabel ?? 'Cancel' }}</button>
          <button type="button" class="btn btn--primary"
                  [class.btn--primary-danger]="(data.tone ?? 'danger') === 'danger'"
                  (click)="confirmed.emit()">{{ data.confirmLabel ?? 'Confirm' }}</button>
        </div>
      </div>
    }
  `,
  styles: [`
    /* Same backdrop-as-<button> pattern as app-drawer — activatable by assistive tech
       but outside the focus trap's Tab cycle (Esc and the footer buttons cover keyboard
       dismissal). The global .modal-backdrop flex centering is irrelevant for an empty
       button, so the modal centers itself instead. */
    button.modal-backdrop {
      border: none;
      padding: 0;
      cursor: pointer;
    }
    .modal {
      position: fixed;
      inset: 0;
      margin: auto;
      height: fit-content;
      z-index: 61;
    }
    /* Danger-filled primary for destructive confirms (prototype inline-styles this). */
    .btn--primary-danger {
      background: var(--danger);
      border-color: var(--danger);
    }
  `]
})
export class ConfirmDialogComponent {
  @Input() open = false;
  @Input({required: true}) data!: ModalData;
  @Output() confirmed = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.open) this.cancelled.emit();
  }
}
