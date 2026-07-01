import {Component, computed, input} from '@angular/core';
import {Referee} from '../../../model/referee';

/**
 * Initial-circle avatar plus referee name. Two sizes:
 * - `sm` (default, 24×24) — name only
 * - `lg` (36×36) — name + email
 *
 * The avatar circle styles itself via `.avatar` / `.avatar--lg` from styles.scss.
 */
@Component({
  selector: 'app-ref-avatar',
  template: `
    <span class="ref-avatar">
      <span class="avatar" [class.avatar--lg]="size() === 'lg'">{{ initials() }}</span>
      <span class="ref-avatar__text">
        <div class="ref-avatar__name">{{ fullName() }}</div>
        @if (size() === 'lg' && referee()?.email) {
          <div class="ref-avatar__email">{{ referee()?.email }}</div>
        }
      </span>
    </span>
  `,
  styles: [`
    .ref-avatar {
      display: inline-flex;
      align-items: center;
      gap: 8px;
    }
    .ref-avatar__name {
      font-weight: 500;
      font-size: 13px;
    }
    .ref-avatar__email {
      font-size: 11px;
      color: var(--ink-3);
    }
  `]
})
export class RefAvatarComponent {
  readonly referee = input<Referee | undefined>(undefined);
  readonly size = input<'sm' | 'lg'>('sm');

  readonly initials = computed(() => {
    const r = this.referee();
    if (!r) return '';
    return ((r.firstName?.[0] ?? '') + (r.lastName?.[0] ?? '')).toUpperCase();
  });

  readonly fullName = computed(() => {
    const r = this.referee();
    if (!r) return '';
    return `${r.firstName ?? ''} ${r.lastName ?? ''}`.trim();
  });
}
