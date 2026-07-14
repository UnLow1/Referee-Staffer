/**
 * A pre-pinned (match, referee) pair sent with a staffing request. The backend keeps
 * the pair as-is and staffs only the remaining matches of the queue.
 */
export interface StaffingLock {
  matchId: number;
  refereeId: number;
}
