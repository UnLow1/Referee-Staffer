package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.exception.ImportException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Turns an uploaded season CSV into validated {@link ImportRow}s.
 *
 * <p>Replaces the hand-rolled {@code line.split(";")} the importer used to do (RS-77). That split
 * had no notion of quoting, so a single team name containing a semicolon shifted every following
 * column, and it silently dropped trailing empty cells, which made "row has 7 columns" and "row has
 * 8 columns with an empty grade" indistinguishable. Apache Commons CSV handles quoting and escaping;
 * this class adds the format validation on top and reports every failure as an
 * {@link ImportException} naming the offending row and column instead of letting an
 * {@code ArrayIndexOutOfBoundsException} bubble up from deep inside the service.
 *
 * <p>Commons CSV was picked over OpenCSV because it has no transitive dependencies and the importer
 * only needs positional record access, not bean binding.
 */
final class ImportCsvParser {

    private static final String DATE_FORMAT = "dd.MM.yyyy HH:mm";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    private static final int QUEUE = 0;
    private static final int HOME_TEAM = 1;
    private static final int AWAY_TEAM = 2;
    private static final int DATE = 3;
    private static final int REFEREE = 4;
    private static final int HOME_TEAM_SCORE = 5;
    private static final int AWAY_TEAM_SCORE = 6;
    private static final int GRADE = 7;

    /** Queue, both team names and the date are mandatory; everything past them is optional. */
    private static final int REQUIRED_COLUMNS = 4;
    private static final int MAX_COLUMNS = 8;

    /** An observer mark is a plain decimal on a 0-10 scale — see {@link #parseGradeComponent}. */
    private static final Pattern GRADE_COMPONENT = Pattern.compile("\\d+(\\.\\d+)?");
    private static final double MAX_GRADE = 10;

    /** Longest raw cell echoed back in an error message, so a 1 MB upload cannot become a 1 MB 400. */
    private static final int MAX_QUOTED_LENGTH = 50;

    private static final String GRADE_FORMAT_HINT = "grade must be a number between 0 and " + (int) MAX_GRADE
            + ", on its own (\"8.3\") or as a split observer grade (\"7.9/8.3\"), but was ";

    /**
     * {@code CSVFormat.DEFAULT} already skips blank lines and treats {@code "} as the quote
     * character; only the delimiter differs from the default. Because blank lines are skipped,
     * {@code rowNumber} in error messages counts CSV records (row 1 being the header) — for the
     * files this app actually sees that is also the physical line number. Surrounding spaces are
     * stripped explicitly in {@link #cell} instead of via {@code setIgnoreSurroundingSpaces},
     * because that option leaves tabs in place.
     */
    private static final CSVFormat FORMAT = CSVFormat.Builder.create(CSVFormat.DEFAULT)
            .setDelimiter(';')
            .get();

    private static final int BOM = 0xFEFF;

    private ImportCsvParser() {
    }

    static List<ImportRow> parse(MultipartFile file) {
        var filename = file.getOriginalFilename();
        try (var reader = openReader(file); var parser = FORMAT.parse(reader)) {
            return readRows(filename, parser);
        } catch (IOException e) {
            throw new ImportException(filename, e);
        }
    }

    private static List<ImportRow> readRows(String filename, CSVParser parser) {
        var rows = new ArrayList<ImportRow>();
        try {
            for (var record : parser) {
                if (record.getRecordNumber() == 1) {
                    requireHeader(filename, record);
                    continue;
                }
                rows.add(toRow(filename, record));
            }
        } catch (UncheckedIOException | IllegalStateException e) {
            // Commons CSV surfaces a mid-iteration IOException (content after a closing quote, an
            // unterminated quoted cell) wrapped in one of these. Its own message carries the
            // position, so it is folded into the detail rather than dropped — otherwise the most
            // likely real-world CSV defect would be the one failure with no explanation.
            var cause = e.getCause();
            if (!(cause instanceof IOException)) {
                throw e;
            }
            throw new ImportException(filename, parser.getCurrentLineNumber(),
                    "could not be read as CSV: " + cause.getMessage());
        }
        return rows;
    }

    /**
     * The first record is the header and is dropped, as it always was. It is sanity-checked first,
     * because dropping a data row instead would silently lose a match — a headerless export would
     * import one fewer match than it contains, and a single-row headerless file would report a
     * successful import of nothing.
     */
    private static void requireHeader(String filename, CSVRecord record) {
        var firstCell = cell(record, QUEUE);
        if (firstCell != null && GRADE_COMPONENT.matcher(firstCell).matches()) {
            throw new ImportException(filename, record.getRecordNumber(),
                    "the first row must be a header, but " + quote(firstCell) + " looks like data");
        }
    }

    /**
     * Reads as UTF-8 (the platform default used before was only right by accident on JDK 18+) and
     * swallows a leading byte order mark, which spreadsheet exports routinely prepend — including
     * {@code data/import data file.csv}.
     */
    private static Reader openReader(MultipartFile file) throws IOException {
        var reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
        try {
            reader.mark(1);
            if (reader.read() != BOM) {
                reader.reset();
            }
            return reader;
        } catch (IOException e) {
            // Nothing has registered the reader with a try-with-resources yet, so close it here or
            // the multipart stream leaks.
            reader.close();
            throw e;
        }
    }

    private static ImportRow toRow(String filename, CSVRecord record) {
        var row = record.getRecordNumber();
        if (record.size() < REQUIRED_COLUMNS) {
            throw new ImportException(filename, row, "expected at least " + REQUIRED_COLUMNS
                    + " columns (queue, home team, away team, date) but got " + record.size());
        }
        // Spreadsheet exports like to pad rows with trailing delimiters, so surplus columns are
        // tolerated as long as they carry nothing — a surplus column with content means the row is
        // genuinely misaligned and the values below would be read from the wrong cells.
        for (var index = MAX_COLUMNS; index < record.size(); index++) {
            if (cell(record, index) != null) {
                throw new ImportException(filename, row, "expected at most " + MAX_COLUMNS
                        + " columns but got " + record.size());
            }
        }

        var queue = parseShort(filename, row, "queue", required(filename, row, "queue", record, QUEUE), 1);
        var homeTeamName = required(filename, row, "home team", record, HOME_TEAM);
        var awayTeamName = required(filename, row, "away team", record, AWAY_TEAM);
        var date = parseDate(filename, row, required(filename, row, "date", record, DATE));
        var referee = parseReferee(filename, row, cell(record, REFEREE));

        var rawHomeScore = cell(record, HOME_TEAM_SCORE);
        var rawAwayScore = cell(record, AWAY_TEAM_SCORE);
        var rawGrade = cell(record, GRADE);
        if ((rawHomeScore == null) != (rawAwayScore == null)) {
            throw new ImportException(filename, row, "both team scores must be given or both left empty");
        }
        // A grade marks a referee's performance and is only ever read back by walking a referee's
        // matches, so a result or grade without a referee would be write-only data inflating the
        // reported counters. The old parser rejected this shape too, by accident.
        if (referee == null && (rawHomeScore != null || rawGrade != null)) {
            throw new ImportException(filename, row, "a result or grade requires a referee");
        }
        var homeTeamScore = rawHomeScore == null ? null
                : parseShort(filename, row, "home team score", rawHomeScore, 0);
        var awayTeamScore = rawAwayScore == null ? null
                : parseShort(filename, row, "away team score", rawAwayScore, 0);

        var grade = parseGrade(filename, row, rawGrade);

        return new ImportRow(queue, homeTeamName, awayTeamName, date, referee,
                homeTeamScore, awayTeamScore, grade.value(), grade.secondValue());
    }

    /**
     * Splits the single referee cell into first and last name. Exactly two whitespace-separated
     * parts are required: the old code took {@code [0]} and {@code [1]} and silently dropped the
     * rest, and the "S C" central-referee sentinel (see {@code Referee.isCentralSentinel}) relies
     * on that two-part shape.
     */
    private static ImportRow.RefereeName parseReferee(String filename, long row, String rawReferee) {
        if (rawReferee == null) {
            return null;
        }
        var parts = rawReferee.split("\\s+");
        if (parts.length != 2) {
            throw new ImportException(filename, row,
                    "referee must be given as \"<first name> <last name>\" but was " + quote(rawReferee));
        }
        return new ImportRow.RefereeName(parts[0], parts[1]);
    }

    /**
     * Parses the grade cell into value plus optional second value, the second one only set for a
     * split observer grade ("7.9/8.3" — two components whose mean referee stats use). Returns an
     * empty pair for an empty cell.
     */
    private static ParsedGrade parseGrade(String filename, long row, String rawGrade) {
        if (rawGrade == null) {
            return new ParsedGrade(null, null);
        }
        // limit -1 keeps trailing empty strings, so a malformed "8.3/" is rejected here instead of
        // silently passing as the plain grade "8.3"
        var parts = rawGrade.split("/", -1);
        if (parts.length > 2) {
            throw new ImportException(filename, row, GRADE_FORMAT_HINT + quote(rawGrade));
        }
        var value = parseGradeComponent(filename, row, rawGrade, parts[0]);
        var secondValue = parts.length == 2 ? parseGradeComponent(filename, row, rawGrade, parts[1]) : null;
        return new ParsedGrade(value, secondValue);
    }

    /**
     * Accepts only a plain decimal on the 0-10 scale. {@code Double.parseDouble} alone would also
     * take {@code NaN}, {@code Infinity}, {@code 1e400} (which overflows to infinity), a {@code d}
     * or {@code f} suffix and hex float notation. {@code NaN} is the dangerous one: it reaches
     * {@code RefereeService.countAverageGrade} through {@code Grade.getEffectiveValue()} and from
     * there poisons every score that touches the referee, with nothing to point at the cause.
     */
    private static Double parseGradeComponent(String filename, long row, String rawGrade, String component) {
        if (!GRADE_COMPONENT.matcher(component).matches()) {
            throw new ImportException(filename, row, GRADE_FORMAT_HINT + quote(rawGrade));
        }
        var value = Double.parseDouble(component);
        if (value > MAX_GRADE) {
            throw new ImportException(filename, row, GRADE_FORMAT_HINT + quote(rawGrade));
        }
        return value;
    }

    private record ParsedGrade(Double value, Double secondValue) {
    }

    private static LocalDateTime parseDate(String filename, long row, String rawDate) {
        try {
            return LocalDateTime.parse(rawDate, FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ImportException(filename, row,
                    "date must match " + DATE_FORMAT + " but was " + quote(rawDate));
        }
    }

    /**
     * Parses a whole number, rejecting anything below {@code minimum}. A negative queue or score
     * used to pass straight through: {@code queue} drives the queue navigation across the app and
     * scores feed the standings arithmetic in {@code TeamService}, and this parser is the one gate
     * where such a rule belongs.
     */
    private static short parseShort(String filename, long row, String column, String rawValue, int minimum) {
        short value;
        try {
            value = Short.parseShort(rawValue);
        } catch (NumberFormatException e) {
            throw new ImportException(filename, row, column + " must be a number but was " + quote(rawValue));
        }
        if (value < minimum) {
            throw new ImportException(filename, row, column + " must be at least " + minimum + " but was " + value);
        }
        return value;
    }

    private static String required(String filename, long row, String column, CSVRecord record, int index) {
        var value = cell(record, index);
        if (value == null) {
            throw new ImportException(filename, row, column + " must not be empty");
        }
        return value;
    }

    /** Returns the stripped cell, or {@code null} when it is missing or blank. */
    private static String cell(CSVRecord record, int index) {
        if (index >= record.size()) {
            return null;
        }
        var value = record.get(index).strip();
        return value.isEmpty() ? null : value;
    }

    /** Quotes a raw cell for an error message, truncated so the 400 body stays a readable size. */
    private static String quote(String value) {
        var shown = value.length() <= MAX_QUOTED_LENGTH ? value : value.substring(0, MAX_QUOTED_LENGTH) + "…";
        return '"' + shown + '"';
    }
}
