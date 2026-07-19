import {Component, OnInit, computed, inject, signal, ChangeDetectionStrategy} from '@angular/core';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {forkJoin} from 'rxjs';
import {Match} from '../../model/match';
import {Referee} from '../../model/referee';
import {Team} from '../../model/team';
import {Grade} from '../../model/grade';
import {RefereeService} from '../../service/referee.service';
import {MatchService} from '../../service/match.service';
import {TeamService} from '../../service/team.service';
import {GradeService} from '../../service/grade.service';
import {IconComponent} from '../common/icon/icon.component';
import {KpiComponent} from '../common/kpi/kpi.component';
import {TeamPillComponent} from '../common/team-pill/team-pill.component';
import {MeterComponent} from '../common/meter/meter.component';

/**
 * Referee profile — identity, key stats, and full match history.
 *
 *  - Hero (avatar + name + email) and KPI strip: avg grade, potential, and last queue
 *    come enriched from RefereeDto, with local fallbacks derived from the loaded
 *    match history; tiles show `—` when a value is genuinely absent.
 *  - The grade-trend chart from the design is intentionally not ported yet — it needs
 *    a per-match grade history list (already loaded) but the SVG chart is large; tracked
 *    as a follow-up.
 *  - The fairness panel: home/away balance bar driven by {@code homeWins} /
 *    {@code awayWins} from RefereeDto, plus a top-cities list derived locally from
 *    match history × team city.
 *  - Match history is fully wired: filter `/api/matches` by referee id, join in teams
 *    and grades.
 */
@Component({
  selector: 'app-referee-profile',
  templateUrl: './referee-profile.component.html',
  styleUrl: './referee-profile.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [RouterLink, IconComponent, KpiComponent, TeamPillComponent, MeterComponent]
})
export class RefereeProfileComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly refereeService = inject(RefereeService);
  private readonly matchService = inject(MatchService);
  private readonly teamService = inject(TeamService);
  private readonly gradeService = inject(GradeService);

  readonly referee = signal<Referee | null>(null);
  readonly matches = signal<Match[]>([]);
  readonly teamsById = signal<Map<number, Team>>(new Map());
  readonly gradesById = signal<Map<number, Grade>>(new Map());

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

  readonly idLabel = computed(() => {
    const r = this.referee();
    return r ? `#${String(r.id).padStart(3, '0')}` : '';
  });

  /** Sorted queue desc — newest match first. */
  readonly history = computed(() => {
    return [...this.matches()].sort((a, b) => (b.queue ?? 0) - (a.queue ?? 0));
  });

  /** Server-computed value when present, otherwise derived from the match history. */
  readonly lastQueue = computed<number | null>(() => {
    const fromServer = this.referee()?.lastQueue;
    if (typeof fromServer === 'number') return fromServer;
    return this.history()[0]?.queue ?? null;
  });

  readonly gradedMatchCount = computed(() =>
    this.matches().filter(m => m.gradeId != null).length
  );

  readonly avgGrade = computed<number | null>(() => {
    // Prefer the server-computed value (RefereeService.enrichWithStats applies the
    // DEFAULT_GRADE fallback consistently with the staffer pipeline). Fall back to a
    // local calc derived from the loaded grades while waiting on the referee response.
    const fromServer = this.referee()?.averageGrade;
    if (typeof fromServer === 'number') return fromServer;
    const grades = this.matches()
      .map(m => m.gradeId != null ? this.gradesById().get(m.gradeId)?.value : undefined)
      .filter((v): v is number => typeof v === 'number');
    if (grades.length === 0) return null;
    return grades.reduce((s, g) => s + g, 0) / grades.length;
  });

  readonly potential = computed<number | null>(() => {
    const p = this.referee()?.potential;
    return typeof p === 'number' ? p : null;
  });

  /** Home-team-win count from server. Falls back to 0 so the bar still renders cleanly. */
  readonly homeWins = computed<number>(() => this.referee()?.homeWins ?? 0);
  readonly awayWins = computed<number>(() => this.referee()?.awayWins ?? 0);

  /** True only when at least one win counter is non-zero — gates the bar so we don't
   *  render a degenerate flex-0/flex-0 strip for fresh referees. */
  readonly hasWinData = computed(() => this.homeWins() + this.awayWins() > 0);

  /**
   * Top 6 cities the referee has officiated in, sorted by appearance count desc. Each
   * match contributes once for the home team's city and once for the away team's. We
   * derive locally rather than asking the backend because we already have the full
   * matches × teams join in memory for the history table.
   */
  readonly topCities = computed<{city: string; count: number}[]>(() => {
    const teams = this.teamsById();
    const counts = new Map<string, number>();
    for (const m of this.matches()) {
      const homeCity = teams.get(m.homeTeamId)?.city;
      const awayCity = teams.get(m.awayTeamId)?.city;
      if (homeCity) counts.set(homeCity, (counts.get(homeCity) ?? 0) + 1);
      if (awayCity) counts.set(awayCity, (counts.get(awayCity) ?? 0) + 1);
    }
    return [...counts.entries()]
      .map(([city, count]) => ({city, count}))
      .sort((a, b) => b.count - a.count)
      .slice(0, 6);
  });

  /** Max city count for the bar widths. Returns 1 when empty so we never divide by zero. */
  readonly topCitiesMax = computed(() => Math.max(1, ...this.topCities().map(c => c.count)));

  cityBarWidth(count: number): string {
    return `${(count / this.topCitiesMax()) * 100}%`;
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.router.navigate(['/referees']);
      return;
    }

    forkJoin({
      referee: this.refereeService.findById(id),
      allMatches: this.matchService.findAll()
    }).subscribe(({referee, allMatches}) => {
      const refMatches = allMatches.filter(m => m.refereeId === id);
      const teamIds = unique(refMatches.flatMap(m => [m.homeTeamId, m.awayTeamId]));
      const gradeIds = unique(refMatches.map(m => m.gradeId).filter(notEmpty));

      forkJoin({
        teams: teamIds.length > 0 ? this.teamService.findByIds(teamIds) : Promise.resolve([] as Team[]),
        grades: gradeIds.length > 0 ? this.gradeService.findByIds(gradeIds) : Promise.resolve([] as Grade[])
      }).subscribe(({teams, grades}) => {
        this.referee.set(referee);
        this.matches.set(refMatches);
        this.teamsById.set(toMap(teams));
        this.gradesById.set(toMap(grades));
      });
    });
  }

  getTeam(teamId: number): Team | undefined {
    return this.teamsById().get(teamId);
  }

  getGrade(gradeId: number | null | undefined): Grade | undefined {
    if (gradeId == null) return undefined;
    return this.gradesById().get(gradeId);
  }

  scoreLine(match: Match): string | null {
    if (match.homeScore == null || match.awayScore == null) return null;
    return `${match.homeScore} – ${match.awayScore}`;
  }

  gradeAsMeter(grade: Grade | undefined): number {
    return (grade?.value ?? 0) * 10;
  }

  formatGrade(value: number | null): string {
    if (value == null) return '—';
    return value.toFixed(2);
  }

  editReferee(): void {
    const r = this.referee();
    if (!r) return;
    this.router.navigate(['/addReferee', r.id]);
  }
}

function unique<T>(arr: T[]): T[] {
  return [...new Set(arr)];
}

function notEmpty<T>(v: T | null | undefined): v is T {
  return v != null;
}

function toMap<T extends {id: number}>(items: T[]): Map<number, T> {
  return new Map(items.map(t => [t.id, t]));
}
