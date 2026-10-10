package com.jamex.refereestaffer.mcp.view;

/**
 * One row of the referee list as exposed to an AI client. Deliberately flatter and
 * smaller than {@link com.jamex.refereestaffer.model.dto.RefereeDto}: first/last name are
 * joined, the teams-refereed map is dropped (it is per-team noise that only the staffing
 * algorithm consumes) and the win-distribution counters move to the profile view.
 *
 * @param matchesRefereed total matches already officiated — the same number the staffing
 *                        penalty is computed from (Referee#numberOfMatchesInRound, which
 *                        despite its name counts all matches, not just one round)
 * @param central         true for the "S C" sentinel (Sędzia z Centrali): a top-down
 *                        central assignment the staffer must never touch
 */
public record RefereeSummary(
        Long id,
        String name,
        String email,
        int experience,
        Double averageGrade,
        Double potential,
        Short matchesRefereed,
        Short lastQueue,
        boolean central
) {
}
