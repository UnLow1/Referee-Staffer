import { Component, OnInit, inject } from '@angular/core';
import {Vacation} from "../../model/vacation";
import { Router, RouterLink } from "@angular/router";
import {VacationService} from "../../service/vacation.service";
import {Referee} from "../../model/referee";
import {RefereeService} from "../../service/referee.service";
import { EditButtonComponent } from '../common/button/edit-button/edit-button.component';
import { DeleteButtonComponent } from '../common/button/delete-button/delete-button.component';
import { AddButtonComponent } from '../common/button/add-button/add-button.component';
import { DatePipe } from '@angular/common';

@Component({
    selector: 'app-vacation-list',
    templateUrl: './vacation-list.component.html',
    styleUrls: ['./vacation-list.component.scss'],
    imports: [EditButtonComponent, DeleteButtonComponent, AddButtonComponent, RouterLink, DatePipe]
})
export class VacationListComponent implements OnInit {
  private router = inject(Router);
  private vacationService = inject(VacationService);
  private refereeService = inject(RefereeService);


  vacations: Vacation[]
  referees: Referee[] = []

  ngOnInit(): void {
    this.vacationService.findAll().subscribe(vacations => {
      this.vacations = vacations
      const refereesIds = vacations.map(vac => vac.refereeId)
      // TODO remove sub in sub
      this.refereeService.findByIds(refereesIds).subscribe(referees => this.referees = referees)
    })
  }

  editVacation(vacation: Vacation) {
    this.router.navigate(['addVacation', vacation.id])
  }

  deleteVacation(vacationToDelete: Vacation) {
    this.vacationService.delete(vacationToDelete.id).subscribe(() =>
      this.vacations = this.vacations.filter((vacation: Vacation) => vacation.id !== vacationToDelete.id))
  }

  getRefereeName(refereeId: number): string {
    // TODO don't show undefined if referees is not loaded yet
    const referee = this.referees.find(ref => ref.id === refereeId)
    return `${referee?.firstName} ${referee?.lastName}`
  }
}
