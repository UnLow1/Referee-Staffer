import {Component, OnInit, computed, inject, signal} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Referee} from '../../model/referee';
import {RefereeService} from '../../service/referee.service';
import {IconComponent} from '../common/icon/icon.component';
import {RefAvatarComponent} from '../common/ref-avatar/ref-avatar.component';
import {MeterComponent} from '../common/meter/meter.component';
import {RefereeFormComponent} from '../referee-form/referee-form.component';

/**
 * Referee list — searchable directory with row-click navigation to the referee profile.
 *
 * The stat columns (avg grade, potential, last queue) come enriched from the backend
 * (RefereeService.enrichWithStats); they render as `—` when a value is absent so the
 * column shape stays as designed.
 */
@Component({
  selector: 'app-referee-list',
  templateUrl: './referee-list.component.html',
  styleUrl: './referee-list.component.scss',
  imports: [IconComponent, RefAvatarComponent, MeterComponent, RefereeFormComponent]
})
export class RefereeListComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly refereeService = inject(RefereeService);

  readonly referees = signal<Referee[]>([]);
  readonly searchTerm = signal('');

  /** Add/edit drawer state — the list owns it (repo forms convention, see CLAUDE.md). */
  readonly formOpen = signal(false);
  readonly editingReferee = signal<Referee | null>(null);

  readonly visibleReferees = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const list = this.referees();
    // Sort by potential desc — the design's primary ranking. Falls back to experience
    // for referees that haven't been enriched (e.g. fresh data before stats run).
    const sorted = [...list].sort((a, b) => {
      const ap = a.potential ?? a.experience ?? 0;
      const bp = b.potential ?? b.experience ?? 0;
      return bp - ap;
    });
    if (!term) return sorted;
    return sorted.filter(r => {
      const fullName = `${r.firstName ?? ''} ${r.lastName ?? ''}`.toLowerCase();
      const email = (r.email ?? '').toLowerCase();
      return fullName.includes(term) || email.includes(term);
    });
  });

  /** Highest potential in the pool — used to scale the per-row Potential meter. */
  readonly maxPotential = computed(() => {
    const max = Math.max(0, ...this.referees().map(r => r.potential ?? 0));
    return max > 0 ? max : 1;
  });

  ngOnInit(): void {
    this.refereeService.findAll().subscribe(referees => this.referees.set(referees));

    // Deep-link support: /addReferee and /addReferee/:id route here and open the drawer
    // on load, so the legacy form URLs keep working.
    if (this.route.snapshot.url[0]?.path === 'addReferee') {
      const id = Number(this.route.snapshot.paramMap.get('id'));
      if (id) {
        this.refereeService.findById(id).subscribe(referee => {
          this.editingReferee.set(referee);
          this.formOpen.set(true);
        });
      } else {
        this.formOpen.set(true);
      }
    }
  }

  setSearch(value: string): void {
    this.searchTerm.set(value);
  }

  openProfile(referee: Referee): void {
    this.router.navigate(['/referees', referee.id]);
  }

  addReferee(): void {
    this.editingReferee.set(null);
    this.formOpen.set(true);
  }

  editReferee(referee: Referee, event: Event): void {
    event.stopPropagation();
    this.editingReferee.set(referee);
    this.formOpen.set(true);
  }

  closeForm(): void {
    this.formOpen.set(false);
    this.editingReferee.set(null);
    // A deep-linked drawer leaves /addReferee in the URL — normalize back to the list.
    if (this.route.snapshot.url[0]?.path === 'addReferee') {
      this.router.navigate(['/referees']);
    }
  }

  onSaved(): void {
    // Re-fetch instead of patching in place: save responses aren't enriched with the
    // stats columns (avg grade, potential, last queue) the list renders.
    this.refereeService.findAll().subscribe(referees => this.referees.set(referees));
    this.closeForm();
  }

  deleteReferee(referee: Referee, event: Event): void {
    event.stopPropagation();
    this.refereeService.delete(referee.id).subscribe(() => {
      this.referees.update(prev => prev.filter(r => r.id !== referee.id));
    });
  }

  formatGrade(grade: number | undefined | null): string {
    return grade != null ? grade.toFixed(1) : '—';
  }

  hasPotential(referee: Referee): boolean {
    return referee.potential !== null && referee.potential !== undefined;
  }

  potentialPct(referee: Referee): number {
    if (!this.hasPotential(referee)) return 0;
    return Math.round((referee.potential! / this.maxPotential()) * 100);
  }
}
