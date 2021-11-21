import {Component} from '@angular/core';
import {GradeService} from "./service/grade.service";
import {MatchService} from "./service/match.service";
import {TeamService} from "./service/team.service";
import {RefereeService} from "./service/referee.service";
import {MatDialog} from "@angular/material/dialog";
import {InfoModalComponent} from "./component/modals/info-modal/info-modal.component";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {

  title: string;

  constructor(private matchService: MatchService, private teamService: TeamService,
              private refereeService: RefereeService, private gradeService: GradeService,
              private dialog: MatDialog) {
    this.title = 'Referee Staffer';
  }

  clearData() {
    this.gradeService.deleteAll().subscribe(() =>
      this.matchService.deleteAll().subscribe(() => {
        this.refereeService.deleteAll().subscribe()
        this.teamService.deleteAll().subscribe()
        this.dialog.open(InfoModalComponent, {data: {header: "Success", message: "Data has been cleared successfully!"}})
      }))
  }
}
