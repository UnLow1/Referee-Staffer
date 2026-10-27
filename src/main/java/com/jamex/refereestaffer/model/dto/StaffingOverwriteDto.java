package com.jamex.refereestaffer.model.dto;

import java.util.List;

/**
 * What a staffing run for {@code queue} would take away: the ids of matches that are
 * assignable (so the staffer will clear them) and already carry a referee today.
 *
 * <p>Generating persists immediately and reclaims manually assigned matches, so the
 * Staffer screen fetches this before regenerating and warns about the assignments at
 * stake. Ids rather than a bare count, because the screen subtracts the matches the
 * user has locked — those keep their referee through the run.
 *
 * <p>Deliberately not the cast itself: the warning only needs to know how much would be
 * lost, not who is assigned to what.
 */
public record StaffingOverwriteDto(
        short queue,
        List<Long> assignedMatchIds
) {}
