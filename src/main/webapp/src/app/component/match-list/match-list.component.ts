import { Component, OnInit } from '@angular/core';
import {Match} from "../../model/match";
import {MatchService} from "../../service/match.service";

@Component({
  selector: 'app-match-list',
  templateUrl: './match-list.component.html',
  styleUrls: ['./match-list.component.css']
})
export class MatchListComponent implements OnInit {

  constructor(private matchService: MatchService) {
  }

  matches: Match[]

  ngOnInit() {
    this.matchService.findAll().subscribe(data => {
      this.matches = data
    })
  }
}
