package com.jamex.refereestaffer.mcp.view;

import java.time.LocalDateTime;

/**
 * One match with its current assignment. Team and referee names are inlined instead of
 * the ids {@link com.jamex.refereestaffer.model.dto.MatchDto} carries, so a client does
 * not have to resolve them with extra calls; ids are kept as well because the
 * difficulty tool takes a match id.
 *
 * @param central true when the assignment is the "S C" central sentinel, i.e. fixed
 *                top-down and out of the staffer's reach
 */
public record MatchSummary(
        Long id,
        Short queue,
        LocalDateTime date,
        String homeTeam,
        String awayTeam,
        Short homeScore,
        Short awayScore,
        Long refereeId,
        String refereeName,
        boolean central,
        Double grade
) {
}
