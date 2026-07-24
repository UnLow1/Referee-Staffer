import {Component, EventEmitter, Input, OnInit, Output, inject, ChangeDetectionStrategy} from '@angular/core';
import {FormsModule, NgForm} from '@angular/forms';
import {Vacation} from '../../model/vacation';
import {Referee} from '../../model/referee';
import {VacationService} from '../../service/vacation.service';
import {RefereeService} from '../../service/referee.service';
import {FormDrawerComponent} from '../common/form-drawer/form-drawer.component';
import {IconComponent} from '../common/icon/icon.component';

/**
 * Vacation add/edit form — drawer opened from the vacation list.
 *
 * The dates use a reciprocal min/max binding (start ≤ end) on native
 * type="date" inputs; since Angular's min/max validators don't cover dates, the
 * end-after-start rule is enforced via `endBeforeStart` (ISO strings compare
 * lexicographically) and folded into the drawer's `valid` input.
 */
@Component({
  selector: 'app-vacation-form',
  templateUrl: './vacation-form.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [FormsModule, FormDrawerComponent, IconComponent]
})
export class VacationFormComponent implements OnInit {
  private readonly vacationService = inject(VacationService);
  private readonly refereeService = inject(RefereeService);

  /** Vacation being edited, or null to add a new one. */
  @Input() vacation: Vacation | null = null;
  @Output() saved = new EventEmitter<Vacation>();
  @Output() closed = new EventEmitter<void>();

  referees: Referee[] = [];
  model: Pick<Vacation, 'refereeId' | 'startDate' | 'endDate'> = {
    refereeId: undefined as unknown as number,
    startDate: undefined as unknown as Date,
    endDate: undefined as unknown as Date
  };

  get editMode(): boolean {
    return this.vacation != null;
  }

  get endBeforeStart(): boolean {
    const {startDate, endDate} = this.model;
    return !!startDate && !!endDate && endDate < startDate;
  }

  ngOnInit(): void {
    this.refereeService.findAll().subscribe(referees => {
      this.referees = [...referees].sort((a, b) => (a.lastName ?? '').localeCompare(b.lastName ?? ''));
    });
    if (this.vacation) {
      const {refereeId, startDate, endDate} = this.vacation;
      this.model = {refereeId, startDate, endDate};
    }
  }

  onSubmit(form: NgForm): void {
    if (!form.valid || this.endBeforeStart) return;
    const payload: Vacation = {...(this.vacation ?? {} as Vacation), ...this.model};
    const request = this.editMode
      ? this.vacationService.update(payload)
      : this.vacationService.save(payload);
    request.subscribe(saved => this.saved.emit(saved));
  }
}
