import {Component, EventEmitter, Input, OnInit, Output, inject, ChangeDetectionStrategy} from '@angular/core';
import {FormsModule, NgForm} from '@angular/forms';
import {deriveShortCode, SHORT_CODE_MAX_LENGTH, SHORT_CODE_PATTERN, Team} from '../../model/team';
import {TeamService} from '../../service/team.service';
import {FormDrawerComponent} from '../common/form-drawer/form-drawer.component';
import {IconComponent} from '../common/icon/icon.component';

/**
 * Team add/edit form — drawer opened from the team list. `name`, `city` and the optional
 * short-code override are user-edited; `points` and the computed `short` stay backend-owned
 * and ride along via the spread on submit.
 *
 * The short-code field writes `shortOverride`, not `short`: the latter is a computed read
 * model (the backend derives it from the name when no override is stored), so submitting it
 * would freeze the code against future renames. Leaving the field empty submits null, which
 * clears any override and hands the code back to the name-derived default.
 *
 * Rendered behind an @if by the host, so ngOnInit sees the final input.
 */
@Component({
  selector: 'app-team-form',
  templateUrl: './team-form.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [FormsModule, FormDrawerComponent, IconComponent]
})
export class TeamFormComponent implements OnInit {
  private readonly teamService = inject(TeamService);

  /** Team being edited, or null to add a new one. */
  @Input() team: Team | null = null;
  @Output() saved = new EventEmitter<Team>();
  @Output() closed = new EventEmitter<void>();

  model: {name: string; city: string; shortOverride: string} = {name: '', city: '', shortOverride: ''};

  get editMode(): boolean {
    return this.team != null;
  }

  /**
   * Bound to the input's `[pattern]` as a RegExp rather than a string: Angular compiles a
   * string pattern without the `u` flag, which would turn `\p{L}` into an identity escape
   * and mark every real code invalid.
   */
  readonly shortCodePattern = SHORT_CODE_PATTERN;
  readonly shortCodeMaxLength = SHORT_CODE_MAX_LENGTH;

  /** The code the pill renders today — shown as the placeholder so the default is visible. */
  get derivedShort(): string {
    return deriveShortCode(this.model.name);
  }

  get subtitle(): string {
    return this.editMode ? this.team!.name : 'New club in the league';
  }

  ngOnInit(): void {
    if (this.team) {
      this.model = {
        name: this.team.name,
        city: this.team.city,
        shortOverride: this.team.shortOverride ?? ''
      };
    }
  }

  onSubmit(form: NgForm): void {
    if (!form.valid) return;
    // Blank means "no override" — send null rather than '' so the backend clears the column
    // instead of storing an empty string.
    const shortOverride = this.model.shortOverride.trim().toUpperCase() || null;
    const payload: Team = {...(this.team ?? {} as Team), ...this.model, shortOverride};
    const request = this.editMode
      ? this.teamService.update(payload)
      : this.teamService.save(payload);
    request.subscribe(saved => this.saved.emit(saved));
  }
}
