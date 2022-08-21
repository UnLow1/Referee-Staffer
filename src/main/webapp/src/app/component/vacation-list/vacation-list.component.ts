import {Component, OnInit} from '@angular/core';
import {Vacation} from "../../model/vacation";
import {Router} from "@angular/router";
import {VacationService} from "../../service/vacation.service";
import {Referee} from "../../model/referee";
import {RefereeService} from "../../service/referee.service";

@Component({
  selector: 'app-vacation-list',
  templateUrl: './vacation-list.component.html',
  styleUrls: ['./vacation-list.component.scss']
})
export class VacationListComponent implements OnInit {

  vacations: Vacation[]
  referees: Referee[] = []

  constructor(private router: Router, private vacationService: VacationService, private refereeService: RefereeService) {
  }

  ngOnInit(): void {
    this.vacationService.findAll().subscribe(vacations => {
      this.vacations = vacations
      let refereesIds = vacations.map(vac => vac.refereeId)
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
    let referee = this.referees.find(ref => ref.id === refereeId)
    return `${referee?.firstName} ${referee?.lastName}`
  }
}
