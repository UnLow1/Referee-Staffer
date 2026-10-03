import {Component, EventEmitter, Input, OnInit, Output, inject, signal, ChangeDetectionStrategy} from '@angular/core';
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
 * It keys off explicit null checks rather than truthiness, so that a grade of `0` counts
 * as an entered value instead of an empty input.
 *
 * The stored grade is fetched asynchronously, so the grade section is gated on that
 * request: whether the match already has a grade is decided from `match.gradeId` (known
 * up front) rather than from the half-loaded `existingGrade`, and the inputs stay disabled
 * until the grade is on screen. Without the gate a save that beats the response created
 * a second grade for the same match instead of updating the stored one.
 *
 * Every field filled from HTTP after the drawer is already on screen lives in a signal,
 * so the write itself marks the view dirty (RS-116). That covers `teams`, `referees`,
 * `grade`, `existingGrade` and the two grade-load flags. `grade` is a signal used as a
 * box: `set()` is what the async load needs, while the two-way `[(ngModel)]` bindings keep
 * mutating the object in place — those edits come from DOM events, which schedule change
 * detection on their own. `model` stays a plain object: it is written synchronously from
 * the @Input before the first render, so it never races the view.
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

  readonly teams = signal<Team[]>([]);
  readonly referees = signal<Referee[]>([]);
  /** Draft of the editable fields — `id` and `gradeId` stay on the match row, not here. */
  model: Partial<Pick<Match,
    'queue' | 'date' | 'homeTeamId' | 'awayTeamId' | 'refereeId' | 'homeScore' | 'awayScore'>> = {};
  /** Grade draft; an empty `value` means "no grade" and drives the delete branch. */
  readonly grade = signal<Partial<Pick<Grade, 'value' | 'secondValue'>>>({});
  /** The grade the edited match already had, loaded in ngOnInit. Null when it had none. */
  readonly existingGrade = signal<Grade | null>(null);

  /** The stored grade is still in flight — the grade inputs and submit are disabled. */
  readonly gradeLoading = signal(false);
  /**
   * Fetching the stored grade failed (the interceptor already showed the toast). The
   * grade section stays disabled and onSubmit skips the grade branch entirely, so the
   * rest of the form can still be saved without touching a grade we never read.
   */
  readonly gradeLoadFailed = signal(false);

  get editMode(): boolean {
    return this.match != null;
  }

  get subtitle(): string {
    return this.editMode ? 'Update fixture, result, or assignment' : 'Schedule a new fixture';
  }

  ngOnInit(): void {
    this.teamService.findAll().subscribe(teams => this.teams.set(teams));
    this.refereeService.findAll().subscribe(referees => this.referees.set(referees));
    if (this.match) {
      const {queue, date, homeTeamId, awayTeamId, refereeId, homeScore, awayScore} = this.match;
      this.model = {queue, date, homeTeamId, awayTeamId, refereeId, homeScore, awayScore};
      if (this.match.gradeId != null) {
        this.gradeLoading.set(true);
        this.gradeService.findById(this.match.gradeId).subscribe({
          next: grade => {
            this.existingGrade.set(grade);
            this.grade.set({value: grade.value, secondValue: grade.secondValue});
            this.gradeLoading.set(false);
          },
          error: () => {
            this.gradeLoading.set(false);
            this.gradeLoadFailed.set(true);
          }
        });
      }
    }
  }

  onSubmit(form: NgForm): void {
    if (!form.valid || !this.canSubmit) return;
    // A cleared datetime-local input binds "" rather than null; send an explicit null so
    // the backend stores "not scheduled yet" instead of failing to parse an empty string.
    if (!this.model.date) this.model.date = null;
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
   * mode the match has no stored grade, so only the create branch can be taken there. A
   * grade that could not be loaded is left alone — the form never saw its stored value.
   */
  private persistGrade(match: Match): void {
    const {value, secondValue} = this.grade();
    const existing = this.existingGrade();
    const emit = (): void => this.saved.emit(match);
    if (!this.gradeEditable)
      emit();
    else if (this.hasStoredGrade && existing && value != null)
      this.gradeService.update({...existing, value, secondValue}).subscribe(emit);
    else if (this.hasStoredGrade && existing)
      this.gradeService.delete(existing).subscribe(emit);
    else if (value != null)
      this.gradeService.save(match, {value, secondValue}).subscribe(emit);
    else
      emit();
  }

  /** The grade on screen is the stored one, so the grade inputs may be edited and saved. */
  get gradeEditable(): boolean {
    return !this.gradeLoading() && !this.gradeLoadFailed();
  }

  /**
   * Saving is held back only while the grade is loading — a save landing in that window
   * would either create a duplicate grade or read the empty input as "grade cleared" and
   * delete the stored one. A failed load does not block the save: the grade is skipped.
   */
  get canSubmit(): boolean {
    return !this.gradeLoading();
  }

  /** Gates the second grade input — a split grade needs its first component first. */
  get gradeValueMissing(): boolean {
    return this.grade().value == null;
  }

  onGradeValueChange(value: number | null): void {
    // A split grade can't consist of the second component alone — clearing the first
    // part drops the second one too (the input is disabled in that state anyway).
    if (value == null) this.grade().secondValue = undefined;
  }

  /**
   * The match already has a grade row on the backend, so it can be updated or deleted.
   * Read from the match rather than from `existingGrade`: the match carries the reference
   * from the moment the form opens, while `existingGrade` is empty until findById answers.
   */
  private get hasStoredGrade(): boolean {
    return this.match?.gradeId != null;
  }
}
