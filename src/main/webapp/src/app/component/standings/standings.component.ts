import {Component, OnInit, computed, inject, signal} from '@angular/core';
import {Standing} from '../../model/standing';
import {TeamService} from '../../service/team.service';
import {TeamPillComponent} from '../common/team-pill/team-pill.component';
import {SegComponent, SegOption} from '../common/seg/seg.component';

const NUMBER_OF_EDGE_TEAMS = 3;

export type StandingsFilter = 'all' | 'top' | 'bottom';

interface StandingRow {
  standing: Standing;
  zone: 'accent' | 'danger' | null;
}

/**
 * Standings — the full league table; standalone sibling of the Dashboard's standings
 * widget.
 *
 * The whole table (order, points, place, P/W/D/L/GF/GA and the "after queue N"
 * subtitle) comes from /api/teams/standings — nothing is derived client-side anymore.
 * Top 3 = accent zone, bottom 3 = danger, matching the dashboard widget's
 * NUMBER_OF_EDGE_TEAMS.
 */
@Component({
  selector: 'app-standings',
  templateUrl: './standings.component.html',
  styleUrls: ['./standings.component.scss'],
  imports: [TeamPillComponent, SegComponent]
})
export class StandingsComponent implements OnInit {
  private readonly teamService = inject(TeamService);

  readonly standings = signal<Standing[]>([]);
  readonly afterQueue = signal<number | null>(null);
  readonly filter = signal<StandingsFilter>('all');

  readonly filterOptions: SegOption<StandingsFilter>[] = [
    {value: 'all', label: 'All'},
    {value: 'top', label: `Top ${NUMBER_OF_EDGE_TEAMS * 2}`},
    {value: 'bottom', label: `Bottom ${NUMBER_OF_EDGE_TEAMS}`}
  ];

  readonly rows = computed<StandingRow[]>(() => {
    const total = this.standings().length;
    return this.standings().map(standing => {
      const zone = standing.place <= NUMBER_OF_EDGE_TEAMS ? 'accent' as const
        : (standing.place > total - NUMBER_OF_EDGE_TEAMS && total >= NUMBER_OF_EDGE_TEAMS * 2) ? 'danger' as const
        : null;
      return {standing, zone};
    });
  });

  readonly visibleRows = computed(() => {
    const filter = this.filter();
    const total = this.rows().length;
    return this.rows().filter(row =>
      filter === 'all' ? true
        : filter === 'top' ? row.standing.place <= NUMBER_OF_EDGE_TEAMS * 2
        : row.standing.place > total - NUMBER_OF_EDGE_TEAMS);
  });

  readonly maxPoints = computed(() => {
    const points = this.standings().map(s => s.points ?? 0);
    return Math.max(...points, 1);
  });

  ngOnInit(): void {
    this.teamService.getStandings().subscribe(standings => {
      this.standings.set(standings.rows);
      this.afterQueue.set(standings.afterQueue);
    });
  }

  goalDiff(row: StandingRow): number {
    return row.standing.goalsFor - row.standing.goalsAgainst;
  }

  formatGoalDiff(row: StandingRow): string {
    const gd = this.goalDiff(row);
    return gd > 0 ? `+${gd}` : `${gd}`;
  }

  barPct(row: StandingRow): number {
    return Math.round(((row.standing.points ?? 0) / this.maxPoints()) * 100);
  }
}
