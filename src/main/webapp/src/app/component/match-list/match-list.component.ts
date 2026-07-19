import {Component, OnInit, computed, inject, signal, ChangeDetectionStrategy} from '@angular/core';
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
import {MatchFormComponent} from '../match-form/match-form.component';

/**
 * Match list — browse fixtures by queue, search by team name, edit / delete inline.
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
    MatchFormComponent
  ]
})
export class MatchListComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly matchService = inject(MatchService);
  private readonly teamService = inject(TeamService);
  private readonly refereeService = inject(RefereeService);
  private readonly gradeService = inject(GradeService);

  readonly matches = signal<Match[]>([]);
  readonly teamsById = signal<Map<number, Team>>(new Map());
  readonly refereesById = signal<Map<number, Referee>>(new Map());
  readonly gradesById = signal<Map<number, Grade>>(new Map());

  readonly selectedQueue = signal<number | null>(null);
  readonly searchTerm = signal('');

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
    return this.matches()
      .filter(m => queue == null || m.queue === queue)
      .filter(m => {
        if (!term) return true;
        const home = this.getTeam(m.homeTeamId)?.name?.toLowerCase() ?? '';
        const away = this.getTeam(m.awayTeamId)?.name?.toLowerCase() ?? '';
        return home.includes(term) || away.includes(term);
      });
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
        const queues = this.availableQueues();
        if (queues.length > 0 && this.selectedQueue() == null) {
          this.selectedQueue.set(queues[0]);
        }
      });
    });
  }

  selectQueue(q: number): void {
    this.selectedQueue.set(q);
  }

  setSearch(value: string): void {
    this.searchTerm.set(value);
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

function unique<T>(arr: T[]): T[] {
  return [...new Set(arr)];
}

function notEmpty<T>(v: T | null | undefined): v is T {
  return v != null;
}

function toMap<T extends {id: number}>(items: T[]): Map<number, T> {
  return new Map(items.map(t => [t.id, t]));
}
