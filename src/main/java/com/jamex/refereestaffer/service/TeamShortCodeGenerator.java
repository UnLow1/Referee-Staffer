package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.entity.Team;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Hands out distinct short codes for a batch of team names, so a CSV import can resolve
 * prefix collisions instead of leaving both teams on the same name-derived fallback.
 *
 * <p>Scope is the batch, not the whole table: one instance is created per import and
 * remembers only the codes it issued. Collisions against teams already stored are left to
 * the manual override in the team form — resolving them here would mean guessing which of
 * the two teams should keep the prefix.
 *
 * <p>The candidate ladder is deterministic, so the same CSV always yields the same codes:
 * <ol>
 *     <li>{@link Team#deriveShortCode(String)} — the plain 3-character prefix;</li>
 *     <li>the 4-character prefix, when the name is long enough ("Lechia" → LECH once "Lech"
 *     has taken LEC) — tried before any rearrangement because it still reads as the name;</li>
 *     <li>the 3-character prefix with its last character swapped for each later character of
 *     the name in turn (LEH, LEI, LEA);</li>
 *     <li>the prefix with a single-digit suffix (LEC2 … LEC9) as a last resort.</li>
 * </ol>
 *
 * <p>Every candidate therefore fits {@link #MAX_CODE_LENGTH} characters, which is what the
 * 22×22 team pill can render — the {@code short_code} column is wider, but the UI is not.
 */
class TeamShortCodeGenerator {

    /** Longest code the ladder may produce; keeps generated codes renderable in the pill. */
    static final int MAX_CODE_LENGTH = 4;

    private final Set<String> issued = new HashSet<>();

    /**
     * Returns a code not yet issued by this instance, or {@code null} when the name has no
     * alphanumeric character to build one from or the whole ladder is exhausted. A
     * {@code null} leaves the team on the name-derived fallback, which is what callers want:
     * a missing override is better than a wrong one.
     */
    String generate(String name) {
        // Lazily: the numeric tail is only evaluated once every readable candidate is taken.
        return candidates(name)
                .filter(issued::add)
                .findFirst()
                .orElse(null);
    }

    private static Stream<String> candidates(String name) {
        var alphanumeric = Team.alphanumericUpperCase(name);
        if (alphanumeric.isEmpty()) {
            return Stream.empty();
        }
        var prefix = Team.deriveShortCode(name);
        var longerPrefix = alphanumeric.codePointCount(0, alphanumeric.length()) > prefix.codePointCount(0, prefix.length())
                ? Stream.of(codePointPrefix(alphanumeric, MAX_CODE_LENGTH))
                : Stream.<String>empty();

        return Stream.of(Stream.of(prefix), longerPrefix, slides(alphanumeric, prefix), numericSuffixes(prefix))
                .flatMap(stream -> stream);
    }

    /** The prefix with its last character replaced by each character the prefix did not consume. */
    private static Stream<String> slides(String alphanumeric, String prefix) {
        var stem = prefix.substring(0, prefix.length() - Character.charCount(prefix.codePointBefore(prefix.length())));
        return alphanumeric.codePoints()
                .skip(prefix.codePointCount(0, prefix.length()))
                .mapToObj(codePoint -> stem + Character.toString(codePoint));
    }

    private static Stream<String> numericSuffixes(String prefix) {
        return IntStream.rangeClosed(2, 9).mapToObj(suffix -> prefix + suffix);
    }

    /** First {@code length} code points of {@code value} — never splits a surrogate pair. */
    private static String codePointPrefix(String value, int length) {
        var count = value.codePointCount(0, value.length());
        return count <= length ? value : value.substring(0, value.offsetByCodePoints(0, length));
    }
}
