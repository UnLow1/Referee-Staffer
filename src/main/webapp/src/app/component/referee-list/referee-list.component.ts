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
}
