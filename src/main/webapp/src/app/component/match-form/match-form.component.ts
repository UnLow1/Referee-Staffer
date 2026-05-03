import { Component, OnInit, inject } from '@angular/core';
import {Referee} from "../../model/referee";
import {ActivatedRoute, ParamMap, Router} from "@angular/router";
import {RefereeService} from "../../service/referee.service";
import {Match} from "../../model/match";
import {MatchService} from "../../service/match.service";
import {TeamService} from "../../service/team.service";
import {Team} from "../../model/team";
import {Grade} from "../../model/grade";
import {GradeService} from "../../service/grade.service";
import { FormsModule } from '@angular/forms';
import { ExcludeValuePipe } from '../../pipe/exclude-value.pipe';

@Component({
    selector: 'app-match-form',
    templateUrl: './match-form.component.html',
    styleUrls: ['./match-form.component.scss'],
    imports: [FormsModule, ExcludeValuePipe]
})
export class MatchFormComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private matchService = inject(MatchService);
  private teamService = inject(TeamService);
  private refereeService = inject(RefereeService);
  private gradeService = inject(GradeService);


  match: Match = new Match()
  grade: Grade = new Grade()
  teams: Team[]
  referees: Referee[]
  editMode: boolean

  ngOnInit() {
    this.teamService.findAll().subscribe(data => {
      this.teams = data
    })
    this.refereeService.findAll().subscribe(data => {
      this.referees = data
    })
    this.route.paramMap.subscribe(
      (params: ParamMap) => {
        const id = Number(params.get('id'))
        if (id) {
          this.editMode = true
          this.matchService.findById(id).subscribe(match => {
            this.match = match
            if (match.gradeId)
              this.gradeService.findById(match.gradeId).subscribe(grade => this.grade = grade)
          })
        }
      }
    )
  }

  onSubmit() {
    if (this.editMode)
      this.matchService.update(this.match).subscribe(match => {
        if (this.isGradeUpdated())
          this.gradeService.update(this.grade).subscribe(() => this.gotoMatchesList())
        else if (this.isNewGradeAdded())
          this.gradeService.save(match, this.grade).subscribe(() => this.gotoMatchesList())
        else if (this.isGradeRemoved())
          this.gradeService.delete(this.grade).subscribe(() => this.gotoMatchesList())
        else
          this.gotoMatchesList()
      })
    else
      this.matchService.save(this.match).subscribe(match => {
          if (this.isNewGradeAdded())
            this.gradeService.save(match, this.grade).subscribe(() => this.gotoMatchesList())
          else
            this.gotoMatchesList()
        }
      )
  }

  private isGradeRemoved() {
    return this.grade.id;
  }

  private isNewGradeAdded() {
    return this.grade.value;
  }

  private isGradeUpdated() {
    return this.isGradeRemoved() && this.isNewGradeAdded() ;
  }

  gotoMatchesList() {
    this.router.navigate(['matches'])
  }
}
