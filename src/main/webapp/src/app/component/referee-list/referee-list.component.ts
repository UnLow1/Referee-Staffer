import {Component, OnInit} from '@angular/core';
import {RefereeService} from "../../service/referee.service";
import {Referee} from "../../model/referee";

@Component({
  selector: 'app-referee-list',
  templateUrl: './referee-list.component.html',
  styleUrls: ['./referee-list.component.scss']
})
export class RefereeListComponent implements OnInit {

  referees: Referee[]

  constructor(private refereeService: RefereeService) {
  }

  ngOnInit() {
    this.refereeService.findAll().subscribe(referees => this.referees = referees)
  }

  editReferee(referee: Referee) {
    console.log("Editing referee = " + referee.firstName + " " + referee.lastName)
  }

  deleteReferee(refereeToDelete: Referee) {
    this.refereeService.delete(refereeToDelete.id).subscribe(() =>
      this.referees = this.referees.filter((referee: Referee) => referee.id !== refereeToDelete.id))
  }
}
