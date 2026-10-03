import {Component, EventEmitter, Input, OnInit, Output, inject, ChangeDetectionStrategy} from '@angular/core';
import {FormsModule, NgForm} from '@angular/forms';
import {Team} from '../../model/team';
import {TeamService} from '../../service/team.service';
import {FormDrawerComponent} from '../common/form-drawer/form-drawer.component';
import {IconComponent} from '../common/icon/icon.component';

/**
 * Team add/edit form — drawer opened from the team list. Only `name` and `city` are
 * user-edited; `points` / `short` stay backend-owned. A create posts the draft as-is
 * (`NewTeam` has no `id`), an edit spreads the draft over the original row so the
 * backend-owned fields ride along.
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
    const request = this.team
      ? this.teamService.update({...this.team, ...this.model})
      : this.teamService.save(this.model);
    request.subscribe(saved => this.saved.emit(saved));
  }
}
