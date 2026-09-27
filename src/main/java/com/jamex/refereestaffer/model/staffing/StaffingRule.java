package com.jamex.refereestaffer.model.staffing;

/**
 * A rule the staffer would break by putting a given referee on a given match.
 *
 * <p>Rules are advisory, never blocking: the auto-staffer uses them as a filter, but a human
 * may always assign a referee by hand and only gets a warning (RS-111). The enum name is the
 * wire format — the frontend switches on it to pick a short chip label, so renaming a constant
 * is a breaking API change.
 */
public enum StaffingRule {

    /** The referee has a vacation covering the match day. */
    VACATION,

    /** The referee already officiates another match on the same calendar day (RS-57). */
    SAME_DAY_MATCH,

    /** The referee already officiates another match in the same queue. */
    DOUBLE_MATCH_IN_QUEUE
}
