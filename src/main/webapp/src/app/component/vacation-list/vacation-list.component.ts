import {Component, OnInit, computed, inject, signal, ChangeDetectionStrategy} from '@angular/core';
import {DatePipe} from '@angular/common';
import {ActivatedRoute, Router} from '@angular/router';
import {Vacation} from '../../model/vacation';
import {Referee} from '../../model/referee';
import {ModalData} from '../../model/modalData';
import {VacationService} from '../../service/vacation.service';
import {RefereeService} from '../../service/referee.service';
import {IconComponent} from '../common/icon/icon.component';
import {RefAvatarComponent} from '../common/ref-avatar/ref-avatar.component';
import {ChipComponent} from '../common/chip/chip.component';
import {ConfirmDialogComponent} from '../common/confirm-dialog/confirm-dialog.component';
import {VacationFormComponent} from '../vacation-form/vacation-form.component';

export type VacationStatus = 'upcoming' | 'active' | 'past';

/**
 * Vacation list — referee unavailability windows with a derived status chip, the
 * add/edit drawer, and the delete confirm.
 *
 * Status is pure client-side: today's date against the window.
 */
@Component({
  selector: 'app-vacation-list',
  templateUrl: './vacation-list.component.html',
  styleUrls: ['./vacation-list.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    DatePipe, IconComponent, RefAvatarComponent, ChipComponent, ConfirmDialogComponent, VacationFormComponent
  ]
})
export class VacationListComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly vacationService = inject(VacationService);
  private readonly refereeService = inject(RefereeService);

  readonly vacations = signal<Vacation[]>([]);
  readonly refereesById = signal<Map<number, Referee>>(new Map());

  /** Add/edit drawer state — the list owns it (repo forms convention, see CLAUDE.md). */
  readonly formOpen = signal(false);
  readonly editingVacation = signal<Vacation | null>(null);
  readonly deleteTarget = signal<Vacation | null>(null);

  readonly sortedVacations = computed(() =>
    [...this.vacations()].sort((a, b) => String(a.startDate).localeCompare(String(b.startDate))));

  readonly deleteGuard = computed<ModalData>(() => {
    const target = this.deleteTarget();
    const referee = target ? this.getReferee(target.refereeId) : undefined;
    const who = referee ? `${referee.firstName} ${referee.lastName}'s vacation` : 'this vacation';
    return {
      header: 'Delete vacation?',
      message: `This will permanently remove ${who}. This action cannot be undone.`,
      confirmLabel: 'Delete',
      tone: 'danger',
      icon: 'trash'
    };
  });

  ngOnInit(): void {
    this.load();

    // Deep-link support: /addVacation and /addVacation/:id route here and open the
    // drawer on load, so the legacy form URLs keep working.
    if (this.route.snapshot.url[0]?.path === 'addVacation') {
      const id = Number(this.route.snapshot.paramMap.get('id'));
      if (id) {
        this.vacationService.findById(id).subscribe(vacation => {
          this.editingVacation.set(vacation);
          this.formOpen.set(true);
        });
      } else {
        this.formOpen.set(true);
      }
    }
  }

  private load(): void {
    this.vacationService.findAll().subscribe(vacations => {
      this.vacations.set(vacations);
      const ids = [...new Set(vacations.map(v => v.refereeId).filter(id => id != null))];
      if (ids.length === 0) {
        this.refereesById.set(new Map());
        return;
      }
      this.refereeService.findByIds(ids).subscribe(referees =>
        this.refereesById.set(new Map(referees.map(r => [r.id, r]))));
    });
  }

  getReferee(refereeId: number | null | undefined): Referee | undefined {
    if (refereeId == null) return undefined;
    return this.refereesById().get(refereeId);
  }

  /** Inclusive day count — a one-day window is 1, not 0. */
  days(vacation: Vacation): number {
    const ms = new Date(vacation.endDate).getTime() - new Date(vacation.startDate).getTime();
    return Math.round(ms / 86_400_000) + 1;
  }

  status(vacation: Vacation): VacationStatus {
    const today = todayIso();
    if (today < String(vacation.startDate)) return 'upcoming';
    if (today > String(vacation.endDate)) return 'past';
    return 'active';
  }

  addVacation(): void {
    this.editingVacation.set(null);
    this.formOpen.set(true);
  }

  editVacation(vacation: Vacation): void {
    this.editingVacation.set(vacation);
    this.formOpen.set(true);
  }

  closeForm(): void {
    this.formOpen.set(false);
    this.editingVacation.set(null);
    // A deep-linked drawer leaves /addVacation in the URL — normalize back to the list.
    if (this.route.snapshot.url[0]?.path === 'addVacation') {
      this.router.navigate(['/vacations']);
    }
  }

  onSaved(): void {
    this.load();
    this.closeForm();
  }

  askDelete(vacation: Vacation): void {
    this.deleteTarget.set(vacation);
  }

  confirmDelete(): void {
    const vacation = this.deleteTarget();
    if (!vacation) return;
    this.vacationService.delete(vacation.id).subscribe(() => {
      this.vacations.update(prev => prev.filter(v => v.id !== vacation.id));
      this.deleteTarget.set(null);
    });
  }
}

/** Local-time today as yyyy-MM-dd — ISO date strings compare lexicographically. */
function todayIso(): string {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}
