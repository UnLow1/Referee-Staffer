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

/**
 * Pattern a short-code override must match, mirroring TeamDto's `@Pattern` on the backend.
 *
 * Exported as a RegExp, not a string: Angular compiles a string `pattern` with
 * `new RegExp('^' + pattern + '$')` and no `u` flag, which silently degrades `\p{L}` to an
 * identity escape and rejects every ordinary code. Bind `[pattern]` to this instead.
 */
export const SHORT_CODE_PATTERN = /^[\p{L}\p{N}]*$/u;

/** Longest override the team pill can render; matches TeamDto's `@Size(max = 4)`. */
export const SHORT_CODE_MAX_LENGTH = 4;

/**
 * The code to render for a team: its backend-computed `short` when present, otherwise the
 * name-derived fallback.
 *
 * The fallback mirrors `Team.deriveShortCode` in Java — first three alphanumeric characters,
 * upper-cased, separators skipped so "FC Barcelona" gives FCB. Like
 * `Grade.getEffectiveValue` / `effectiveGradeValue`, the formula necessarily exists on both
 * sides; change one and you must change the other.
 */
export function shortCodeOf(team: Team | undefined | null): string {
  if (!team) return '';
  return team.short ?? deriveShortCode(team.name);
}

/** Name-derived short code — the frontend half of `Team.deriveShortCode`. */
export function deriveShortCode(name: string | undefined | null): string {
  return [...(name ?? '')].filter(c => /[\p{L}\p{N}]/u.test(c)).slice(0, 3).join('').toUpperCase();
}
