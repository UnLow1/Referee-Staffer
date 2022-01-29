import {Component, OnInit} from '@angular/core';
import {Referee} from "../../model/referee";
import {ActivatedRoute, ParamMap, Router} from "@angular/router";
import {RefereeService} from "../../service/referee.service";
import {Match} from "../../model/match";
import {MatchService} from "../../service/match.service";
import {TeamService} from "../../service/team.service";
import {Team} from "../../model/team";
import {Grade} from "../../model/grade";
import {GradeService} from "../../service/grade.service";

@Component({
  selector: 'app-match-form',
  templateUrl: './match-form.component.html',
  styleUrls: ['./match-form.component.scss']
})
export class MatchFormComponent implements OnInit {

  match: Match = new Match()
  grade: Grade = new Grade()
  teams: Team[]
  referees: Referee[]
  editMode: boolean

  constructor(private route: ActivatedRoute, private router: Router, private matchService: MatchService,
              private teamService: TeamService, private refereeService: RefereeService,
              private gradeService: GradeService) {
  }

  ngOnInit() {
    this.teamService.findAll().subscribe(data => {
      this.teams = data
    })
    this.refereeService.findAll().subscribe(data => {
      this.referees = data
    })
    this.route.paramMap.subscribe(
      (params: ParamMap) => {
        let id = Number(params.get('id'))
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
    this.router.navigate(['/matches'])
  }
}
