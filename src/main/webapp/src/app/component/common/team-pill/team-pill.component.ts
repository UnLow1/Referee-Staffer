import {Component, computed, input, ChangeDetectionStrategy} from '@angular/core';
import {Team} from '../../../model/team';

/**
 * 22×22 rounded square with the team's 3-letter code (mono uppercase) plus the full
 * team name. `side` flips the colour treatment — home is dark, away is light.
 *
 * The backend sends `Team.short` (TeamDto renames shortCode → short); this atom still
 * derives the code from `name` as a fallback so list views render coherently even
 * when the field is absent.
 */
@Component({
  selector: 'app-team-pill',
  template: `
    <span class="team-pill">
      <span class="team-pill__short" [class.team-pill__short--home]="side() === 'home'">{{ short() }}</span>
      <span class="team-pill__name">{{ team()?.name }}</span>
    </span>
  `,
  changeDetection: ChangeDetectionStrategy.Eager,
  styles: [`
    .team-pill {
      display: inline-flex;
      align-items: center;
      gap: 8px;
    }
    /* Default colour treatment is the away variant; --home overrides for home side. */
    .team-pill__short {
      width: 22px;
      height: 22px;
      border-radius: 4px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      font-family: var(--mono);
      font-size: 9px;
      font-weight: 600;
      letter-spacing: 0.02em;
      background: var(--panel-2);
      color: var(--ink-2);
      border: 1px solid var(--line);
    }
    /* var(--bg) instead of literal white: --ink flips light in dark theme, and a white
       glyph on a light chip would vanish. --bg always contrasts with --ink. */
    .team-pill__short--home {
      background: var(--ink);
      color: var(--bg);
      border-color: var(--ink);
    }
    .team-pill__name {
      font-weight: 500;
    }
  `]
})
export class TeamPillComponent {
  readonly team = input<Team | undefined>(undefined);
  readonly side = input<'home' | 'away'>('home');

  readonly short = computed(() => {
    const t = this.team();
    if (!t) return '';
    return t.short ?? t.name?.slice(0, 3).toUpperCase() ?? '';
  });
}
