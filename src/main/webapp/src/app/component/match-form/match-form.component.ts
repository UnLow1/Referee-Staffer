import {Component, EventEmitter, Input, OnInit, Output, inject, ChangeDetectionStrategy} from '@angular/core';
import {FormsModule, NgForm} from '@angular/forms';
import {Match, NewMatch} from '../../model/match';
import {Team} from '../../model/team';
import {Referee} from '../../model/referee';
import {Grade} from '../../model/grade';
import {MatchService} from '../../service/match.service';
import {TeamService} from '../../service/team.service';
import {RefereeService} from '../../service/referee.service';
import {GradeService} from '../../service/grade.service';
import {ExcludeValuePipe} from '../../pipe/exclude-value.pipe';
import {FormDrawerComponent} from '../common/form-drawer/form-drawer.component';
import {IconComponent} from '../common/icon/icon.component';

/**
 * Match add/edit form — the `wide` (560px) drawer opened from the match list: queue +
 * date, a "Fixture" section with the home/away vs-split (selects exclude each other via
 * the excludeValue pipe), and a "Result & assignment" section.
 *
 * `persistGrade` reflects that `Match` references the grade by `gradeId` while the form
 * edits a separate `grade.value`; the save/update/delete decision tree must stay intact.
 */
@Component({
  selector: 'app-match-form',
  templateUrl: './match-form.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [FormsModule, ExcludeValuePipe, FormDrawerComponent, IconComponent]
})
export class MatchFormComponent implements OnInit {
  private readonly matchService = inject(MatchService);
  private readonly teamService = inject(TeamService);
  private readonly refereeService = inject(RefereeService);
  private readonly gradeService = inject(GradeService);

  /** Match being edited, or null to add a new one. */
  @Input() match: Match | null = null;
  @Output() saved = new EventEmitter<Match>();
  @Output() closed = new EventEmitter<void>();

  teams: Team[] = [];
  referees: Referee[] = [];
  /** Draft of the editable fields — `id` and `gradeId` stay on the match row, not here. */
  model: Partial<Pick<Match,
    'queue' | 'date' | 'homeTeamId' | 'awayTeamId' | 'refereeId' | 'homeScore' | 'awayScore'>> = {};
  /** Grade draft; an empty `value` means "no grade" and drives the delete branch. */
  grade: Partial<Pick<Grade, 'value' | 'secondValue'>> = {};
  /** The grade the edited match already had, loaded in ngOnInit. Null when it had none. */
  existingGrade: Grade | null = null;

  get editMode(): boolean {
    return this.match != null;
  }

  get subtitle(): string {
    return this.editMode ? 'Update fixture, result, or assignment' : 'Schedule a new fixture';
  }

  ngOnInit(): void {
    this.teamService.findAll().subscribe(teams => this.teams = teams);
    this.refereeService.findAll().subscribe(referees => this.referees = referees);
    if (this.match) {
      const {queue, date, homeTeamId, awayTeamId, refereeId, homeScore, awayScore} = this.match;
      this.model = {queue, date, homeTeamId, awayTeamId, refereeId, homeScore, awayScore};
      if (this.match.gradeId) {
        this.gradeService.findById(this.match.gradeId).subscribe(grade => {
          this.existingGrade = grade;
          this.grade = {value: grade.value, secondValue: grade.secondValue};
        });
      }
    }
  }

  onSubmit(form: NgForm): void {
    if (!form.valid) return;
    const payload = this.toPayload();
    if (!payload) return;
    const match = this.match;
    const request = match
      ? this.matchService.update({...match, ...payload})
      : this.matchService.save(payload);
    request.subscribe(saved => this.persistGrade(saved));
  }

  /**
   * Narrows the draft into a create payload. Queue and both teams are `required` in the
   * template (and unconditionally `@NotNull` on MatchDto), so a valid form always filled
   * them — this re-proves that to the compiler instead of casting an incomplete draft to
   * a full `Match`. A `null` result therefore means the validators were bypassed, not
   * that the user left something out — the drawer keeps submit disabled until then.
   */
  private toPayload(): NewMatch | null {
    const {queue, homeTeamId, awayTeamId} = this.model;
    if (queue == null || homeTeamId == null || awayTeamId == null) return null;
    return {...this.model, queue, homeTeamId, awayTeamId};
  }

  /**
   * Grade side of the submit, run once the match round-trip returned: create, update or
   * delete the grade depending on what the form holds, and only then emit `saved`. In add
   * mode `existingGrade` is always null, so only the create branch can be taken there.
   */
  private persistGrade(match: Match): void {
    const {value, secondValue} = this.grade;
    const existing = this.existingGrade;
    const emit = (): void => this.saved.emit(match);
    if (existing && value)
      this.gradeService.update({...existing, value, secondValue}).subscribe(emit);
    else if (existing)
      this.gradeService.delete(existing).subscribe(emit);
    else if (value)
      this.gradeService.save(match, {value, secondValue}).subscribe(emit);
    else
      emit();
  }

  /** Gates the second grade input — a split grade needs its first component first. */
  get gradeValueMissing(): boolean {
    return this.grade.value == null;
  }

  onGradeValueChange(value: number | null): void {
    // A split grade can't consist of the second component alone — clearing the first
    // part drops the second one too (the input is disabled in that state anyway).
    if (value == null) this.grade.secondValue = undefined;
  }
}
