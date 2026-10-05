import {Component, EventEmitter, Input, OnInit, Output, inject, ChangeDetectionStrategy} from '@angular/core';
import {FormsModule, NgForm} from '@angular/forms';
import {Match} from '../../model/match';
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
 * The grade branch in onSubmit reflects that `Match` references the grade by `gradeId`
 * while the form edits a separate `grade.value`; the save/update/delete decision tree
 * must stay intact. It keys off explicit null checks rather than truthiness, so that a
 * grade of `0` counts as an entered value instead of an empty input.
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
  model: Match = {} as Match;
  grade: Grade = {} as Grade;

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
      this.model = {...this.match};
      if (this.match.gradeId != null) {
        this.gradeService.findById(this.match.gradeId).subscribe(grade => this.grade = grade);
      }
    }
  }

  onSubmit(form: NgForm): void {
    if (!form.valid) return;
    if (this.editMode)
      this.matchService.update(this.model).subscribe(match => {
        if (this.hasStoredGrade && this.hasGradeValue)
          this.gradeService.update(this.grade).subscribe(() => this.saved.emit(match));
        else if (this.hasGradeValue)
          this.gradeService.save(match, this.grade).subscribe(() => this.saved.emit(match));
        else if (this.hasStoredGrade)
          this.gradeService.delete(this.grade).subscribe(() => this.saved.emit(match));
        else
          this.saved.emit(match);
      });
    else
      this.matchService.save(this.model).subscribe(match => {
        if (this.hasGradeValue)
          this.gradeService.save(match, this.grade).subscribe(() => this.saved.emit(match));
        else
          this.saved.emit(match);
      });
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

  /** The match already has a grade row on the backend, so it can be updated or deleted. */
  private get hasStoredGrade(): boolean {
    return this.grade.id != null;
  }

  /**
   * The form carries a grade value. Explicitly a null check, not a truthiness test:
   * `0` is a value like any other, and treating it as "nothing entered" would send an
   * edit from 7.5 to 0 down the delete branch instead of the update one.
   */
  private get hasGradeValue(): boolean {
    return this.grade.value != null;
  }
}
