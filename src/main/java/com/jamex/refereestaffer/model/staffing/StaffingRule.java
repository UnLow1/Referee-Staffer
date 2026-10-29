package com.jamex.refereestaffer.model.staffing;

/**
 * A rule the staffer would break by putting a given referee on a given match.
 *
 * <p>Rules are advisory, never blocking: the auto-staffer uses them as a filter, but a human
 * may always assign a referee by hand and only gets a warning (RS-111). The enum name is the
 * wire format — the frontend switches on it to pick a short chip label, so renaming a constant
 * is a breaking API change, and a <strong>new</strong> constant needs a matching entry in
 * {@code RULE_LABELS} in {@code staffer.component.ts} (without one the chip falls back to
 * showing the raw code).
 */
public enum StaffingRule {

    /** The referee has a vacation covering the match day. */
    VACATION,

    /** The referee already officiates another match on the same calendar day (RS-57). */
    SAME_DAY_MATCH,

    /**
     * The referee already officiates another match in the same queue. Only reachable for a
     * referee who is not drawn from a queue's availability pool — that pool is by definition
     * "referees with no match in this queue" — so in practice this fires for a stored or
     * hand-made assignment, not for a candidate the auto-staffer is choosing between.
     */
    DOUBLE_MATCH_IN_QUEUE
}
