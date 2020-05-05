import { Component, OnInit } from '@angular/core';
import {TeamService} from "../../service/team.service";
import {Team} from "../../model/team";

@Component({
  selector: 'app-team-list',
  templateUrl: './team-list.component.html',
  styleUrls: ['./team-list.component.css']
})
export class TeamListComponent implements OnInit {

  teams: Team[]

  constructor(private teamService: TeamService) { }

  ngOnInit(): void {
    this.teamService.findAll().subscribe(teams => this.teams = teams)
  }

}
