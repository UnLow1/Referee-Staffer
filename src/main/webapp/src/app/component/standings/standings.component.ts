import {Component, OnInit, computed, inject, signal} from '@angular/core';
import {forkJoin} from 'rxjs';
import {Team} from '../../model/team';
import {Match} from '../../model/match';
import {TeamService} from '../../service/team.service';
import {MatchService} from '../../service/match.service';
import {TeamPillComponent} from '../common/team-pill/team-pill.component';
import {SegComponent, SegOption} from '../common/seg/seg.component';

const NUMBER_OF_EDGE_TEAMS = 3;

export type StandingsFilter = 'all' | 'top' | 'bottom';

interface StandingRow {
  team: Team;
  pos: number;
  zone: 'accent' | 'danger' | null;
  played: number;
  wins: number;
  draws: number;
  losses: number;
  goalsFor: number;
  goalsAgainst: number;
}

/**
 * Standings — the full league table; standalone sibling of the Dashboard's standings
 * widget.
 *
 * Order and points come from /api/teams/standings (authoritative — points weighting is
 * configurable server-side); P/W/D/L/GF/GA are derived client-side from played matches,
 * the same way the team list derives "Played". Top 3 = accent zone, bottom 3 = danger,
 * matching the dashboard widget's NUMBER_OF_EDGE_TEAMS.
 */
@Component({
  selector: 'app-standings',
  templateUrl: './standings.component.html',
  styleUrls: ['./standings.component.scss'],
  imports: [TeamPillComponent, SegComponent]
})
export class StandingsComponent implements OnInit {
  private readonly teamService = inject(TeamService);
  private readonly matchService = inject(MatchService);

  readonly teams = signal<Team[]>([]);
  readonly matches = signal<Match[]>([]);
  readonly filter = signal<StandingsFilter>('all');

  readonly filterOptions: SegOption<StandingsFilter>[] = [
    {value: 'all', label: 'All'},
    {value: 'top', label: `Top ${NUMBER_OF_EDGE_TEAMS * 2}`},
    {value: 'bottom', label: `Bottom ${NUMBER_OF_EDGE_TEAMS}`}
  ];

  /** Highest queue with at least one played match — "after queue N" in the subtitle. */
  readonly latestPlayedQueue = computed<number | null>(() => {
    const played = this.matches().filter(m => m.homeScore != null && m.awayScore != null);
    if (played.length === 0) return null;
    return Math.max(...played.map(m => m.queue));
  });

  readonly rows = computed<StandingRow[]>(() => {
    const stats = computeStats(this.matches());
    const total = this.teams().length;
    return this.teams().map((team, i) => {
      const pos = i + 1;
      const s = stats.get(team.id) ?? emptyStats();
      const zone = pos <= NUMBER_OF_EDGE_TEAMS ? 'accent' as const
        : (pos > total - NUMBER_OF_EDGE_TEAMS && total >= NUMBER_OF_EDGE_TEAMS * 2) ? 'danger' as const
        : null;
      return {team, pos, zone, ...s};
    });
  });

  readonly visibleRows = computed(() => {
    const filter = this.filter();
    const total = this.rows().length;
    return this.rows().filter(row =>
      filter === 'all' ? true
        : filter === 'top' ? row.pos <= NUMBER_OF_EDGE_TEAMS * 2
        : row.pos > total - NUMBER_OF_EDGE_TEAMS);
  });

  readonly maxPoints = computed(() => {
    const points = this.teams().map(t => t.points ?? 0);
    return Math.max(...points, 1);
  });

  ngOnInit(): void {
    forkJoin({
      standings: this.teamService.getStandings(),
      matches: this.matchService.findAll()
    }).subscribe(({standings, matches}) => {
      this.teams.set(standings);
      this.matches.set(matches);
    });
  }

  goalDiff(row: StandingRow): number {
    return row.goalsFor - row.goalsAgainst;
  }

  formatGoalDiff(row: StandingRow): string {
    const gd = this.goalDiff(row);
    return gd > 0 ? `+${gd}` : `${gd}`;
  }

  barPct(row: StandingRow): number {
    return Math.round(((row.team.points ?? 0) / this.maxPoints()) * 100);
  }
}

type Stats = Omit<StandingRow, 'team' | 'pos' | 'zone'>;

function emptyStats(): Stats {
  return {played: 0, wins: 0, draws: 0, losses: 0, goalsFor: 0, goalsAgainst: 0};
}

function computeStats(matches: Match[]): Map<number, Stats> {
  const stats = new Map<number, Stats>();
  const of = (id: number) => {
    let s = stats.get(id);
    if (!s) {
      s = emptyStats();
      stats.set(id, s);
    }
    return s;
  };
  for (const m of matches) {
    if (m.homeScore == null || m.awayScore == null) continue;
    const home = of(m.homeTeamId);
    const away = of(m.awayTeamId);
    home.played++;
    away.played++;
    home.goalsFor += m.homeScore;
    home.goalsAgainst += m.awayScore;
    away.goalsFor += m.awayScore;
    away.goalsAgainst += m.homeScore;
    if (m.homeScore > m.awayScore) {
      home.wins++;
      away.losses++;
    } else if (m.homeScore < m.awayScore) {
      away.wins++;
      home.losses++;
    } else {
      home.draws++;
      away.draws++;
    }
  }
  return stats;
}
