import {Component, computed, inject, signal, ChangeDetectionStrategy, OnInit} from '@angular/core';
import {forkJoin, Observable} from 'rxjs';
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
 * Staffer — the auto-assignment workspace. Pick a queue, review what is stored for it,
 * generate a new cast, lock or swap individual rows, then save. Locked pairs are sent with
 * the staffing request, so a regenerate preserves them server-side and reshuffles the rest.
 *
 * <p>Since RS-105 generating is a draft: the backend computes the cast without writing it,
 * so nothing reaches the database until Save cast. The screen therefore has two sources for
 * the table — the stored cast (loaded on entry and on every queue change) and a generated
 * draft — and `persisted` says which one is on screen.
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
export class StafferComponent implements OnInit {
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
  /**
   * Set when this screen saved the cast, so the footer can say when. A stored cast loaded
   * from the backend leaves it null — nothing records when it was saved.
   */
  readonly savedAt = signal<Date | null>(null);
  /**
   * Whether the cast on screen is the one the backend has stored. True right after a load
   * or a successful save, false for a generated draft, after any manual swap, and while a
   * request is in flight. The PDF is rendered from stored assignments, so this is what gates
   * the export.
   */
  readonly persisted = signal(false);
  /** Set when a cast request failed, so the empty state can offer a retry instead of a lie. */
  readonly loadFailed = signal(false);
  readonly loading = signal(false);
  readonly exporting = signal(false);

  constructor() {
    this.configurationService.ensureEdgeTeamsLoaded();
  }

  ngOnInit(): void {
    this.loadStoredCast();
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

  readonly matchCount = computed(() => this.matches()?.length ?? 0);

  readonly hasCast = computed(() => this.matchCount() > 0);

  /**
   * The sheet is rendered from what the backend has stored, so it may only be exported while
   * the table on screen agrees with the database: a stored cast straight after loading it, or
   * a draft that has been accepted with Save cast. A sheet that silently disagreed with the
   * table would be worse than no sheet.
   *
   * Deliberately not conditioned on the cast being non-empty: the sheet covers the whole
   * queue, including the matches the staffer may not reassign (central assignments, played
   * ones), so a played queue has an empty cast here and a complete sheet. A queue with no
   * matches at all is the one case this lets through, and the backend answers it with a
   * plain "No matches have been found for queue = N".
   */
  readonly canExport = computed(() => this.persisted());

  /** Why the Export PDF button is (not) available — shown as its tooltip. */
  readonly exportHint = computed(() =>
    this.canExport()
      ? 'Download the stored assignments of this queue as a PDF — the sheet covers every match in it'
      : 'Save the cast first — the sheet is rendered from the stored assignments'
  );

  readonly drawerMatch = computed<Match | null>(() => {
    const id = this.drawerMatchId();
    if (id == null) return null;
    return this.matches()?.find(m => m.id === id) ?? null;
  });

  // ——— Public actions ———

  incQueue(): void {
    this.queue.update(q => q + 1);
    this.onQueueChanged();
  }

  decQueue(): void {
    if (this.queue() === 1) {
      return;
    }
    this.queue.update(q => q - 1);
    this.onQueueChanged();
  }

  clearLocks(): void {
    this.locks.set(new Map());
  }

  /**
   * Shows what the backend has stored for the selected queue. Called on entry and on every
   * queue change, so the saved cast can be reviewed and exported without regenerating it —
   * which used to be the only way to see one, and destroyed it in the process.
   */
  loadStoredCast(): void {
    this.fetchCast(this.stafferService.getStoredCast(this.queue()), true);
  }

  /**
   * Asks the backend for a fresh cast. The response is a draft — nothing is stored until
   * Save cast — so the table stops matching the database until then.
   */
  generate(): void {
    const locks = Array.from(this.locks(), ([matchId, refereeId]) => ({matchId, refereeId}));
    this.fetchCast(this.stafferService.staffReferees(this.queue(), locks), false);
  }

  private onQueueChanged(): void {
    // Locks reference matches of the previous queue, and so does an open drawer.
    this.clearLocks();
    this.closeDrawer();
    this.loadStoredCast();
  }

  /**
   * Loads a cast — stored or freshly generated — together with everything the table renders
   * around it, and records whether what lands on screen matches the database.
   */
  private fetchCast(cast$: Observable<Match[]>, persisted: boolean): void {
    this.loading.set(true);
    this.savedAt.set(null);
    this.loadFailed.set(false);
    // In flight the table belongs to no queue in particular, so it agrees with nothing —
    // this is what keeps Export PDF from firing at the new queue while the old cast is still
    // on screen.
    this.persisted.set(false);
    const requestedQueue = this.queue();
    forkJoin({
      matches: cast$,
      standings: this.teamService.getStandings(),
      referees: this.refereeService.findRefereesAvailableForQueue(requestedQueue)
    }).subscribe({
      next: ({matches, standings, referees}) => {
        // Guard against a race: the queue stepper may have moved on while this was in
        // flight, and a late response must not overwrite the newer queue's cast. Returning
        // here deliberately leaves `loading` alone — the newer request owns it now.
        if (this.queue() !== requestedQueue) {
          return;
        }
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
        this.persisted.set(persisted);
        this.loading.set(false);
      },
      error: () => {
        // Same race guard: a failure from a queue the user has already left must not clear
        // the newer request's loading state.
        if (this.queue() !== requestedQueue) {
          return;
        }
        this.loadFailed.set(true);
        this.loading.set(false);
      }
    });
  }

  /**
   * Downloads the assignment sheet PDF for the selected queue. Gated on a stored cast
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
    // The swap lives in this component only — the table no longer matches the database.
    this.savedAt.set(null);
    this.persisted.set(false);
  }

  save(): void {
    const ms = this.matches();
    if (!ms || ms.length === 0) return;
    this.matchService.updateList(ms).subscribe(() => {
      this.savedAt.set(new Date());
      this.persisted.set(true);
    });
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
