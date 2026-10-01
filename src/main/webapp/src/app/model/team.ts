export interface Team {
  id: number;
  name: string;
  city: string;
  points: number;
  // 3-letter code (e.g. "LEG", "LCH") used by TeamPill. The backend always sends it
  // (TeamDto renames shortCode → short); TeamPill still falls back to deriving from
  // `name` when absent. Read-only: writes are ignored, so edit `shortOverride` instead.
  short?: string;
  // The stored override behind `short`, null when the team has none. This is the writable
  // half of the pair — `short` is computed server-side, so echoing it back on an unrelated
  // edit would freeze the code against future renames. Blank clears the override.
  shortOverride?: string | null;
}
