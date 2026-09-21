/**
 * What a staffing run would take away in a queue: the ids of matches that already carry
 * a referee and that the staffer is allowed to clear. Fetched before a regenerate so the
 * Staffer screen can warn about assignments — including hand-made ones — that are about
 * to be replaced (RS-109).
 */
export interface StaffingOverwrite {
  queue: number;
  assignedMatchIds: number[];
}
