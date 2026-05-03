import { Component, OnInit, inject } from '@angular/core';
import {TeamService} from "../../service/team.service";
import {Team} from "../../model/team";
import { Router, RouterLink } from "@angular/router";
import { EditButtonComponent } from '../common/button/edit-button/edit-button.component';
import { DeleteButtonComponent } from '../common/button/delete-button/delete-button.component';
import { AddButtonComponent } from '../common/button/add-button/add-button.component';

@Component({
    selector: 'app-team-list',
    templateUrl: './team-list.component.html',
    styleUrls: ['./team-list.component.scss'],
    imports: [EditButtonComponent, DeleteButtonComponent, AddButtonComponent, RouterLink]
})
export class TeamListComponent implements OnInit {
  private router = inject(Router);
  private teamService = inject(TeamService);


  teams: Team[]

  ngOnInit(): void {
    this.teamService.findAll().subscribe(teams => this.teams = teams)
  }

  editTeam(team: Team) {
    this.router.navigate(['addTeam', team.id])
  }

  deleteTeam(teamToDelete: Team) {
    this.teamService.delete(teamToDelete.id).subscribe(() =>
      this.teams = this.teams.filter((team: Team) => team.id !== teamToDelete.id))
  }
}
