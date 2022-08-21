import {Component, OnInit} from '@angular/core';
import {Vacation} from "../../model/vacation";
import {ActivatedRoute, ParamMap, Router} from "@angular/router";
import {VacationService} from "../../service/vacation.service";
import {RefereeService} from "../../service/referee.service";
import {Referee} from "../../model/referee";

@Component({
  selector: 'app-vacation-form',
  templateUrl: './vacation-form.component.html',
  styleUrls: ['./vacation-form.component.scss']
})
export class VacationFormComponent implements OnInit {

  vacation: Vacation = new Vacation()
  editMode: boolean
  referees: Referee[];

  constructor(private route: ActivatedRoute, private router: Router, private vacationService: VacationService, private refereeService: RefereeService) {
  }

  ngOnInit(): void {
    this.refereeService.findAll().subscribe(data => {
      this.referees = data
    })
    this.route.paramMap.subscribe(
      (params: ParamMap) => {
        let id = Number(params.get('id'))
        if (id) {
          this.editMode = true
          this.vacationService.findById(id).subscribe(vacation => this.vacation = vacation)
        }
      }
    )
  }

  onSubmit() {
    if (this.editMode)
      this.vacationService.update(this.vacation).subscribe(() => this.gotoVacationsList())
    else
      this.vacationService.save(this.vacation).subscribe(() => this.gotoVacationsList())
  }

  gotoVacationsList() {
    this.router.navigate(['vacations'])
  }
}
