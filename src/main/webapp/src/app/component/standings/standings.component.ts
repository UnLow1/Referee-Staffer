import {Component, OnInit} from '@angular/core';
import {TeamService} from "../../service/team.service";
import {Team} from "../../model/team";

@Component({
  selector: 'app-standings',
  templateUrl: './standings.component.html',
  styleUrls: ['./standings.component.scss']
})
export class StandingsComponent implements OnInit {

  teams: Team[]

  constructor(private teamService: TeamService) {
  }

  ngOnInit(): void {
    this.teamService.getStandings().subscribe(teams => this.teams = teams)
  }
}
