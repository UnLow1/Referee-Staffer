import {Component, OnInit, computed, inject, signal} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Team} from '../../model/team';
import {Standing} from '../../model/standing';
import {ModalData} from '../../model/modalData';
import {TeamService} from '../../service/team.service';
import {IconComponent} from '../common/icon/icon.component';
import {TeamPillComponent} from '../common/team-pill/team-pill.component';
import {ConfirmDialogComponent} from '../common/confirm-dialog/confirm-dialog.component';
import {TeamFormComponent} from '../team-form/team-form.component';

/**
 * Team list — searchable league directory sorted by points, with the add/edit drawer
 * and the delete confirm.
 *
 * Points and "Played" both come from /api/teams/standings — no client-side counting
 * from the match list anymore.
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

  /** Sorted by points desc — /api/teams/standings returns rows in table order. */
  readonly teams = signal<Standing[]>([]);
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
    this.teamService.getStandings().subscribe(standings => {
      this.teams.set(standings.rows);
    });
  }

  setSearch(value: string): void {
    this.searchTerm.set(value);
  }

  barPct(team: Standing): number {
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
