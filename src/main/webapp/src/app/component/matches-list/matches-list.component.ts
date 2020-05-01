import { Component, OnInit } from '@angular/core';
import {Match} from "../../model/match";
import {MatchService} from "../../service/match.service";

@Component({
  selector: 'app-matches-list',
  templateUrl: './matches-list.component.html',
  styleUrls: ['./matches-list.component.css']
})
export class MatchesListComponent implements OnInit {

  matches: Match[];

  constructor(private matchService: MatchService) {
  }

  ngOnInit() {
    this.matchService.findAll().subscribe(data => {
      this.matches = data;
    });
  }

}
