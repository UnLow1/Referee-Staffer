package com.jamex.refereestaffer.model.dto;

/**
 * How many rows the "clear all data" wipe removed, per table. Counted before the
 * deletes run, so the numbers describe what was there rather than what is left
 * (which is always nothing). Configuration rows are not part of the wipe and are
 * therefore absent here.
 */
public record ClearDataSummaryDto(
        long grades,
        long vacations,
        long matches,
        long referees,
        long teams
) {
}
