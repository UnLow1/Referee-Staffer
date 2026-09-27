/**
 * A staffing rule the backend says a (match, referee) pairing breaks. Mirrors
 * `com.jamex.refereestaffer.model.staffing.StaffingRule` — the codes are the wire contract,
 * so a backend rename is a breaking change here too.
 */
export type StaffingRule = 'VACATION' | 'SAME_DAY_MATCH' | 'DOUBLE_MATCH_IN_QUEUE';

export interface StaffingViolation {
  rule: StaffingRule;
  /** Ready-to-render sentence built by the backend — the frontend never composes it. */
  message: string;
}

/**
 * One conflicting pairing from `GET /api/staffer/:queue/violations`. The endpoint returns the
 * whole queue's matrix at once and omits clean pairs, so the staffer drawer can show warnings
 * without a request of its own.
 */
export interface CandidateViolations {
  matchId: number;
  refereeId: number;
  violations: StaffingViolation[];
}
