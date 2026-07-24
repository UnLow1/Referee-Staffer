package com.jamex.refereestaffer.model.dto;

/**
 * Per-component breakdown of a match's hardness level. Mirrors the shape consumed by
 * the UI's Staffer drawer + Match detail panels:
 *
 * <pre>
 *   total = parts.base + parts.sameCity + parts.top + parts.bottom
 * </pre>
 *
 * `parts.top` and `parts.bottom` are mutually exclusive in the current implementation
 * — only one (or neither) can be non-zero per match.
 */
public record DifficultyBreakdownDto(
        Long matchId,
        double total,
        Parts parts,
        Flags flags
) {
    public record Parts(
            double base,
            double sameCity,
            double top,
            double bottom
    ) {}

    public record Flags(
            boolean sameCity,
            boolean isTop,
            boolean isBot,
            int pointsDiff
    ) {}
}
