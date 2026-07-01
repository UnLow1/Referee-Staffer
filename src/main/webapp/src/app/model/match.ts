export interface Match {
  id: number;
  queue: number;
  homeTeamId: number;
  awayTeamId: number;
  date: Date;
  refereeId: number;
  gradeId: number;
  homeScore: number;
  awayScore: number;
  // Computed difficulty (0..150-ish), populated by the backend after staffReferees /
  // getMatchesToAssignInQueue. May be undefined for matches fetched via plain /api/matches
  // without going through the staffer flow.
  hardnessLvl?: number;
}
