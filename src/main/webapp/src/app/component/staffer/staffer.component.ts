import { Component, inject } from '@angular/core';
import {StafferService} from "../../service/staffer.service";
import {ActivatedRoute, Router} from "@angular/router";
import {Match} from "../../model/match";
import {Team} from "../../model/team";
import {Referee} from "../../model/referee";
import {TeamService} from "../../service/team.service";
import {RefereeService} from "../../service/referee.service";
import {MatchService} from "../../service/match.service";
import { FormsModule } from '@angular/forms';

@Component({
    selector: 'app-staffer',
    templateUrl: './staffer.component.html',
    styleUrls: ['./staffer.component.scss'],
    imports: [FormsModule]
})
export class StafferComponent {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private stafferService = inject(StafferService);
  private teamService = inject(TeamService);
  private refereeService = inject(RefereeService);
  private matchService = inject(MatchService);


  queue: number
  matches: Match[]
  teams: Team[]
  referees: Referee[]
  acceptedMatches: Match[] = []

  onSubmit() {
    this.stafferService.staffReferees(this.queue).subscribe(matches => {
      this.matches = matches

      const teamIds = matches.map(match => match.homeTeamId)
        .concat(matches.map(match => match.awayTeamId))
        .filter((item, i, ar) => ar.indexOf(item) === i)

      this.teamService.findByIds(teamIds).subscribe(teams => this.teams = teams)
      this.refereeService.findRefereesAvailableForQueue(this.queue).subscribe(referees => this.referees = referees)
    })
  }

  getTeam(teamId: number): Team {
    return this.teams?.find(team => team.id === teamId)
  }

  getRefereeName(referee: Referee): string {
    return referee.firstName + " " + referee.lastName
  }

  updateMatches() {
    this.matchService.updateList(this.matches).subscribe(() => this.gotoMatchesList())
  }

  gotoMatchesList() {
    this.router.navigate(['matches'])
  }

  updateMatch(match: Match) {
    this.matchService.update(match).subscribe(() => this.acceptedMatches.push(match))
  }
}
