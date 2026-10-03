export interface Match {
  id: number;
  queue: number;
  homeTeamId: number;
  awayTeamId: number;
  /**
   * Kick-off as the backend sends it: an ISO-8601 local date-time string
   * ("2026-03-01T12:00:00", LocalDateTime without a zone), bound straight to the
   * native datetime-local input. Never a parsed Date — nothing in the app converts it.
   * Required, not optional: the Match entity column is `nullable = false`, so every
   * persisted match has one (the missing @NotNull on MatchDto.date is a write-side
   * validation gap tracked as RS-119, not a read-side nullability).
   */
  date: string;
  /**
   * The four fields below are `| null`, not just optional: Jackson runs with the
   * default ALWAYS inclusion (nothing sets `spring.jackson.default-property-inclusion`
   * or @JsonInclude), so an unset value arrives as an explicit `"refereeId": null`,
   * never as a missing key. Always test them with `!= null` — `=== undefined` is wrong.
   * Same shape as Grade.secondValue and Standings.afterQueue.
   */
  /** Assigned referee; null until the staffer (or a manual edit) fills it in. */
  refereeId?: number | null;
  /** Observer grade for the assignment; null until the match has been graded. */
  gradeId?: number | null;
  /** Final score; both null until the match has been played. */
  homeScore?: number | null;
  awayScore?: number | null;
  // Computed difficulty (0..150-ish), populated by the backend after staffReferees /
  // getMatchesToAssignInQueue. May be undefined for matches fetched via plain /api/matches
  // without going through the staffer flow.
  hardnessLvl?: number;
}
