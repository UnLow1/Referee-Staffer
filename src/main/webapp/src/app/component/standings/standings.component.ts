import { Component, OnInit, inject } from '@angular/core';
import {TeamService} from "../../service/team.service";
import {Team} from "../../model/team";

@Component({
    selector: 'app-standings',
    templateUrl: './standings.component.html',
    styleUrls: ['./standings.component.scss']
})
export class StandingsComponent implements OnInit {
  private teamService = inject(TeamService);


  teams: Team[]

  ngOnInit(): void {
    this.teamService.getStandings().subscribe(teams => this.teams = teams)
  }
}
