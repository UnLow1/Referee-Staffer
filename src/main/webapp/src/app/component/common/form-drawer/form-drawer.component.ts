import {Component, EventEmitter, Input, Output, signal, ChangeDetectionStrategy} from '@angular/core';
import {DrawerComponent} from '../drawer/drawer.component';
import {ConfirmDialogComponent} from '../confirm-dialog/confirm-dialog.component';
import {IconComponent} from '../icon/icon.component';
import {ModalData} from '../../../model/modalData';

/**
 * Form-shaped drawer — wraps app-drawer (no second drawer primitive) and adds the three
 * things the entity forms need: a subtitle, the `wide` 560px variant, and a footer with
 * a dirty indicator + Cancel + primary submit.
 *
 * The <form> element lives in the projected content; the footer submit button reaches it
 * via the native `form` attribute, so callers must give their form the same id they pass
 * in `formId`. Submit stays disabled until `valid`; any close request (Cancel, X, Esc,
 * backdrop) while `dirty` routes through the discard guard (app-confirm-dialog).
 */
@Component({
  selector: 'app-form-drawer',
  imports: [DrawerComponent, ConfirmDialogComponent, IconComponent],
  template: `
    <!-- app-drawer must be created before app-confirm-dialog so its document-level Esc
         listener runs first: tryClose() no-ops while the guard is open, then the dialog's
         own Esc handler dismisses the guard. -->
    <app-drawer [open]="open" [title]="title" [subtitle]="subtitle" [wide]="wide" (closed)="tryClose()">
      <ng-content></ng-content>
      <div drawerFoot class="panel__foot">
        <span class="fdrawer__dirty">{{ dirty ? 'Unsaved changes' : 'No changes' }}</span>
        <div class="fdrawer__actions">
          <button type="button" class="btn" (click)="tryClose()">Cancel</button>
          <button type="submit" class="btn btn--primary" [disabled]="!valid" [attr.form]="formId">
            <app-icon name="check" [size]="12"></app-icon>{{ submitLabel }}
          </button>
        </div>
      </div>
    </app-drawer>

    <app-confirm-dialog [open]="confirmOpen()" [data]="discardGuard"
                        (confirmed)="discard()" (cancelled)="confirmOpen.set(false)">
    </app-confirm-dialog>
  `,
  changeDetection: ChangeDetectionStrategy.Eager,
  styles: [`
    .fdrawer__dirty {
      font-size: 12px;
      color: var(--ink-3);
    }
    .fdrawer__actions {
      display: flex;
      gap: 8px;
    }
  `]
})
export class FormDrawerComponent {
  @Input() open = false;
  @Input() title = '';
  @Input() subtitle = '';
  @Input() wide = false;
  @Input() submitLabel = 'Save';
  /** id of the projected <form>; wired to the footer submit button's `form` attribute. */
  @Input({required: true}) formId!: string;
  @Input() valid = false;
  @Input() dirty = false;
  /** Emitted once the drawer actually closes (clean close or confirmed discard). */
  @Output() closed = new EventEmitter<void>();

  readonly confirmOpen = signal(false);

  readonly discardGuard: ModalData = {
    header: 'Discard changes?',
    message: 'You have unsaved changes in this form. Closing now will discard them.',
    confirmLabel: 'Discard',
    tone: 'warn',
    icon: 'info'
  };

  tryClose(): void {
    if (this.confirmOpen()) return;
    if (this.dirty) {
      this.confirmOpen.set(true);
    } else {
      this.closed.emit();
    }
  }

  discard(): void {
    this.confirmOpen.set(false);
    this.closed.emit();
  }
}
