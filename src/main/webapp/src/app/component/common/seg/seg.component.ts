import {Component, EventEmitter, Input, Output} from '@angular/core';

export interface SegOption<T = string | number> {
  value: T;
  label: string;
}

/**
 * Segmented button group. Used for queue stepper, density toggle, filter tabs.
 * Two-way bindable via `[(value)]` — emits `valueChange` on click.
 *
 * Usage:
 * <app-seg [(value)]="queue" [options]="[
 *   {value: 28, label: 'Queue 28'},
 *   {value: 29, label: 'Queue 29'},
 *   {value: 30, label: 'Queue 30'}
 * ]"></app-seg>
 */
@Component({
  selector: 'app-seg',
  template: `
    <div class="seg">
      @for (opt of options; track opt.value) {
        <button type="button"
                [class.active]="opt.value === value"
                (click)="select(opt.value)">{{ opt.label }}</button>
      }
    </div>
  `
})
export class SegComponent<T = string | number> {
  @Input({required: true}) options: SegOption<T>[] = [];
  @Input() value!: T;
  @Output() valueChange = new EventEmitter<T>();

  select(v: T): void {
    this.value = v;
    this.valueChange.emit(v);
  }
}
