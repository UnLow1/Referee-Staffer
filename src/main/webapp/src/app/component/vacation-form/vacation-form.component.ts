import { Component, OnInit, inject } from '@angular/core';
import {Vacation} from "../../model/vacation";
import {ActivatedRoute, ParamMap, Router} from "@angular/router";
import {VacationService} from "../../service/vacation.service";
import {RefereeService} from "../../service/referee.service";
import {Referee} from "../../model/referee";
import { FormsModule } from '@angular/forms';

@Component({
    selector: 'app-vacation-form',
    templateUrl: './vacation-form.component.html',
    styleUrls: ['./vacation-form.component.scss'],
    imports: [FormsModule]
})
export class VacationFormComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private vacationService = inject(VacationService);
  private refereeService = inject(RefereeService);


  vacation: Vacation = {} as Vacation
  editMode = false
  referees: Referee[] = []

  ngOnInit(): void {
    this.refereeService.findAll().subscribe(data => {
      this.referees = data
    })
    this.route.paramMap.subscribe(
      (params: ParamMap) => {
        const id = Number(params.get('id'))
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
