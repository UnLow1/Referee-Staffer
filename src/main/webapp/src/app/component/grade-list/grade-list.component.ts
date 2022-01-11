import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {GradeService} from "../../service/grade.service";
import {RefereeService} from "../../service/referee.service";
import {Referee} from "../../model/referee";
import {Grade} from "../../model/grade";
import {MatchService} from "../../service/match.service";
import {Match} from "../../model/match";

@Component({
  selector: 'app-grade-list',
  templateUrl: './grade-list.component.html',
  styleUrls: ['./grade-list.component.scss']
})
export class GradeListComponent implements OnInit {

  matches: Match[]
  referees: Referee[]
  grades: Grade[]
  sortedByGradesDesc: boolean
  sortedByNameAsc: boolean

  constructor(private route: ActivatedRoute, private router: Router, private refereeService: RefereeService,
              private gradeService: GradeService, private matchService: MatchService) {
  }

  ngOnInit(): void {
    this.matchService.findAll().subscribe(matches => {
      this.matches = matches
      let gradeIds = matches.map(match => match.gradeId).filter((item, i, ar) => ar.indexOf(item) === i)
      this.gradeService.findByIds(gradeIds).subscribe(grades => this.grades = grades)
    })
    this.refereeService.findAll().subscribe(referees => this.referees = referees)
  }

  getGradesForReferee(referee: Referee): number[] {
    let gradeIds = this.matches?.filter(match => match.refereeId === referee.id).map(match => match.gradeId)
    return this.grades?.filter(grade => gradeIds.includes(grade.id)).map(grade => grade.value)
  }

  countAverageForReferee(referee: Referee): number {
    let grades = this.getGradesForReferee(referee)
    return grades?.reduce((a, b) => a + b, 0) / grades?.length
  }

  sortTableByGrade() {
    this.referees = this.referees.sort(this.gradeComparator())
    this.sortedByGradesDesc = !this.sortedByGradesDesc
  }

  sortTableByName() {
    this.referees = this.referees.sort(this.nameComparator())
    this.sortedByNameAsc = !this.sortedByNameAsc
  }

  private gradeComparator() {
    if (this.sortedByGradesDesc)
      return (a, b) => this.countAverageForReferee(a) - this.countAverageForReferee(b);
    else
      return (a, b) => this.countAverageForReferee(b) - this.countAverageForReferee(a);
  }

  private nameComparator() {
    if (this.sortedByNameAsc)
      return (a, b) => b.lastName.localeCompare(a.lastName);
    else
      return (a, b) => a.lastName.localeCompare(b.lastName);
  }
}
