package com.jamex.refereestaffer.model.staffing;

/**
 * One broken rule, carrying both the machine-readable code and a ready-to-render sentence.
 *
 * <p>The message is built on the backend on purpose: some rules need data the frontend never
 * loads (matches of other queues, vacation ranges), so a frontend-side message would mean a
 * second, partial implementation of the rules.
 */
public record StaffingViolation(StaffingRule rule, String message) {
}
