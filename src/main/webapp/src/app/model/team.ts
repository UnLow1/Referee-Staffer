export interface Team {
  id: number;
  name: string;
  city: string;
  points: number;
  // 3-letter code (e.g. "LEG", "LCH") used by TeamPill. The backend always sends it
  // (TeamDto renames shortCode → short); TeamPill still falls back to deriving from
  // `name` when absent.
  short?: string;
  // Home ground, both halves optional: teams created by the CSV importer have none until
  // someone fills them in from the team drawer. Rendered as "name (address)" on the
  // assignment PDF, which is currently the only consumer.
  venueName?: string | null;
  venueAddress?: string | null;
}
