import {Component, OnInit} from '@angular/core';
import {StafferService} from "../../service/staffer.service";
import {ActivatedRoute, Router} from "@angular/router";
import {Match} from "../../model/match";
import {Team} from "../../model/team";
import {Referee} from "../../model/referee";
import {TeamService} from "../../service/team.service";
import {RefereeService} from "../../service/referee.service";
import {MatchService} from "../../service/match.service";

@Component({
  selector: 'app-staffer',
  templateUrl: './staffer.component.html',
  styleUrls: ['./staffer.component.css']
})
export class StafferComponent implements OnInit {

  queue: number
  matches: Match[]
  teams: Team[]
  referees: Referee[]

  constructor(private route: ActivatedRoute, private router: Router, private stafferService: StafferService,
              private teamService: TeamService, private refereeService: RefereeService,
              private matchService: MatchService) {
  }

  ngOnInit(): void {
  }

  onSubmit() {
    this.stafferService.staffReferees(this.queue).subscribe(matches => {
      this.matches = matches

      let teamIds = matches.map(match => match.homeTeamId).concat(matches.map(match => match.awayTeamId)).filter((item, i, ar) => ar.indexOf(item) === i)
      let refereeIds = matches.map(match => match.refereeId).filter((item, i, ar) => ar.indexOf(item) === i)

      this.teamService.findByIds(teamIds).subscribe(teams => this.teams = teams)
      this.refereeService.findByIds(refereeIds).subscribe(referees => this.referees = referees)
    })
  }

  getTeam(teamId: number): Team {
    return this.teams?.find(team => team.id === teamId)
  }

  getReferee(refereeId: number): Referee {
    return this.referees?.find(referee => referee.id === refereeId)
  }

  updateMatches() {
    this.matchService.update(this.matches).subscribe(() => this.gotoMatchesList())
  }

  gotoMatchesList() {
    this.router.navigate(['/matches'])
  }
}
