import {Team} from './team';

/**
 * One row of the league table from GET /api/teams/standings. Extends Team so
 * row objects drop into every Team-typed slot (team pills, lookups) directly.
 */
export interface Standing extends Team {
  /** 1-based table position — teams without a finished match rank at the bottom. */
  place: number;
  played: number;
  wins: number;
  draws: number;
  losses: number;
  goalsFor: number;
  goalsAgainst: number;
}

/** Full standings response; afterQueue is the latest queue with a played match. */
export interface Standings {
  afterQueue: number | null;
  rows: Standing[];
}
