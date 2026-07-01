import {Component, computed, input} from '@angular/core';

export type MeterKind = 'default' | 'warn' | 'danger';

/**
 * Compact progress bar used for difficulty / potential / grade visualisations.
 * 80×4 bar plus a numeric value rendered in the mono font. Variants drive the fill
 * colour: default = accent green, warn = amber, danger = red.
 */
@Component({
  selector: 'app-meter',
  template: `
    <div class="meter" [class.meter--warn]="kind() === 'warn'" [class.meter--danger]="kind() === 'danger'">
      <div class="meter__bar"><i [style.width.%]="pct()"></i></div>
      <span class="num">{{ value() }}</span>
    </div>
  `
})
export class MeterComponent {
  readonly value = input.required<number>();
  readonly max = input(100);
  readonly kind = input<MeterKind>('default');

  readonly pct = computed(() => {
    const v = this.value();
    const m = this.max();
    if (m <= 0) return 0;
    return Math.min(100, Math.max(0, (v / m) * 100));
  });
}
