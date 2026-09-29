export interface Referee {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  experience: number;
  /**
   * Average observer grade across the referee's match history. Populated by the
   * read-only endpoints (`/api/referees`, `/api/referees/:id`) via
   * RefereeService.enrichWithStats. Explicitly `null` on the wire when the referee has no
   * graded match yet — RefereeDto does not set `@JsonInclude(NON_NULL)`, so the key is
   * present with a null value rather than absent. Typed to include null so every call site
   * has to handle it under strictNullChecks (RS-114).
   */
  averageGrade?: number | null;
  /** Highest queue this referee has ever been assigned to. Null when never assigned. */
  lastQueue?: number;
  /** Computed potential P = α·avg + β·experience. Null when not enriched. */
  potential?: number;
  /**
   * Number of past matches officiated where the home team won. Paired with
   * {@link awayWins} this is the win-distribution signal surfaced on the Profile
   * screen as a side-by-side bar. Null when not enriched.
   */
  homeWins?: number;
  /** Number of past matches officiated where the away team won. See homeWins. */
  awayWins?: number;
}
