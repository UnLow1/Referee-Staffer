import { Component, OnInit, inject } from '@angular/core';
import {Team} from "../../model/team";
import {ActivatedRoute, ParamMap, Router} from "@angular/router";
import {TeamService} from "../../service/team.service";
import { FormsModule } from '@angular/forms';

@Component({
    selector: 'app-team-form',
    templateUrl: './team-form.component.html',
    styleUrls: ['./team-form.component.scss'],
    imports: [FormsModule]
})
export class TeamFormComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private teamService = inject(TeamService);


  team: Team = new Team()
  editMode: boolean

  ngOnInit(): void {
    this.route.paramMap.subscribe(
      (params: ParamMap) => {
        const id = Number(params.get('id'))
        if (id) {
          this.editMode = true
          this.teamService.findById(id).subscribe(team => this.team = team)
        }
      }
    )
  }

  onSubmit() {
    if (this.editMode)
      this.teamService.update(this.team).subscribe(() => this.gotoTeamsList())
    else
      this.teamService.save(this.team).subscribe(() => this.gotoTeamsList())
  }

  gotoTeamsList() {
    this.router.navigate(['teams'])
  }
}
