import {Team} from "./team";
import {Referee} from "./referee";
import {Grade} from "./grade";

export class Match {
  queue: number
  home: Team
  away: Team
  referee: Referee
  grade: Grade
  homeScore: number
  awayScore: number
}
