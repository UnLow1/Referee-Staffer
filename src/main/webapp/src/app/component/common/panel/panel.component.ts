import {Component} from '@angular/core';

/**
 * Generic card wrapper — bordered, rounded, with optional head/foot slots.
 *
 * Usage:
 * <app-panel>
 *   <app-panel-head>
 *     <span class="panel__title">Upcoming · Queue 30</span>
 *     <button class="btn btn--sm">Filter</button>
 *   </app-panel-head>
 *   ...body content...
 *   <app-panel-foot>...action row...</app-panel-foot>
 * </app-panel>
 *
 * The body slot is wrapped in `.panel__body` for consistent 18px padding. Skip the
 * wrapper when you need a flush layout (tables, KPI strips) by using the raw `.panel`
 * class directly instead of this component.
 */
@Component({
  selector: 'app-panel',
  template: `
    <div class="panel">
      <ng-content select="app-panel-head"></ng-content>
      <div class="panel__body">
        <ng-content></ng-content>
      </div>
      <ng-content select="app-panel-foot"></ng-content>
    </div>
  `
})
export class PanelComponent {}

@Component({
  selector: 'app-panel-head',
  template: `<div class="panel__head"><ng-content></ng-content></div>`
})
export class PanelHeadComponent {}

@Component({
  selector: 'app-panel-foot',
  template: `<div class="panel__foot"><ng-content></ng-content></div>`
})
export class PanelFootComponent {}
