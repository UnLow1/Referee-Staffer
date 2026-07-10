import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from '@angular/core';
import {IconComponent} from '../icon/icon.component';

/**
 * Client-side paginator. Shows the visible range ("26–50 of 240") plus prev/next
 * ghost buttons. Purely presentational — the owner slices its own list and feeds
 * back `page` on `pageChange`.
 *
 * Usage:
 * <app-paginator [total]="items().length" [page]="page()" [pageSize]="25"
 *                (pageChange)="page.set($event)"></app-paginator>
 */
@Component({
  selector: 'app-paginator',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [IconComponent],
  template: `
    <div class="pager">
      <span class="pager__range">{{ rangeStart }}–{{ rangeEnd }} of {{ total }}</span>
      <div class="pager__controls">
        <button type="button" class="btn btn--ghost btn--sm" [disabled]="page <= 1"
                (click)="goTo(page - 1)" title="Previous page">
          <app-icon name="chevLeft" [size]="12"></app-icon>
        </button>
        <span class="pager__page">{{ page }} / {{ totalPages }}</span>
        <button type="button" class="btn btn--ghost btn--sm" [disabled]="page >= totalPages"
                (click)="goTo(page + 1)" title="Next page">
          <app-icon name="chevRight" [size]="12"></app-icon>
        </button>
      </div>
    </div>
  `
})
export class PaginatorComponent {
  @Input({required: true}) total = 0;
  /** 1-based. The owner is expected to keep it within 1..totalPages. */
  @Input({required: true}) page = 1;
  @Input() pageSize = 25;
  @Output() pageChange = new EventEmitter<number>();

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.total / this.pageSize));
  }

  get rangeStart(): number {
    return this.total === 0 ? 0 : (this.page - 1) * this.pageSize + 1;
  }

  get rangeEnd(): number {
    return Math.min(this.page * this.pageSize, this.total);
  }

  goTo(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.pageChange.emit(page);
  }
}
