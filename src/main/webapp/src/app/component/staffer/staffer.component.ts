import {Component, computed, inject, signal, ChangeDetectionStrategy} from '@angular/core';
import {forkJoin} from 'rxjs';
import {saveAs} from 'file-saver';
import {StafferService} from '../../service/staffer.service';
import {TeamService} from '../../service/team.service';
import {RefereeService} from '../../service/referee.service';
import {MatchService} from '../../service/match.service';
import {ConfigurationService} from '../../service/configuration.service';
import {UiSettingsService} from '../../service/ui-settings.service';
import {Match} from '../../model/match';
import {Team} from '../../model/team';
import {Referee} from '../../model/referee';
import {DifficultyBreakdown} from '../../model/difficultyBreakdown';
import {IconComponent} from '../common/icon/icon.component';
import {TeamPillComponent} from '../common/team-pill/team-pill.component';
import {RefAvatarComponent} from '../common/ref-avatar/ref-avatar.component';
import {MeterComponent} from '../common/meter/meter.component';
import {ChipComponent} from '../common/chip/chip.component';
import {KpiComponent} from '../common/kpi/kpi.component';
import {DrawerComponent} from '../common/drawer/drawer.component';

interface MatchFlags {
  sameCity: boolean;
  isTop: boolean;
  isBot: boolean;
}

interface Candidate {
  referee: Referee;
  isAssigned: boolean;
  isUsedElsewhere: boolean;
}

/**
 * Staffer — the auto-assignment workspace. Pick a queue, generate the cast, lock or
 * swap individual rows, then save. Locked pairs are sent with the staffing request,
 * so a regenerate preserves them server-side and reshuffles only the rest.
 */
@Component({
  selector: 'app-staffer',
  templateUrl: './staffer.component.html',
  styleUrl: './staffer.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    IconComponent, TeamPillComponent, RefAvatarComponent, MeterComponent,
    ChipComponent, KpiComponent, DrawerComponent
  ]
})
export class StafferComponent {
  private readonly stafferService = inject(StafferService);
  private readonly teamService = inject(TeamService);
  private readonly refereeService = inject(RefereeService);
  private readonly matchService = inject(MatchService);
  private readonly configurationService = inject(ConfigurationService);
  /** Gates the "How the staffer scores assignments" panel (toggled in the sidebar's Admin section). */
  readonly settings = inject(UiSettingsService);

  /** Edge-zone size (NUMBER_OF_EDGE_TEAMS) from the backend configuration. */
  readonly edgeTeams = this.configurationService.edgeTeams;

  readonly queue = signal(1);
  /**
   * Queues the season actually has, ascending (GET /api/matches/queues). The stepper walks
   * this list instead of incrementing a counter, so it can never select a queue that does
   * not exist (RS-115). Empty until the request lands, or when nothing has been imported.
   */
  readonly availableQueues = signal<number[]>([]);
  /**
   * Where the queue request got to. 'loading' must stay apart from a genuinely empty season:
   * both have no queues, but only one of them means "nothing has been imported", and the
   * screen says so. 'failed' keeps the screen usable — see canGenerate.
   */
  readonly queuesState = signal<'loading' | 'ready' | 'failed'>('loading');
  readonly matches = signal<Match[] | null>(null);
  readonly referees = signal<Referee[]>([]);
  /** Map<teamId, Team> — populated from /api/teams/standings rows. */
  readonly teamsById = signal<Map<number, Team>>(new Map());
  /** Map<teamId, place> — the backend-computed table position. */
  readonly placeById = signal<Map<number, number>>(new Map());
  readonly totalTeams = signal(0);

  /**
   * Map<matchId, refereeId> of locked assignments. Sent with the staffing request:
   * the backend pins each pair and re-staffs only the remaining matches, so locks
   * survive a regenerate. Cleared on queue change — they reference matches of the
   * previously generated queue.
   */
  readonly locks = signal<Map<number, number>>(new Map());
  readonly drawerMatchId = signal<number | null>(null);
  /** Lazy-loaded breakdown for the currently-open drawer. Null while pending or absent. */
  readonly drawerBreakdown = signal<DifficultyBreakdown | null>(null);
  readonly savedAt = signal<Date | null>(null);
  readonly loading = signal(false);
  readonly exporting = signal(false);

  constructor() {
    this.configurationService.ensureEdgeTeamsLoaded();
    this.matchService.getQueues().subscribe({
      next: queues => {
        this.availableQueues.set(queues);
        this.queuesState.set('ready');
        // Queue 1 is only a placeholder default — snap onto a queue that exists so the
        // stepper never starts outside its own range. An empty season leaves it alone;
        // there is nothing to snap to and Generate is disabled anyway.
        if (queues.length > 0 && !queues.includes(this.queue())) {
          this.queue.set(queues[0]);
        }
      },
      // The interceptor already toasts the reason. Recording the failure keeps the screen
      // from claiming the season is empty, and keeps Generate reachable — one unrelated
      // request must not be able to lock the workspace.
      error: () => this.queuesState.set('failed')
    });
  }

  // ——— Derived state ———

  readonly sortedMatches = computed(() => {
    const ms = this.matches();
    if (!ms) return [];
    return [...ms].sort((a, b) => (b.hardnessLvl ?? 0) - (a.hardnessLvl ?? 0));
  });

  readonly totalDifficulty = computed(() =>
    Math.round((this.matches() ?? []).reduce((sum, m) => sum + (m.hardnessLvl ?? 0), 0))
  );

  readonly lockCount = computed(() => this.locks().size);

  /** Position of the selected queue in availableQueues; -1 while the list is still loading. */
  private readonly queueIndex = computed(() => this.availableQueues().indexOf(this.queue()));

  readonly hasQueues = computed(() => this.availableQueues().length > 0);

  /**
   * Generating needs a queue known to exist — or a queue list we failed to load, where the
   * backend validates the queue instead (400 below 1, 404 outside the season) and the user
   * keeps a working screen rather than a dead one.
   */
  readonly canGenerate = computed(() => this.hasQueues() || this.queuesState() === 'failed');

  /** Why Generate is disabled, as the button's tooltip. */
  readonly generateHint = computed(() => {
    if (this.canGenerate()) {
      return 'Assign referees to this queue';
    }
    return this.queuesState() === 'loading'
      ? 'Loading the season\'s queues…'
      : 'Import a season first — there are no matches to staff';
  });
  readonly canDecQueue = computed(() => this.queueIndex() > 0);
  readonly canIncQueue = computed(() => {
    const index = this.queueIndex();
    return index >= 0 && index < this.availableQueues().length - 1;
  });

  /**
   * A generated cast with no rows. Distinct from "not generated yet" (matches() === null):
   * the queue exists, it simply has nothing assignable left — every match is already played
   * or held by the central-assignment sentinel. The template says so instead of rendering an
   * empty table under zeroed KPI tiles, which used to look identical to a mistyped queue.
   */
  readonly castIsEmpty = computed(() => this.matches()?.length === 0);

  /**
   * The sheet is rendered from what the backend has stored, so it may only be exported
   * once the cast on screen has been accepted with Save cast. Generating alone is not
   * enough: manual swaps live in the component until saved, and a sheet that silently
   * disagreed with the table on screen would be worse than no sheet.
   */
  readonly canExport = computed(() => this.matches() !== null && this.savedAt() !== null);

  readonly drawerMatch = computed<Match | null>(() => {
    const id = this.drawerMatchId();
    if (id == null) return null;
    return this.matches()?.find(m => m.id === id) ?? null;
  });

  // ——— Public actions ———

  incQueue(): void {
    this.stepQueue(1);
  }

  decQueue(): void {
    this.stepQueue(-1);
  }

  /**
   * Moves to the neighbouring queue in availableQueues. Stepping by list position rather
   * than by number also keeps gaps in the season (a queue with no matches at all) out of
   * reach, and is a no-op at either end.
   */
  private stepQueue(delta: number): void {
    const index = this.queueIndex();
    if (index < 0) {
      return;
    }
    const next = this.availableQueues()[index + delta];
    if (next === undefined) {
      return;
    }
    this.queue.set(next);
    // The cast on screen belongs to the queue we just left. Keeping it would make the
    // empty-cast panel assert a reason for a queue nothing was ever computed for, and would
    // leave canExport() true so Export PDF fetched a different queue's sheet than the table.
    this.matches.set(null);
    this.savedAt.set(null);
    this.closeDrawer();
    this.clearLocks();
  }

  clearLocks(): void {
    this.locks.set(new Map());
  }

  generate(): void {
    if (!this.canGenerate()) {
      return;
    }
    this.loading.set(true);
    this.savedAt.set(null);
    const locks = Array.from(this.locks(), ([matchId, refereeId]) => ({matchId, refereeId}));
    forkJoin({
      matches: this.stafferService.staffReferees(this.queue(), locks),
      standings: this.teamService.getStandings(),
      referees: this.refereeService.findRefereesAvailableForQueue(this.queue())
    }).subscribe({
      next: ({matches, standings, referees}) => {
        // Build lookup maps so flag derivation (top/bottom) doesn't re-scan the table
        // on every cell render; `place` comes straight from the backend row.
        const teamsMap = new Map<number, Team>();
        const placeMap = new Map<number, number>();
        standings.rows.forEach(t => {
          teamsMap.set(t.id, t);
          placeMap.set(t.id, t.place);
        });
        this.teamsById.set(teamsMap);
        this.placeById.set(placeMap);
        this.totalTeams.set(standings.rows.length);
        this.referees.set(referees);
        this.matches.set([...matches]);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  /**
   * Downloads the assignment sheet PDF for the selected queue. Gated on a saved cast
   * (see canExport) — the template disables the button, and this guard keeps the rule
   * in one place for any other caller.
   */
  exportPdf(): void {
    if (!this.canExport()) {
      return;
    }
    this.exporting.set(true);
    this.matchService.downloadAssignmentsPdf(this.queue()).subscribe({
      next: blob => {
        saveAs(blob, `referee-assignments-queue-${this.queue()}.pdf`);
        this.exporting.set(false);
      },
      error: () => this.exporting.set(false)
    });
  }

  toggleLock(match: Match): void {
    this.locks.update(prev => {
      const next = new Map(prev);
      if (next.has(match.id)) {
        next.delete(match.id);
      } else if (match.refereeId != null) {
        next.set(match.id, match.refereeId);
      }
      return next;
    });
  }

  openDrawer(match: Match): void {
    this.drawerMatchId.set(match.id);
    this.drawerBreakdown.set(null);
    this.matchService.getDifficultyBreakdown(match.id)
      .subscribe(breakdown => {
        // Guard against a race: another row may have been clicked before the response landed.
        if (this.drawerMatchId() === match.id) {
          this.drawerBreakdown.set(breakdown);
        }
      });
  }

  closeDrawer(): void {
    this.drawerMatchId.set(null);
    this.drawerBreakdown.set(null);
  }

  swap(match: Match, refereeId: number): void {
    this.matches.update(ms => {
      if (!ms) return ms;
      return ms.map(m => m.id === match.id ? {...m, refereeId} : m);
    });
    // Swapping in the drawer counts as locking — the user's intent is "use this referee".
    this.locks.update(prev => {
      const next = new Map(prev);
      next.set(match.id, refereeId);
      return next;
    });
    this.savedAt.set(null);
  }

  save(): void {
    const ms = this.matches();
    if (!ms) return;
    this.matchService.updateList(ms).subscribe(() => this.savedAt.set(new Date()));
  }

  // ——— Read helpers used by the template ———

  getTeam(teamId: number | undefined): Team | undefined {
    if (teamId == null) return undefined;
    return this.teamsById().get(teamId);
  }

  getReferee(refereeId: number | null | undefined): Referee | undefined {
    if (refereeId == null) return undefined;
    return this.referees().find(r => r.id === refereeId);
  }

  isLocked(match: Match): boolean {
    return this.locks().has(match.id);
  }

  flags(match: Match): MatchFlags {
    const home = this.getTeam(match.homeTeamId);
    const away = this.getTeam(match.awayTeamId);
    const homePlace = this.placeById().get(match.homeTeamId);
    const awayPlace = this.placeById().get(match.awayTeamId);
    const total = this.totalTeams();

    const edge = this.edgeTeams();
    const sameCity = !!(home?.city && away?.city && home.city === away.city);
    const isTop = !!(homePlace && awayPlace && homePlace <= edge && awayPlace <= edge);
    const isBot = !!(homePlace && awayPlace && total > 0
      && homePlace > total - edge && awayPlace > total - edge);
    return {sameCity, isTop, isBot};
  }

  hasNoFlags(match: Match): boolean {
    const f = this.flags(match);
    return !f.sameCity && !f.isTop && !f.isBot;
  }

  /** Difficulty meter intensity: warn when a match is in the upper third of the scale. */
  difficultyKind(value: number | null | undefined): 'default' | 'warn' {
    return (value ?? 0) >= 100 ? 'warn' : 'default';
  }

  /**
   * Drawer candidate list: every available referee, with per-row flags so the template
   * can dim the ones already used by another match in this queue and highlight the
   * currently-assigned one. Sorted by potential desc; falls back to experience for
   * un-enriched responses.
   */
  candidatesFor(match: Match): Candidate[] {
    const used = new Set(
      (this.matches() ?? [])
        .filter(m => m.id !== match.id)
        .map(m => m.refereeId)
        .filter((id): id is number => id != null)
    );
    return [...this.referees()]
      .sort((a, b) => {
        const ap = a.potential ?? a.experience ?? 0;
        const bp = b.potential ?? b.experience ?? 0;
        return bp - ap;
      })
      .map<Candidate>(r => ({
        referee: r,
        isAssigned: r.id === match.refereeId,
        isUsedElsewhere: used.has(r.id)
      }));
  }

  formatTime(d: Date): string {
    return d.toLocaleTimeString();
  }

  round(value: number | null | undefined): number {
    return Math.round(value ?? 0);
  }

  pad2(n: number): string {
    return n.toString().padStart(2, '0');
  }
}
