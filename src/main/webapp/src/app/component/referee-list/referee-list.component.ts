import { Component, OnInit } from '@angular/core';
import {RefereeService} from "../../service/referee.service";
import {Referee} from "../../model/referee";

@Component({
  selector: 'app-referee-list',
  templateUrl: './referee-list.component.html',
  styleUrls: ['./referee-list.component.css']
})
export class RefereeListComponent implements OnInit {

  constructor(private refereeService: RefereeService) {
  }

  referees: Referee[]

  ngOnInit() {
    this.refereeService.findAll().subscribe(data => {
      this.referees = data
    })
  }
}
