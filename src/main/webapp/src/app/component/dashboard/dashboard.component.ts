import {Component, OnInit, computed, inject, signal} from '@angular/core';
import {RouterLink} from '@angular/router';
import {forkJoin} from 'rxjs';
import {Match} from '../../model/match';
import {Referee} from '../../model/referee';
import {Team} from '../../model/team';
import {MatchService} from '../../service/match.service';
import {RefereeService} from '../../service/referee.service';
import {TeamService} from '../../service/team.service';
import {ConfigurationService} from '../../service/configuration.service';
import {IconComponent} from '../common/icon/icon.component';
import {KpiComponent} from '../common/kpi/kpi.component';
import {TeamPillComponent} from '../common/team-pill/team-pill.component';
import {RefAvatarComponent} from '../common/ref-avatar/ref-avatar.component';
import {MeterComponent} from '../common/meter/meter.component';

/**
 * Overview — at-a-glance dashboard.
 *
 *  - KPI strip — match/referee counts, aggregate difficulty of the upcoming queue, and
 *    the pool-wide average observer grade (from the enriched RefereeDto).
 *  - Upcoming · Queue N — fixtures from the earliest queue with unplayed matches,
 *    joined with team pills and the difficulty meter.
 *  - Top referees — sorted by `potential` desc (falls back to experience for
 *    un-enriched responses); the meter normalises against the top scorer.
 *  - Standings — only Pts + form bar (W/D/L/GF/GA need a season-stats endpoint that
 *    doesn't exist yet).
 */
@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  imports: [RouterLink, IconComponent, KpiComponent, TeamPillComponent, RefAvatarComponent, MeterComponent]
})
export class DashboardComponent implements OnInit {
  private readonly matchService = inject(MatchService);
  private readonly refereeService = inject(RefereeService);
  private readonly teamService = inject(TeamService);
  private readonly configurationService = inject(ConfigurationService);

  /** Edge-zone size (NUMBER_OF_EDGE_TEAMS) from the backend configuration. */
  readonly edgeTeams = this.configurationService.edgeTeams;

  readonly matches = signal<Match[]>([]);
  readonly referees = signal<Referee[]>([]);
  readonly standings = signal<Team[]>([]);

  readonly upcomingQueue = computed(() => {
    const unplayed = this.matches().filter(m => m.homeScore == null || m.awayScore == null);
    if (unplayed.length === 0) return null;
    return Math.min(...unplayed.map(m => m.queue));
  });

  readonly upcomingMatches = computed(() => {
    const q = this.upcomingQueue();
    if (q == null) return [];
    return this.matches()
      .filter(m => m.queue === q && (m.homeScore == null || m.awayScore == null))
      .sort((a, b) => (b.hardnessLvl ?? 0) - (a.hardnessLvl ?? 0));
  });

  readonly upcomingDifficulty = computed(() =>
    Math.round(this.upcomingMatches().reduce((s, m) => s + (m.hardnessLvl ?? 0), 0))
  );

  readonly topReferees = computed(() => {
    // Sort by potential desc. Falls back to experience for the brief window before
    // the response lands or for endpoints that don't enrich.
    return [...this.referees()]
      .sort((a, b) => {
        const ap = a.potential ?? a.experience ?? 0;
        const bp = b.potential ?? b.experience ?? 0;
        return bp - ap;
      })
      .slice(0, 7);
  });

  readonly topRefereesMaxPotential = computed(() => {
    const top = this.topReferees()[0]?.potential ?? this.topReferees()[0]?.experience ?? 0;
    return Math.max(top, 1);
  });

  /** Aggregate average across all referees with a non-null average grade. */
  readonly avgObserverGrade = computed<number | null>(() => {
    const grades = this.referees()
      .map(r => r.averageGrade)
      .filter((g): g is number => typeof g === 'number');
    if (grades.length === 0) return null;
    return grades.reduce((s, g) => s + g, 0) / grades.length;
  });

  readonly standingsTotal = computed(() => this.standings().length);

  readonly maxStandingsPoints = computed(() => {
    const points = this.standings().map(t => t.points ?? 0);
    return Math.max(...points, 1);
  });

  ngOnInit(): void {
    this.configurationService.ensureEdgeTeamsLoaded();
    forkJoin({
      matches: this.matchService.findAll(),
      referees: this.refereeService.findAll(),
      standings: this.teamService.getStandings()
    }).subscribe(({matches, referees, standings}) => {
      this.matches.set(matches);
      this.referees.set(referees);
      this.standings.set(standings);
    });
  }

  // ——— Helpers ———

  getTeam(teamId: number | undefined): Team | undefined {
    if (teamId == null) return undefined;
    return this.standings().find(t => t.id === teamId);
  }

  difficultyKind(value: number | null | undefined): 'default' | 'warn' {
    return (value ?? 0) >= 100 ? 'warn' : 'default';
  }

  potentialPct(referee: Referee): number {
    const value = referee.potential ?? referee.experience ?? 0;
    return Math.round((value / this.topRefereesMaxPotential()) * 100);
  }

  formatPotential(referee: Referee): string {
    return referee.potential !== null && referee.potential !== undefined
      ? referee.potential.toFixed(0)
      : (referee.experience ?? 0).toString();
  }

  formatAvg(value: number | null): string {
    return value !== null ? value.toFixed(1) : '—';
  }

  pointsBarPct(team: Team): number {
    return Math.round(((team.points ?? 0) / this.maxStandingsPoints()) * 100);
  }

  zoneFor(index: number): 'top' | 'relegation' | null {
    const total = this.standingsTotal();
    const edge = this.edgeTeams();
    if (index < edge) return 'top';
    if (index >= total - edge && total >= edge * 2) return 'relegation';
    return null;
  }
}
