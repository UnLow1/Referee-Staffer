package com.jamex.refereestaffer.mcp.view;

import java.util.List;

/**
 * Everything an AI client needs to reason about a single referee: the list-level summary
 * plus the full match history (with grades) and the unavailability windows.
 *
 * @param homeWins matches officiated that the home team won
 * @param awayWins matches officiated that the away team won — together with
 *                 {@code homeWins} a fairness signal, not proof of bias on its own
 */
public record RefereeProfile(
        RefereeSummary referee,
        short homeWins,
        short awayWins,
        List<RefereeMatch> matches,
        List<VacationPeriod> vacations
) {
}
