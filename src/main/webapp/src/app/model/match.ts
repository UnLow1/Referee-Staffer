export interface Match {
  id: number;
  queue: number;
  homeTeamId: number;
  awayTeamId: number;
  // Null while the fixture is planned but not scheduled yet — the auto-staffer skips it.
  date: Date | null;
  refereeId: number;
  gradeId: number;
  homeScore: number;
  awayScore: number;
  // Computed difficulty (0..150-ish), populated by the backend after staffReferees /
  // getMatchesToAssignInQueue. May be undefined for matches fetched via plain /api/matches
  // without going through the staffer flow.
  hardnessLvl?: number;
}
