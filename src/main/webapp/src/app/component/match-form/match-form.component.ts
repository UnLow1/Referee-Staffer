import {Component, OnInit} from '@angular/core';
import {Referee} from "../../model/referee";
import {ActivatedRoute, Router} from "@angular/router";
import {RefereeService} from "../../service/referee.service";
import {Match} from "../../model/match";
import {MatchService} from "../../service/match.service";
import {TeamService} from "../../service/team.service";
import {Team} from "../../model/team";

@Component({
  selector: 'app-match-form',
  templateUrl: './match-form.component.html',
  styleUrls: ['./match-form.component.css']
})
export class MatchFormComponent implements OnInit {

  match: Match
  teams: Team[]
  referees: Referee[]

  constructor(private route: ActivatedRoute, private router: Router, private matchService: MatchService,
              private teamService: TeamService, private refereeService: RefereeService) {
    this.match = new Match()
  }

  ngOnInit() {
    this.teamService.findAll().subscribe(data => {
      this.teams = data
    })
    this.refereeService.findAll().subscribe(data => {
      this.referees = data
    })
  }

  onSubmit() {
    this.matchService.save(this.match).subscribe(result => this.gotoMatchesList())
  }

  gotoMatchesList() {
    this.router.navigate(['/matches'])
  }
}
