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

/**
 * Body of POST /api/matches. `id` is backend-assigned (MatchDto marks it `@NotNull` only
 * for the OnUpdate group), `gradeId` is managed through /api/grades and `hardnessLvl` is
 * computed server-side, so none of the three belongs in a create payload. Queue and both
 * team ids are mandatory — exactly MatchDto's unconditional `@NotNull` set; everything
 * else the backend accepts as null.
 */
export type NewMatch =
  Partial<Omit<Match, 'id' | 'gradeId' | 'hardnessLvl'>>
  & Pick<Match, 'queue' | 'homeTeamId' | 'awayTeamId'>;
