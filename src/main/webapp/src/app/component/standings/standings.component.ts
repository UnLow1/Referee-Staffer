import {Component, OnInit, computed, inject, signal, ChangeDetectionStrategy} from '@angular/core';
import {Standing} from '../../model/standing';
import {TeamService} from '../../service/team.service';
import {ConfigurationService} from '../../service/configuration.service';
import {TeamPillComponent} from '../common/team-pill/team-pill.component';
import {SegComponent, SegOption} from '../common/seg/seg.component';

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
 * Zone sizes follow NUMBER_OF_EDGE_TEAMS from the backend configuration (top edge =
 * accent, bottom edge = danger), matching the dashboard widget.
 */
@Component({
  selector: 'app-standings',
  templateUrl: './standings.component.html',
  styleUrls: ['./standings.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [TeamPillComponent, SegComponent]
})
export class StandingsComponent implements OnInit {
  private readonly teamService = inject(TeamService);
  private readonly configurationService = inject(ConfigurationService);

  /** Edge-zone size (NUMBER_OF_EDGE_TEAMS) from the backend configuration. */
  readonly edgeTeams = this.configurationService.edgeTeams;

  readonly standings = signal<Standing[]>([]);
  readonly afterQueue = signal<number | null>(null);
  readonly filter = signal<StandingsFilter>('all');

  readonly filterOptions = computed<SegOption<StandingsFilter>[]>(() => [
    {value: 'all', label: 'All'},
    {value: 'top', label: `Top ${this.edgeTeams() * 2}`},
    {value: 'bottom', label: `Bottom ${this.edgeTeams()}`}
  ]);

  readonly rows = computed<StandingRow[]>(() => {
    const total = this.standings().length;
    const edge = this.edgeTeams();
    return this.standings().map(standing => {
      const zone = standing.place <= edge ? 'accent' as const
        : (standing.place > total - edge && total >= edge * 2) ? 'danger' as const
        : null;
      return {standing, zone};
    });
  });

  readonly visibleRows = computed(() => {
    const filter = this.filter();
    const total = this.rows().length;
    const edge = this.edgeTeams();
    return this.rows().filter(row =>
      filter === 'all' ? true
        : filter === 'top' ? row.standing.place <= edge * 2
        : row.standing.place > total - edge);
  });

  readonly maxPoints = computed(() => {
    const points = this.standings().map(s => s.points ?? 0);
    return Math.max(...points, 1);
  });

  ngOnInit(): void {
    this.configurationService.ensureEdgeTeamsLoaded();
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
