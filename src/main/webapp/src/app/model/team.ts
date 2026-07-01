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
