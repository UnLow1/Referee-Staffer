import {Component, OnInit} from '@angular/core';
import {RefereeService} from "../../service/referee.service";
import {Referee} from "../../model/referee";
import {Router} from "@angular/router";

@Component({
  selector: 'app-referee-list',
  templateUrl: './referee-list.component.html',
  styleUrls: ['./referee-list.component.scss']
})
export class RefereeListComponent implements OnInit {

  referees: Referee[]

  constructor(private router: Router, private refereeService: RefereeService) {
  }

  ngOnInit() {
    this.refereeService.findAll().subscribe(referees => this.referees = referees)
  }

  editReferee(referee: Referee) {
    this.router.navigate(['addReferee', referee.id])
  }

  deleteReferee(refereeToDelete: Referee) {
    this.refereeService.delete(refereeToDelete.id).subscribe(() =>
      this.referees = this.referees.filter((referee: Referee) => referee.id !== refereeToDelete.id))
  }
}
