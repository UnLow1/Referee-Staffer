import { Component, OnInit } from '@angular/core';
import {Team} from "../../model/team";
import {ActivatedRoute, Router} from "@angular/router";
import {TeamService} from "../../service/team.service";

@Component({
  selector: 'app-team-form',
  templateUrl: './team-form.component.html',
  styleUrls: ['./team-form.component.css']
})
export class TeamFormComponent implements OnInit {

  team: Team

  constructor(private route: ActivatedRoute, private router: Router, private teamService: TeamService) {
    this.team = new Team()
  }

  ngOnInit(): void {
  }

  onSubmit() {
    this.teamService.save(this.team).subscribe(() => this.gotoTeamsList())
  }

  gotoTeamsList() {
    this.router.navigate(['/teams'])
  }
}
