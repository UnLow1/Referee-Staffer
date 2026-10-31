export interface Team {
  id: number;
  name: string;
  city: string;
  // 3-letter code (e.g. "LEG", "LCH") used by TeamPill. The backend always sends it
  // (TeamDto renames shortCode → short); TeamPill still falls back to deriving from
  // `name` when absent.
  short?: string;
}

/**
 * Body of POST /api/teams. `id` is backend-assigned and `short` is read-only (TeamDto
 * ignores it on write), so a create carries name + city — TeamDto's `@NotBlank` pair.
 * Both are omitted rather than made optional, so the type cannot be used to send a value
 * the backend ignores.
 */
export type NewTeam = Omit<Team, 'id' | 'short'>;
