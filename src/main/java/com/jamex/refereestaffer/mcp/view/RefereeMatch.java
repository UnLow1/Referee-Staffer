package com.jamex.refereestaffer.mcp.view;

import java.time.LocalDateTime;

/**
 * One match from a referee's history.
 *
 * @param score         final score as {@code "2:1"}, or null when the match has not been played
 * @param grade         the observer grade that counts towards statistics — the mean of both
 *                      components for a split grade (see {@code Grade#getEffectiveValue})
 * @param gradeAsAwarded the grade as the observer wrote it: {@code "7.9/8.3"} for a split
 *                      grade ("łamana ocena"), {@code "8.5"} for a plain one, null when
 *                      no grade has been entered
 */
public record RefereeMatch(
        Long matchId,
        Short queue,
        LocalDateTime date,
        String homeTeam,
        String awayTeam,
        String score,
        Double grade,
        String gradeAsAwarded
) {
}
