import {Component, OnInit} from '@angular/core';
import {TeamService} from "../../service/team.service";
import {Team} from "../../model/team";
import {Router} from "@angular/router";

@Component({
  selector: 'app-team-list',
  templateUrl: './team-list.component.html',
  styleUrls: ['./team-list.component.scss']
})
export class TeamListComponent implements OnInit {

  teams: Team[]

  constructor(private router: Router, private teamService: TeamService) {
  }

  ngOnInit(): void {
    this.teamService.findAll().subscribe(teams => this.teams = teams)
  }

  editTeam(team: Team) {
    this.router.navigate(['/addTeam/', team.id])
  }

  deleteTeam(teamToDelete: Team) {
    this.teamService.delete(teamToDelete.id).subscribe(() =>
      this.teams = this.teams.filter((team: Team) => team.id !== teamToDelete.id))
  }
}
