import { Component, Input, inject } from '@angular/core';
import {InfoModalComponent} from "../../modals/info-modal/info-modal.component";
import {MatchService} from "../../../service/match.service";
import {TeamService} from "../../../service/team.service";
import {RefereeService} from "../../../service/referee.service";
import {GradeService} from "../../../service/grade.service";
import {MatDialog} from "@angular/material/dialog";
import { RouterLink } from '@angular/router';

@Component({
    selector: 'app-header',
    templateUrl: './header.component.html',
    styleUrls: ['./header.component.scss'],
    imports: [RouterLink]
})
export class HeaderComponent {
  private matchService = inject(MatchService);
  private teamService = inject(TeamService);
  private refereeService = inject(RefereeService);
  private gradeService = inject(GradeService);
  private dialog = inject(MatDialog);


  @Input()
  title: string

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
