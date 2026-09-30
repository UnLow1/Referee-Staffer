package com.jamex.refereestaffer.service;

import java.time.LocalDateTime;

/**
 * One validated data row of an import CSV, produced by {@link ImportCsvParser}.
 *
 * <p>Parsing and persistence used to be interleaved: every helper in {@link ImporterService}
 * re-split the same raw line, so a malformed cell surfaced as an
 * {@code ArrayIndexOutOfBoundsException} from whichever helper happened to touch it first. The
 * parser now converts each record once and the service only reads typed values off this row.
 *
 * <p>Optional cells are {@code null} rather than empty strings, so
 * {@code "31;1;2;01.01.2025 12:00;;;;"} (a scheduled, not-yet-played match) is a valid row with no
 * referee, no result and no grade. The row deliberately carries no CSV line number: by construction
 * every parse failure is raised before the row exists, so a line number here would be a field
 * nothing reads.
 */
public record ImportRow(short queue,
                        String homeTeamName,
                        String awayTeamName,
                        LocalDateTime date,
                        RefereeName referee,
                        Short homeTeamScore,
                        Short awayTeamScore,
                        Double gradeValue,
                        Double gradeSecondValue) {

    /**
     * Referee identity as the CSV carries it — a single "first last" cell. It is a record so
     * {@code distinct()} deduplicates referees across rows without string juggling.
     */
    public record RefereeName(String firstName, String lastName) {
    }

    public boolean hasReferee() {
        return referee != null;
    }

    public boolean hasResult() {
        return homeTeamScore != null && awayTeamScore != null;
    }

    public boolean hasGrade() {
        return gradeValue != null;
    }
}
