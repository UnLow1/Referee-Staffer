import {Component, OnInit} from '@angular/core';
import {Match} from "../../model/match";
import {MatchService} from "../../service/match.service";
import {TeamService} from "../../service/team.service";
import {RefereeService} from "../../service/referee.service";
import {Team} from "../../model/team";
import {Referee} from "../../model/referee";
import {GradeService} from "../../service/grade.service";
import {Grade} from "../../model/grade";

@Component({
  selector: 'app-match-list',
  templateUrl: './match-list.component.html',
  styleUrls: ['./match-list.component.css']
})
export class MatchListComponent implements OnInit {

  constructor(private matchService: MatchService, private teamService: TeamService,
              private refereeService: RefereeService, private gradeService: GradeService) {
  }

  matches: Match[]
  teams: Team[]
  referees: Referee[]
  grades: Grade[]

  ngOnInit() {
    this.matchService.findAll().subscribe(matches => {
      this.matches = matches.sort((a, b) => a.queue - b.queue)
      let teamIds = matches.map(match => match.homeTeamId).concat(matches.map(match => match.awayTeamId)).filter((item, i, ar) => ar.indexOf(item) === i)
      let refereeIds = matches.map(match => match.refereeId).filter((item, i, ar) => ar.indexOf(item) === i)
      let gradeIds = matches.map(match => match.gradeId).filter((item, i, ar) => ar.indexOf(item) === i)

      this.teamService.findByIds(teamIds).subscribe(teams => this.teams = teams)
      this.refereeService.findByIds(refereeIds).subscribe(referees => this.referees = referees)
      this.gradeService.findByIds(gradeIds).subscribe(grades => this.grades = grades)
    })
  }

  getTeam(teamId: number): Team {
    return this.teams?.find(team => team.id === teamId)
  }

  getReferee(refereeId: number): Referee {
    return this.referees?.find(referee => referee.id ===refereeId)
  }

  getGrade(gradeId: number): Grade {
    return this.grades?.find(grade => grade.id === gradeId)
  }
}
