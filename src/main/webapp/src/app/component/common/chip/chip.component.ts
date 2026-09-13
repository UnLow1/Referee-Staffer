import {Component, Input, ChangeDetectionStrategy} from '@angular/core';

export type ChipVariant = 'default' | 'accent' | 'warn' | 'danger' | 'ghost';

/**
 * 20px-tall pill with optional leading dot. Used for derby / top-3 / relegation flags
 * on the Staffer table and Match detail hero. Content is projected via <ng-content>.
 */
@Component({
  selector: 'app-chip',
  changeDetection: ChangeDetectionStrategy.Eager,
  template: `
    <span class="chip"
          [class.chip--accent]="variant === 'accent'"
          [class.chip--warn]="variant === 'warn'"
          [class.chip--danger]="variant === 'danger'"
          [class.chip--ghost]="variant === 'ghost'">
      @if (dot) {
        <span class="dot"
              [class.dot--accent]="variant === 'accent'"
              [class.dot--warn]="variant === 'warn'"
              [class.dot--danger]="variant === 'danger'"></span>
      }
      <ng-content></ng-content>
    </span>
  `
})
export class ChipComponent {
  @Input() variant: ChipVariant = 'default';
  @Input() dot = false;
}
