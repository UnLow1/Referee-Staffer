import {Component, Input} from '@angular/core';
import {InfoModalComponent} from "../../modals/info-modal/info-modal.component";
import {MatchService} from "../../../service/match.service";
import {TeamService} from "../../../service/team.service";
import {RefereeService} from "../../../service/referee.service";
import {GradeService} from "../../../service/grade.service";
import {MatDialog} from "@angular/material/dialog";

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss']
})
export class HeaderComponent {

  @Input()
  title: string;

  constructor(private matchService: MatchService, private teamService: TeamService,
              private refereeService: RefereeService, private gradeService: GradeService,
              private dialog: MatDialog) {
  }

  clearData() {
    this.gradeService.deleteAll().subscribe(() =>
      this.matchService.deleteAll().subscribe(() => {
        this.refereeService.deleteAll().subscribe()
        this.teamService.deleteAll().subscribe()
        this.dialog.open(InfoModalComponent, {
          data: {
            header: "Success",
            message: "Data has been cleared successfully!"
          }
        })
      }))
  }
}
