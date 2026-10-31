import {Component, EventEmitter, Input, OnInit, Output, inject, ChangeDetectionStrategy} from '@angular/core';
import {FormsModule, NgForm} from '@angular/forms';
import {NewReferee, Referee} from '../../model/referee';
import {RefereeService} from '../../service/referee.service';
import {FormDrawerComponent} from '../common/form-drawer/form-drawer.component';
import {IconComponent} from '../common/icon/icon.component';

/**
 * Referee add/edit form — a right-side drawer opened from the referee list.
 *
 * The host renders this component behind an @if, so it is recreated per open and the
 * working copy can be taken once in ngOnInit. Editable fields only — the enriched stats
 * (averageGrade, potential, lastQueue, homeWins, awayWins) never appear in the form;
 * they survive an edit because the draft is spread over the original referee.
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

  /** Draft of the editable fields — `experience` starts out unset, so all four are optional. */
  model: Partial<Pick<Referee, 'firstName' | 'lastName' | 'email' | 'experience'>> = {
    firstName: '',
    lastName: '',
    email: ''
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
    const payload = this.toPayload();
    if (!payload) return;
    const request = this.referee
      ? this.refereeService.update({...this.referee, ...payload})
      : this.refereeService.save(payload);
    request.subscribe(saved => this.saved.emit(saved));
  }

  /**
   * Narrows the draft into a create payload. All four inputs are `required` in the
   * template, so a valid form always filled them — this re-proves that to the compiler
   * instead of casting an incomplete draft to a full `Referee`. A `null` result therefore
   * means the validators were bypassed, not that the user left something out — the drawer
   * keeps submit disabled until then.
   */
  private toPayload(): NewReferee | null {
    const {firstName, lastName, email, experience} = this.model;
    if (firstName == null || lastName == null || email == null || experience == null) return null;
    return {firstName, lastName, email, experience};
  }
}
