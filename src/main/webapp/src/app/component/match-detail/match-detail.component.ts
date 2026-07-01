import {Component, OnInit, computed, inject, signal} from '@angular/core';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {forkJoin} from 'rxjs';
import {Match} from '../../model/match';
import {Team} from '../../model/team';
import {Referee} from '../../model/referee';
import {DifficultyBreakdown} from '../../model/difficultyBreakdown';
import {MatchService} from '../../service/match.service';
import {TeamService} from '../../service/team.service';
import {RefereeService} from '../../service/referee.service';
import {IconComponent} from '../common/icon/icon.component';
import {ChipComponent} from '../common/chip/chip.component';
import {RefAvatarComponent} from '../common/ref-avatar/ref-avatar.component';

const NUMBER_OF_EDGE_TEAMS = 3;

interface CompareRow {
  label: string;
  home: number | string;
  away: number | string;
  /** When both numeric, render mirrored bars. */
  bar: boolean;
}

/**
 * Match detail — read-only deep dive on a single fixture.
 *
 * Difficulty parts load from /api/matches/:id/difficulty; candidate ranking uses the
 * enriched RefereeDto `potential`. Per-team W/D/L/GF/GA still wait on a season-stats
 * endpoint.
 */
@Component({
  selector: 'app-match-detail',
  templateUrl: './match-detail.component.html',
  styleUrl: './match-detail.component.scss',
  imports: [RouterLink, IconComponent, ChipComponent, RefAvatarComponent]
})
export class MatchDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly matchService = inject(MatchService);
  private readonly teamService = inject(TeamService);
  private readonly refereeService = inject(RefereeService);

  readonly match = signal<Match | null>(null);
  readonly breakdown = signal<DifficultyBreakdown | null>(null);
  readonly home = signal<Team | undefined>(undefined);
  readonly away = signal<Team | undefined>(undefined);
  /** Standings sorted by points desc — index lookup for `place`. */
  readonly standings = signal<Team[]>([]);
  readonly referees = signal<Referee[]>([]);
  readonly assignedReferee = signal<Referee | undefined>(undefined);

  readonly homePlace = computed(() => this.placeOf(this.home()?.id));
  readonly awayPlace = computed(() => this.placeOf(this.away()?.id));

  readonly pointsDiff = computed(() => {
    const h = this.home()?.points ?? 0;
    const a = this.away()?.points ?? 0;
    return Math.abs(h - a);
  });

  /*
   * Flags prefer backend breakdown when loaded — it's authoritative because it uses the
   * server-side place computation. Until the breakdown lands they fall back to the local
   * derivation, which keeps the chip row meaningful during the brief loading window.
   */
  readonly sameCity = computed(() => {
    const flags = this.breakdown()?.flags;
    if (flags) return flags.sameCity;
    const h = this.home();
    const a = this.away();
    return !!(h?.city && a?.city && h.city === a.city);
  });

  readonly isTopMatch = computed(() => {
    const flags = this.breakdown()?.flags;
    if (flags) return flags.isTop;
    const hp = this.homePlace();
    const ap = this.awayPlace();
    return hp != null && ap != null && hp <= NUMBER_OF_EDGE_TEAMS && ap <= NUMBER_OF_EDGE_TEAMS;
  });

  readonly isRelegationMatch = computed(() => {
    const flags = this.breakdown()?.flags;
    if (flags) return flags.isBot;
    const hp = this.homePlace();
    const ap = this.awayPlace();
    const total = this.standings().length;
    return hp != null && ap != null && total > 0
      && hp > total - NUMBER_OF_EDGE_TEAMS
      && ap > total - NUMBER_OF_EDGE_TEAMS;
  });

  readonly compareRows = computed<CompareRow[]>(() => {
    const h = this.home();
    const a = this.away();
    const hp = this.homePlace();
    const ap = this.awayPlace();
    if (!h || !a) return [];
    return [
      {label: 'Position', home: hp != null ? `#${hp}` : '—', away: ap != null ? `#${ap}` : '—', bar: false},
      {label: 'Points', home: h.points ?? 0, away: a.points ?? 0, bar: true},
      // W / D / L / GF / GA need a backend stats endpoint that doesn't exist yet. For
      // now they're omitted; placeholder note in the template tells the reader why the
      // panel feels short.
    ];
  });

  readonly candidates = computed(() => {
    // Sort by potential desc. Falls back to experience for un-enriched responses.
    const assignedId = this.match()?.refereeId;
    return [...this.referees()]
      .sort((x, y) => {
        const xp = x.potential ?? x.experience ?? 0;
        const yp = y.potential ?? y.experience ?? 0;
        return yp - xp;
      })
      .map(r => ({referee: r, isAssigned: r.id === assignedId}));
  });

  ngOnInit(): void {
    const matchId = Number(this.route.snapshot.paramMap.get('id'));
    if (!matchId) {
      this.router.navigate(['/matches']);
      return;
    }
    forkJoin({
      match: this.matchService.findById(matchId),
      breakdown: this.matchService.getDifficultyBreakdown(matchId),
      standings: this.teamService.getStandings(),
      referees: this.refereeService.findAll()
    }).subscribe(({match, breakdown, standings, referees}) => {
      this.match.set(match);
      this.breakdown.set(breakdown);
      this.standings.set(standings);
      this.referees.set(referees);
      this.home.set(standings.find(t => t.id === match.homeTeamId));
      this.away.set(standings.find(t => t.id === match.awayTeamId));
      this.assignedReferee.set(referees.find(r => r.id === match.refereeId));
    });
  }

  assign(referee: Referee): void {
    const m = this.match();
    if (!m) return;
    const updated: Match = {...m, refereeId: referee.id};
    this.matchService.update(updated).subscribe(saved => {
      this.match.set(saved);
      this.assignedReferee.set(referee);
    });
  }

  scoreLine(): string | null {
    const m = this.match();
    if (!m || m.homeScore == null || m.awayScore == null) return null;
    return `${m.homeScore} – ${m.awayScore}`;
  }

  /** Mirrored bar widths normalise both sides to the larger value (or 1 if both 0). */
  barWidth(value: number, otherValue: number): string {
    const max = Math.max(value, otherValue, 1);
    return `${(value / max) * 100}%`;
  }

  isNumber(value: unknown): value is number {
    return typeof value === 'number';
  }

  asNumber(value: number | string): number {
    return typeof value === 'number' ? value : 0;
  }

  round(value: number | null | undefined): number {
    return Math.round(value ?? 0);
  }

  private placeOf(teamId: number | undefined): number | null {
    if (teamId == null) return null;
    const idx = this.standings().findIndex(t => t.id === teamId);
    return idx >= 0 ? idx + 1 : null;
  }
}
