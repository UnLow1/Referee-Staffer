import {
  Component, ElementRef, Injector, OnInit, afterNextRender, computed, inject, signal, viewChild,
  ChangeDetectionStrategy
} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {forkJoin} from 'rxjs';
import {Match} from '../../model/match';
import {Team} from '../../model/team';
import {Referee} from '../../model/referee';
import {Grade, effectiveGradeValue} from '../../model/grade';
import {ModalData} from '../../model/modalData';
import {MatchService} from '../../service/match.service';
import {TeamService} from '../../service/team.service';
import {RefereeService} from '../../service/referee.service';
import {GradeService} from '../../service/grade.service';
import {IconComponent} from '../common/icon/icon.component';
import {TeamPillComponent} from '../common/team-pill/team-pill.component';
import {RefAvatarComponent} from '../common/ref-avatar/ref-avatar.component';
import {MeterComponent} from '../common/meter/meter.component';
import {ConfirmDialogComponent} from '../common/confirm-dialog/confirm-dialog.component';
import {SegComponent, SegOption} from '../common/seg/seg.component';
import {PaginatorComponent} from '../common/paginator/paginator.component';
import {MatchFormComponent} from '../match-form/match-form.component';

export type ResultFilter = 'all' | 'played' | 'unplayed';
export type RefereeFilter = 'all' | 'assigned' | 'unassigned';

/**
 * Match list — browse fixtures by queue, search by team name, filter by result /
 * referee assignment, edit / delete inline.
 *
 * Data fetch is a single forkJoin so all four dependencies (matches, teams, referees,
 * grades) land together — replaces the previous nested subscribe chain.
 */
@Component({
  selector: 'app-match-list',
  templateUrl: './match-list.component.html',
  styleUrl: './match-list.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    IconComponent, TeamPillComponent, RefAvatarComponent, MeterComponent, ConfirmDialogComponent,
    SegComponent, PaginatorComponent, MatchFormComponent
  ]
})
export class MatchListComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly matchService = inject(MatchService);
  private readonly teamService = inject(TeamService);
  private readonly refereeService = inject(RefereeService);
  private readonly gradeService = inject(GradeService);
  private readonly injector = inject(Injector);

  /** The horizontally scrollable queue bar — see scrollQueueIntoView. */
  private readonly queueBar = viewChild<ElementRef<HTMLElement>>('queueBar');

  readonly matches = signal<Match[]>([]);
  readonly teamsById = signal<Map<number, Team>>(new Map());
  readonly refereesById = signal<Map<number, Referee>>(new Map());
  readonly gradesById = signal<Map<number, Grade>>(new Map());

  /**
   * `undefined` = not initialized yet (load() will default to the latest queue),
   * `null` = the user explicitly picked "All queues" — must survive a reload.
   */
  readonly selectedQueue = signal<number | null | undefined>(undefined);
  readonly searchTerm = signal('');

  /** Requested page (1-based). May point past the end after the list shrinks — see `currentPage`. */
  readonly page = signal(1);
  readonly pageSize = 25;

  /** Filter bar state — toggled by the "Filter" button in the page head. */
  readonly filtersOpen = signal(false);
  readonly resultFilter = signal<ResultFilter>('all');
  readonly refereeFilter = signal<RefereeFilter>('all');

  readonly resultOptions: SegOption<ResultFilter>[] = [
    {value: 'all', label: 'All'},
    {value: 'played', label: 'Played'},
    {value: 'unplayed', label: 'Not played'}
  ];
  readonly refereeOptions: SegOption<RefereeFilter>[] = [
    {value: 'all', label: 'All'},
    {value: 'assigned', label: 'Assigned'},
    {value: 'unassigned', label: 'Unassigned'}
  ];

  readonly activeFilterCount = computed(() =>
    (this.resultFilter() === 'all' ? 0 : 1) + (this.refereeFilter() === 'all' ? 0 : 1));

  /** Add/edit drawer state — the list owns it (repo forms convention, see CLAUDE.md). */
  readonly formOpen = signal(false);
  readonly editingMatch = signal<Match | null>(null);
  readonly deleteTarget = signal<Match | null>(null);

  readonly deleteGuard = computed<ModalData>(() => ({
    header: 'Delete match?',
    message: `This will permanently remove ${this.deleteTargetLabel()}. This action cannot be undone.`,
    confirmLabel: 'Delete',
    tone: 'danger',
    icon: 'trash'
  }));

  readonly deleteTargetLabel = computed(() => {
    const match = this.deleteTarget();
    if (!match) return 'this match';
    const home = this.getTeam(match.homeTeamId)?.name ?? '?';
    const away = this.getTeam(match.awayTeamId)?.name ?? '?';
    return `${home} – ${away}`;
  });

  /** All queues that have at least one match, sorted desc so the latest is first. */
  readonly availableQueues = computed(() => {
    const set = new Set<number>();
    this.matches().forEach(m => set.add(m.queue));
    return [...set].sort((a, b) => b - a);
  });

  readonly visibleMatches = computed(() => {
    const queue = this.selectedQueue();
    const term = this.searchTerm().trim().toLowerCase();
    const result = this.resultFilter();
    const referee = this.refereeFilter();
    return this.matches()
      .filter(m => queue == null || m.queue === queue)
      .filter(m => matchesResult(m, result))
      .filter(m => matchesReferee(m, referee))
      .filter(m => {
        if (!term) return true;
        const home = this.getTeam(m.homeTeamId)?.name?.toLowerCase() ?? '';
        const away = this.getTeam(m.awayTeamId)?.name?.toLowerCase() ?? '';
        return home.includes(term) || away.includes(term);
      });
  });

  /** Requested page clamped to the filtered list, so a shrink (delete, filter) can't strand us on an empty page. */
  readonly currentPage = computed(() => {
    const totalPages = Math.max(1, Math.ceil(this.visibleMatches().length / this.pageSize));
    return Math.min(this.page(), totalPages);
  });

  readonly pagedMatches = computed(() => {
    const start = (this.currentPage() - 1) * this.pageSize;
    return this.visibleMatches().slice(start, start + this.pageSize);
  });

  ngOnInit(): void {
    this.load();

    // Deep-link support: /addMatch and /addMatch/:id route here and open the drawer on
    // load, so the legacy form URLs keep working.
    if (this.route.snapshot.url[0]?.path === 'addMatch') {
      const id = Number(this.route.snapshot.paramMap.get('id'));
      if (id) {
        this.matchService.findById(id).subscribe(match => {
          this.editingMatch.set(match);
          this.formOpen.set(true);
        });
      } else {
        this.formOpen.set(true);
      }
    }
  }

  private load(): void {
    this.matchService.findAll().subscribe(matches => {
      const teamIds = unique(matches.flatMap(m => [m.homeTeamId, m.awayTeamId]));
      const refereeIds = unique(matches.map(m => m.refereeId).filter(notEmpty));
      const gradeIds = unique(matches.map(m => m.gradeId).filter(notEmpty));

      forkJoin({
        teams: this.teamService.findByIds(teamIds),
        referees: refereeIds.length > 0 ? this.refereeService.findByIds(refereeIds) : Promise.resolve([] as Referee[]),
        grades: gradeIds.length > 0 ? this.gradeService.findByIds(gradeIds) : Promise.resolve([] as Grade[])
      }).subscribe(({teams, referees, grades}) => {
        this.matches.set(matches);
        this.teamsById.set(toMap(teams));
        this.refereesById.set(toMap(referees));
        this.gradesById.set(toMap(grades));

        // Default to the latest queue with matches so the user lands on something.
        // Only on first load — an explicit "All queues" choice (null) must stick.
        const queues = this.availableQueues();
        if (queues.length > 0 && this.selectedQueue() === undefined) {
          this.selectedQueue.set(queues[0]);
        }
        this.scrollSelectedQueueIntoView();
      });
    });
  }

  selectQueue(q: number | null): void {
    this.selectedQueue.set(q);
    this.page.set(1);
    this.scrollSelectedQueueIntoView();
  }

  /**
   * Keeps the selected queue's button reachable inside the scrollable bar (RS-113).
   * The first load lands on the newest queue, whose button sits at the head of the bar
   * and is visible anyway; this earns its keep when the selection is mid-season and the
   * list re-renders around it — a save reloads through `load()`, and `selectQueue` can
   * be reached from a partly-scrolled bar.
   *
   * Deferred to the next render because on first load the buttons do not exist yet:
   * the queue list is derived from matches that have only just arrived.
   */
  private scrollSelectedQueueIntoView(): void {
    afterNextRender(() => {
      const bar = this.queueBar()?.nativeElement;
      if (bar) {
        scrollQueueIntoView(bar, this.selectedQueue() ?? null);
      }
    }, {injector: this.injector});
  }

  setSearch(value: string): void {
    this.searchTerm.set(value);
    this.page.set(1);
  }

  setPage(page: number): void {
    this.page.set(page);
  }

  toggleFilters(): void {
    this.filtersOpen.update(open => !open);
  }

  setResultFilter(filter: ResultFilter): void {
    this.resultFilter.set(filter);
    this.page.set(1);
  }

  setRefereeFilter(filter: RefereeFilter): void {
    this.refereeFilter.set(filter);
    this.page.set(1);
  }

  clearFilters(): void {
    this.resultFilter.set('all');
    this.refereeFilter.set('all');
    this.page.set(1);
  }

  openDetail(match: Match): void {
    this.router.navigate(['/matches', match.id]);
  }

  addMatch(): void {
    this.editingMatch.set(null);
    this.formOpen.set(true);
  }

  editMatch(match: Match, event: Event): void {
    event.stopPropagation();
    this.editingMatch.set(match);
    this.formOpen.set(true);
  }

  closeForm(): void {
    this.formOpen.set(false);
    this.editingMatch.set(null);
    // A deep-linked drawer leaves /addMatch in the URL — normalize back to the list.
    if (this.route.snapshot.url[0]?.path === 'addMatch') {
      this.router.navigate(['/matches']);
    }
  }

  onSaved(): void {
    // Re-fetch the full join: a save can touch teams/referees/grades the list joins on.
    this.load();
    this.closeForm();
  }

  askDelete(match: Match, event: Event): void {
    event.stopPropagation();
    this.deleteTarget.set(match);
  }

  confirmDelete(): void {
    const match = this.deleteTarget();
    if (!match) return;
    this.matchService.delete(match.id).subscribe(() => {
      this.matches.update(prev => prev.filter(m => m.id !== match.id));
      this.deleteTarget.set(null);
    });
  }

  // ——— Helpers ———

  getTeam(teamId: number | undefined): Team | undefined {
    if (teamId == null) return undefined;
    return this.teamsById().get(teamId);
  }

  getReferee(refereeId: number | null | undefined): Referee | undefined {
    if (refereeId == null) return undefined;
    return this.refereesById().get(refereeId);
  }

  getGrade(gradeId: number | null | undefined): Grade | undefined {
    if (gradeId == null) return undefined;
    return this.gradesById().get(gradeId);
  }

  scoreOrPlaceholder(match: Match): string | null {
    if (match.homeScore == null || match.awayScore == null) return null;
    return `${match.homeScore} – ${match.awayScore}`;
  }

  /** Grade values are 1..10 — scale to 0..100 for the Meter atom (max=100). */
  gradeAsMeter(grade: Grade | undefined): number {
    return (grade ? effectiveGradeValue(grade) : 0) * 10;
  }
}

/** A match counts as played once both halves of the score are recorded. */
function isPlayed(match: Match): boolean {
  return match.homeScore != null && match.awayScore != null;
}

function matchesResult(match: Match, filter: ResultFilter): boolean {
  if (filter === 'all') return true;
  return filter === 'played' ? isPlayed(match) : !isPlayed(match);
}

function matchesReferee(match: Match, filter: RefereeFilter): boolean {
  if (filter === 'all') return true;
  const assigned = match.refereeId != null;
  return filter === 'assigned' ? assigned : !assigned;
}

/**
 * Centers `queue`'s button inside the scrollable queue bar — but only when it is not
 * already fully visible. Re-centering on every click would yank the button the user
 * just clicked out from under the cursor, and with 31 queues that is a jump of several
 * hundred pixels, so a second click on the same spot would hit a different queue.
 *
 * Positions come from `getBoundingClientRect`, deliberately not from `offsetLeft`:
 * `offsetLeft` is measured from the nearest positioned ancestor, and nothing in this
 * tree is positioned, so it would report the button's position on the page and feed
 * the bar's own X (sidebar + panel padding) into the math as a constant error.
 */
export function scrollQueueIntoView(bar: HTMLElement, queue: number | null): void {
  const wanted = queue == null ? 'all' : String(queue);
  const button = [...bar.querySelectorAll<HTMLElement>('[data-queue]')]
    .find(b => b.getAttribute('data-queue') === wanted);
  if (!button) return;

  const barRect = bar.getBoundingClientRect();
  const buttonRect = button.getBoundingClientRect();
  // Rects are viewport-relative and move with the scroll, so add it back to get the
  // button's offset within the bar's content.
  const left = buttonRect.left - barRect.left + bar.scrollLeft;
  const right = left + buttonRect.width;
  if (left >= bar.scrollLeft && right <= bar.scrollLeft + bar.clientWidth) return;

  bar.scrollLeft = centerScrollLeft(left, buttonRect.width, bar.clientWidth, bar.scrollWidth);
}

/**
 * Scroll offset that centers an item of `itemWidth` at `itemOffset` inside a viewport
 * of `viewportWidth`, clamped to the scrollable range. A bar narrower than its viewport
 * (`scrollWidth <= viewportWidth`) has nothing to scroll and yields 0.
 */
export function centerScrollLeft(
  itemOffset: number, itemWidth: number, viewportWidth: number, scrollWidth: number
): number {
  const centered = itemOffset + itemWidth / 2 - viewportWidth / 2;
  return Math.max(0, Math.min(centered, scrollWidth - viewportWidth));
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
