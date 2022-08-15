import { Component, OnInit } from '@angular/core';
import {Team} from "../../model/team";
import {ActivatedRoute, ParamMap, Router} from "@angular/router";
import {TeamService} from "../../service/team.service";

@Component({
  selector: 'app-team-form',
  templateUrl: './team-form.component.html',
  styleUrls: ['./team-form.component.scss']
})
export class TeamFormComponent implements OnInit {

  team: Team = new Team()
  editMode: boolean

  constructor(private route: ActivatedRoute, private router: Router, private teamService: TeamService) {
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe(
      (params: ParamMap) => {
        let id = Number(params.get('id'))
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
