import {Component} from '@angular/core';
import {GradeService} from "./service/grade.service";
import {MatchService} from "./service/match.service";
import {TeamService} from "./service/team.service";
import {RefereeService} from "./service/referee.service";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {

  title: string;

  constructor(private matchService: MatchService, private teamService: TeamService,
              private refereeService: RefereeService, private gradeService: GradeService) {
    this.title = 'Referee Staffer';
  }

  clearData() {
    this.gradeService.deleteAll().subscribe(() =>
      this.matchService.deleteAll().subscribe(() => {
        this.refereeService.deleteAll().subscribe()
        this.teamService.deleteAll().subscribe()
      }))
  }
}
