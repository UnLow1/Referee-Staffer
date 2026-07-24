import {Component, EventEmitter, Input, OnInit, Output, inject, ChangeDetectionStrategy} from '@angular/core';
import {FormsModule, NgForm} from '@angular/forms';
import {Referee} from '../../model/referee';
import {RefereeService} from '../../service/referee.service';
import {FormDrawerComponent} from '../common/form-drawer/form-drawer.component';
import {IconComponent} from '../common/icon/icon.component';

/**
 * Referee add/edit form — a right-side drawer opened from the referee list.
 *
 * The host renders this component behind an @if, so it is recreated per open and the
 * working copy can be taken once in ngOnInit. Editable fields only — the enriched stats
 * (averageGrade, potential, lastQueue, homeWins, awayWins) never appear in the form;
 * they survive an edit because the original referee is spread into the payload.
 */
@Component({
  selector: 'app-referee-form',
  templateUrl: './referee-form.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [FormsModule, FormDrawerComponent, IconComponent]
})
export class RefereeFormComponent implements OnInit {
  private readonly refereeService = inject(RefereeService);

  /** Referee being edited, or null to add a new one. */
  @Input() referee: Referee | null = null;
  @Output() saved = new EventEmitter<Referee>();
  @Output() closed = new EventEmitter<void>();

  // README §Validation rules: Angular's `email` validator accepts addresses without a
  // TLD dot, so a stricter regex is enforced via `pattern`.
  readonly emailPattern = '^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$';

  model: Pick<Referee, 'firstName' | 'lastName' | 'email' | 'experience'> = {
    firstName: '',
    lastName: '',
    email: '',
    experience: undefined as unknown as number
  };

  get editMode(): boolean {
    return this.referee != null;
  }

  get subtitle(): string {
    return this.editMode
      ? `${this.referee!.firstName} ${this.referee!.lastName}`
      : 'New official in the pool';
  }

  ngOnInit(): void {
    if (this.referee) {
      const {firstName, lastName, email, experience} = this.referee;
      this.model = {firstName, lastName, email, experience};
    }
  }

  onSubmit(form: NgForm): void {
    if (!form.valid) return;
    const payload: Referee = {...(this.referee ?? {} as Referee), ...this.model};
    const request = this.editMode
      ? this.refereeService.update(payload)
      : this.refereeService.save(payload);
    request.subscribe(saved => this.saved.emit(saved));
  }
}
