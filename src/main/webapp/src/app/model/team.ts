export interface Team {
  id: number;
  name: string;
  city: string;
  points: number;
  // 3-letter code (e.g. "LEG", "LCH") used by TeamPill. The backend always sends it
  // (TeamDto renames shortCode → short); TeamPill still falls back to deriving from
  // `name` when absent.
  short?: string;
}

/**
 * Body of POST /api/teams. `id` is backend-assigned, `points` is maintained from match
 * results and `short` is read-only (TeamDto ignores it on write), so a create carries
 * name + city — TeamDto's `@NotBlank` pair.
 */
export type NewTeam = Partial<Omit<Team, 'id'>> & Pick<Team, 'name' | 'city'>;
