export interface Match {
  id: number;
  queue: number;
  homeTeamId: number;
  awayTeamId: number;
  /**
   * Kick-off as the backend sends it: an ISO-8601 local date-time string
   * ("2026-03-01T12:00:00", LocalDateTime without a zone), bound straight to the
   * native datetime-local input. Never a parsed Date — nothing in the app converts
   * it. Optional because MatchDto.date carries no @NotNull.
   */
  date?: string;
  /** Assigned referee; absent until the staffer (or a manual edit) fills it in. */
  refereeId?: number;
  /** Observer grade for the assignment; absent until the match has been graded. */
  gradeId?: number;
  /** Final score; both absent until the match has been played. */
  homeScore?: number;
  awayScore?: number;
  // Computed difficulty (0..150-ish), populated by the backend after staffReferees /
  // getMatchesToAssignInQueue. May be undefined for matches fetched via plain /api/matches
  // without going through the staffer flow.
  hardnessLvl?: number;
}
