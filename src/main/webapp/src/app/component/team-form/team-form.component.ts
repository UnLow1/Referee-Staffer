import {Component, EventEmitter, Input, OnInit, Output, inject, ChangeDetectionStrategy} from '@angular/core';
import {FormsModule, NgForm} from '@angular/forms';
import {Team} from '../../model/team';
import {TeamService} from '../../service/team.service';
import {FormDrawerComponent} from '../common/form-drawer/form-drawer.component';
import {IconComponent} from '../common/icon/icon.component';

/**
 * Team add/edit form — drawer opened from the team list. Only `name` and `city` are
 * user-edited; `points` / `short` stay backend-owned and ride along via the spread
 * on submit.
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

  model: Pick<Team, 'name' | 'city'> = {name: '', city: ''};

  get editMode(): boolean {
    return this.team != null;
  }

  get subtitle(): string {
    return this.editMode ? this.team!.name : 'New club in the league';
  }

  ngOnInit(): void {
    if (this.team) {
      this.model = {name: this.team.name, city: this.team.city};
    }
  }

  onSubmit(form: NgForm): void {
    if (!form.valid) return;
    const payload: Team = {...(this.team ?? {} as Team), ...this.model};
    const request = this.editMode
      ? this.teamService.update(payload)
      : this.teamService.save(payload);
    request.subscribe(saved => this.saved.emit(saved));
  }
}
