import {Component, Input} from '@angular/core';

/**
 * Single KPI cell — label, big mono value, optional delta line. Stack horizontally
 * inside a panel and the right-border divider comes from styles.scss (.kpi rule).
 *
 * Usage:
 * <div class="panel">
 *   <div class="kpi-strip">
 *     <app-kpi label="Active referees" value="19"></app-kpi>
 *     <app-kpi label="Queue 30 difficulty" value="142.5" delta="+3.1 vs Q29"></app-kpi>
 *   </div>
 * </div>
 */
@Component({
  selector: 'app-kpi',
  template: `
    <div class="kpi">
      <div class="kpi__label">{{ label }}</div>
      <div class="kpi__value">{{ value }}</div>
      @if (delta) {
        <div class="kpi__delta">{{ delta }}</div>
      }
    </div>
  `
})
export class KpiComponent {
  @Input({required: true}) label = '';
  @Input({required: true}) value: string | number = '';
  @Input() delta?: string;
}
