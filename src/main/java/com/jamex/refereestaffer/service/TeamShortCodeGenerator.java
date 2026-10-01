package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.entity.Team;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Hands out distinct short codes for a batch of team names, so a CSV import can populate
 * {@code short_code} instead of leaving every team on the name-derived fallback.
 *
 * <p>Scope is the batch, not the whole table: one instance is created per import and
 * remembers only the codes it issued. Collisions against teams already stored are left to
 * the manual override in the team form — resolving them here would mean guessing which of
 * the two teams should keep the prefix.
 *
 * <p>The candidate ladder is deterministic, so the same CSV always yields the same codes:
 * <ol>
 *     <li>{@link Team#deriveShortCode(String)} — the plain 3-character prefix;</li>
 *     <li>that prefix with its last character swapped for each later character of the name
 *     in turn ("Lechia" → LEC, then LEH, LEI, LEA);</li>
 *     <li>the prefix with a numeric suffix (LEC2 … LEC99) as a last resort.</li>
 * </ol>
 */
class TeamShortCodeGenerator {

    /** Upper bound for the numeric-suffix tail of the ladder; keeps codes inside the 8-char column. */
    private static final int MAX_NUMERIC_SUFFIX = 99;

    private final Set<String> issued = new HashSet<>();

    /**
     * Returns a code not yet issued by this instance, or {@code null} when the name has no
     * alphanumeric character to build one from or the whole ladder is exhausted. A
     * {@code null} leaves the team on the name-derived fallback, which is what callers want:
     * a missing override is always better than a wrong one.
     */
    String generate(String name) {
        for (var candidate : candidates(name)) {
            if (issued.add(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static List<String> candidates(String name) {
        var alphanumeric = Team.alphanumericUpperCase(name);
        if (alphanumeric.isEmpty()) {
            return List.of();
        }
        var prefix = Team.deriveShortCode(name);
        var candidates = new ArrayList<String>();
        candidates.add(prefix);

        // Swap the prefix's last character for each character the prefix did not consume.
        var stem = prefix.substring(0, prefix.length() - 1);
        for (var i = prefix.length(); i < alphanumeric.length(); i++) {
            candidates.add(stem + alphanumeric.charAt(i));
        }

        for (var suffix = 2; suffix <= MAX_NUMERIC_SUFFIX; suffix++) {
            candidates.add(prefix + suffix);
        }
        return candidates;
    }
}
