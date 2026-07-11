package com.jamex.refereestaffer.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * The computed league table served by {@code GET /api/teams/standings}. Replaces the
 * old {@code Collection<TeamDto>} response so the frontend no longer has to derive
 * P/W/D/L/GF/GA client-side from the full match list.
 *
 * {@code afterQueue} is the highest queue with at least one finished match ("table
 * after queue N" in the UI), or {@code null} before the season starts.
 */
public record StandingsDto(
        Short afterQueue,
        List<Row> rows
) {
    /**
     * One table row. Carries the same team fields as {@link TeamDto} (so team pills
     * render from a row directly) plus the season stats. `place` is 1-based table
     * position — teams without a finished match sort to the bottom with zeroed stats.
     */
    public record Row(
            Long id,
            String name,
            String city,
            @JsonProperty("short") String shortCode,
            short points,
            short place,
            short played,
            short wins,
            short draws,
            short losses,
            short goalsFor,
            short goalsAgainst
    ) {}
}
