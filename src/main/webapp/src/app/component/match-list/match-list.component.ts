import { Component, OnInit, inject } from '@angular/core';
import {Match} from "../../model/match";
import {MatchService} from "../../service/match.service";
import {TeamService} from "../../service/team.service";
import {RefereeService} from "../../service/referee.service";
import {Team} from "../../model/team";
import {Referee} from "../../model/referee";
import {GradeService} from "../../service/grade.service";
import {Grade} from "../../model/grade";
import { Router, RouterLink } from "@angular/router";
import {groupBy} from 'lodash-es';
import { EditButtonComponent } from '../common/button/edit-button/edit-button.component';
import { DeleteButtonComponent } from '../common/button/delete-button/delete-button.component';
import { AddButtonComponent } from '../common/button/add-button/add-button.component';
import { DatePipe, KeyValuePipe } from '@angular/common';

@Component({
    selector: 'app-match-list',
    templateUrl: './match-list.component.html',
    styleUrls: ['./match-list.component.scss'],
    imports: [EditButtonComponent, DeleteButtonComponent, AddButtonComponent, RouterLink, DatePipe, KeyValuePipe]
})
export class MatchListComponent implements OnInit {
  private router = inject(Router);
  private matchService = inject(MatchService);
  private teamService = inject(TeamService);
  private refereeService = inject(RefereeService);
  private gradeService = inject(GradeService);


  groupedMatches: Record<number, Match[]>
  teams: Team[]
  referees: Referee[]
  grades: Grade[]

  ngOnInit() {
    this.matchService.findAll().subscribe(matches => {
      this.groupedMatches = groupBy(matches, (match) => match.queue);
      const teamIds = matches.map(match => match.homeTeamId)
        .concat(matches.map(match => match.awayTeamId))
        .filter((item, i, ar) => ar.indexOf(item) === i)
      const refereeIds = matches.map(match => match.refereeId)
        .filter(this.notEmpty)
        .filter((item, i, ar) => ar.indexOf(item) === i)
      const gradeIds = matches.map(match => match.gradeId)
        .filter(this.notEmpty)
        .filter((item, i, ar) => ar.indexOf(item) === i)

      this.teamService.findByIds(teamIds).subscribe(teams => this.teams = teams)
      this.refereeService.findByIds(refereeIds).subscribe(referees => this.referees = referees)
      this.gradeService.findByIds(gradeIds).subscribe(grades => this.grades = grades)
    })
  }

  notEmpty<TValue>(value: TValue | null | undefined): value is TValue {
    return value !== null && value !== undefined;
  }

  getTeam(teamId: number): Team {
    return this.teams?.find(team => team.id === teamId)
  }

  getReferee(refereeId: number): Referee {
    return this.referees?.find(referee => referee.id === refereeId)
  }

  getGrade(gradeId: number): Grade {
    return this.grades?.find(grade => grade.id === gradeId)
  }

  editMatch(match: Match) {
    this.router.navigate(['addMatch', match.id])
  }

  deleteMatch(matchToDelete: Match) {
    this.matchService.delete(matchToDelete.id).subscribe(() =>
      this.groupedMatches[matchToDelete.queue] = this.groupedMatches[matchToDelete.queue].filter((match: Match) => match.id !== matchToDelete.id))
  }
}
