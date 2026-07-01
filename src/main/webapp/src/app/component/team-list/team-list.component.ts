import {Component, OnInit, computed, inject, signal} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {forkJoin} from 'rxjs';
import {Team} from '../../model/team';
import {Match} from '../../model/match';
import {ModalData} from '../../model/modalData';
import {TeamService} from '../../service/team.service';
import {MatchService} from '../../service/match.service';
import {IconComponent} from '../common/icon/icon.component';
import {TeamPillComponent} from '../common/team-pill/team-pill.component';
import {ConfirmDialogComponent} from '../common/confirm-dialog/confirm-dialog.component';
import {TeamFormComponent} from '../team-form/team-form.component';

/**
 * Team list — searchable league directory sorted by points, with the add/edit drawer
 * and the delete confirm.
 *
 * "Played" is derived client-side from the match list (fixtures with both scores) —
 * the standings endpoint only carries points today; a backend `played` counter is a
 * tracked follow-up.
 */
@Component({
  selector: 'app-team-list',
  templateUrl: './team-list.component.html',
  styleUrls: ['./team-list.component.scss'],
  imports: [IconComponent, TeamPillComponent, ConfirmDialogComponent, TeamFormComponent]
})
export class TeamListComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly teamService = inject(TeamService);
  private readonly matchService = inject(MatchService);

  /** Sorted by points desc — /api/teams/standings returns them in table order. */
  readonly teams = signal<Team[]>([]);
  readonly playedById = signal<Map<number, number>>(new Map());
  readonly searchTerm = signal('');

  /** Add/edit drawer state — the list owns it (repo forms convention, see CLAUDE.md). */
  readonly formOpen = signal(false);
  readonly editingTeam = signal<Team | null>(null);
  readonly deleteTarget = signal<Team | null>(null);

  readonly visibleTeams = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    if (!term) return this.teams();
    return this.teams().filter(t =>
      `${t.name ?? ''} ${t.city ?? ''}`.toLowerCase().includes(term));
  });

  readonly maxPoints = computed(() => {
    const points = this.teams().map(t => t.points ?? 0);
    return Math.max(...points, 1);
  });

  readonly deleteGuard = computed<ModalData>(() => ({
    header: 'Delete team?',
    message: `This will permanently remove ${this.deleteTarget()?.name ?? 'this team'}. This action cannot be undone.`,
    confirmLabel: 'Delete',
    tone: 'danger',
    icon: 'trash'
  }));

  ngOnInit(): void {
    this.load();

    // Deep-link support: /addTeam and /addTeam/:id route here and open the drawer on
    // load, so the legacy form URLs keep working.
    if (this.route.snapshot.url[0]?.path === 'addTeam') {
      const id = Number(this.route.snapshot.paramMap.get('id'));
      if (id) {
        this.teamService.findById(id).subscribe(team => {
          this.editingTeam.set(team);
          this.formOpen.set(true);
        });
      } else {
        this.formOpen.set(true);
      }
    }
  }

  private load(): void {
    forkJoin({
      standings: this.teamService.getStandings(),
      matches: this.matchService.findAll()
    }).subscribe(({standings, matches}) => {
      this.teams.set(standings);
      this.playedById.set(countPlayed(matches));
    });
  }

  setSearch(value: string): void {
    this.searchTerm.set(value);
  }

  played(team: Team): number {
    return this.playedById().get(team.id) ?? 0;
  }

  barPct(team: Team): number {
    return Math.round(((team.points ?? 0) / this.maxPoints()) * 100);
  }

  addTeam(): void {
    this.editingTeam.set(null);
    this.formOpen.set(true);
  }

  editTeam(team: Team): void {
    this.editingTeam.set(team);
    this.formOpen.set(true);
  }

  closeForm(): void {
    this.formOpen.set(false);
    this.editingTeam.set(null);
    // A deep-linked drawer leaves /addTeam in the URL — normalize back to the list.
    if (this.route.snapshot.url[0]?.path === 'addTeam') {
      this.router.navigate(['/teams']);
    }
  }

  onSaved(): void {
    this.load();
    this.closeForm();
  }

  askDelete(team: Team): void {
    this.deleteTarget.set(team);
  }

  confirmDelete(): void {
    const team = this.deleteTarget();
    if (!team) return;
    this.teamService.delete(team.id).subscribe(() => {
      this.teams.update(prev => prev.filter(t => t.id !== team.id));
      this.deleteTarget.set(null);
    });
  }
}

/** Map<teamId, played> — a match counts once it has both scores. */
function countPlayed(matches: Match[]): Map<number, number> {
  const played = new Map<number, number>();
  for (const m of matches) {
    if (m.homeScore == null || m.awayScore == null) continue;
    played.set(m.homeTeamId, (played.get(m.homeTeamId) ?? 0) + 1);
    played.set(m.awayTeamId, (played.get(m.awayTeamId) ?? 0) + 1);
  }
  return played;
}
