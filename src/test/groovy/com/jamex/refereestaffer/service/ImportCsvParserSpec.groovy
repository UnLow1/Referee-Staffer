package com.jamex.refereestaffer.service

import com.jamex.refereestaffer.model.exception.ImportException
import org.springframework.mock.web.MockMultipartFile
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.time.LocalDateTime

class ImportCsvParserSpec extends Specification {

    static final String HEADER = "Kolejka;Gospodarze;Goście;Data;Sędzia główny;Wynik gospodarze;Wynik goście;Ocena"

    static final String GRADE_HINT = 'grade must be a number between 0 and 10, on its own ("8.3") ' +
            'or as a split observer grade ("7.9/8.3"), but was'

    def "should parse a fully populated row"() {
        when:
        def rows = parse("$HEADER\n3;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;8.3")

        then:
        rows.size() == 1
        with(rows[0]) {
            queue == 3 as short
            homeTeamName == "Team 1"
            awayTeamName == "Team 2"
            date == LocalDateTime.of(2018, 8, 15, 17, 0)
            referee == new ImportRow.RefereeName("Jan", "Kowalski")
            homeTeamScore == 2 as short
            awayTeamScore == 1 as short
            gradeValue == 8.3d
            gradeSecondValue == null
            hasReferee()
            hasResult()
            hasGrade()
        }
    }

    def "should keep a semicolon inside a quoted team name instead of shifting the columns"() {
        when:
        def rows = parse("$HEADER\n1;\"Widzew; Łódź\";Team 2;15.08.2018 17:00;Jan Kowalski;2;1;8.3")

        then:
        rows[0].homeTeamName == "Widzew; Łódź"
        rows[0].awayTeamName == "Team 2"
        rows[0].date == LocalDateTime.of(2018, 8, 15, 17, 0)
        rows[0].gradeValue == 8.3d
    }

    def "should unescape a doubled quote inside a quoted cell"() {
        when:
        def rows = parse("$HEADER\n1;\"Team \"\"A\"\"\";Team 2;15.08.2018 17:00;Jan Kowalski;2;1;8.3")

        then:
        rows[0].homeTeamName == 'Team "A"'
    }

    def "should skip a leading byte order mark and keep diacritics"() {
        given:
        def csv = "\uFEFF$HEADER\n1;Górnik;Śląsk;15.08.2018 17:00;Sędzia 1;2;1;8.3"

        when:
        def rows = parse(csv)

        then:
        rows.size() == 1
        rows[0].queue == 1 as short
        rows[0].homeTeamName == "Górnik"
        rows[0].awayTeamName == "Śląsk"
        rows[0].referee == new ImportRow.RefereeName("Sędzia", "1")
    }

    def "should strip surrounding whitespace from cells"() {
        when:
        def rows = parse("$HEADER\n 1 ;  Team 1  ;Team 2\t;  15.08.2018 17:00 ; Jan Kowalski ; 2 ; 1 ; 8.3 ")

        then:
        rows[0].queue == 1 as short
        rows[0].homeTeamName == "Team 1"
        rows[0].awayTeamName == "Team 2"
        rows[0].date == LocalDateTime.of(2018, 8, 15, 17, 0)
        rows[0].referee == new ImportRow.RefereeName("Jan", "Kowalski")
        rows[0].homeTeamScore == 2 as short
        rows[0].gradeValue == 8.3d
    }

    def "should read a split observer grade as two components"() {
        when:
        def rows = parse("$HEADER\n1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;7.9/8.3")

        then:
        rows[0].gradeValue == 7.9d
        rows[0].gradeSecondValue == 8.3d
    }

    def "should treat blank optional cells as absent"() {
        when:
        def rows = parse("$HEADER\n$row")

        then:
        rows.size() == 1
        !rows[0].hasReferee()
        !rows[0].hasResult()
        !rows[0].hasGrade()
        rows[0].homeTeamScore == null
        rows[0].awayTeamScore == null

        where:
        description            | row
        "trailing delimiters" | "1;Team 1;Team 2;15.08.2018 17:00;;;;"
        "only required cells" | "1;Team 1;Team 2;15.08.2018 17:00"
    }

    def "should keep a referee on a row that has no result yet"() {
        when:
        def rows = parse("$HEADER\n1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;;;")

        then:
        rows[0].referee == new ImportRow.RefereeName("Jan", "Kowalski")
        !rows[0].hasResult()
        !rows[0].hasGrade()
    }

    def "should accept a row whose surplus columns are blank"() {
        when:
        def rows = parse("$HEADER\n1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;8.3;;")

        then:
        rows[0].gradeValue == 8.3d
    }

    def "should skip blank lines and handle CRLF endings"() {
        when:
        def rows = parse("$HEADER\r\n1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;8.3\r\n\r\n" +
                "2;Team 2;Team 1;22.08.2018 16:00;Jan Kowalski;0;0;8.1\r\n")

        then:
        rows.size() == 2
        rows[1].queue == 2 as short
        rows[1].gradeValue == 8.1d
    }

    def "should return no rows for an empty file and for a header-only file"() {
        expect:
        parse(csv).isEmpty()

        where:
        csv << ["", HEADER, "$HEADER\n"]
    }

    def "should report the offending row number and column when a cell is invalid"() {
        when:
        parse("$HEADER\n1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;8.3\n$row")

        then:
        def e = thrown(ImportException)
        e.message == String.format(ImportException.ROW_ERROR_MESSAGE, "import.csv", 3L, detail)

        where:
        row                                                              || detail
        "x;Team 1;Team 2;15.08.2018 17:00"                               || 'queue must be a number but was "x"'
        ";Team 1;Team 2;15.08.2018 17:00"                                || "queue must not be empty"
        "1;;Team 2;15.08.2018 17:00"                                     || "home team must not be empty"
        "1;Team 1;;15.08.2018 17:00"                                     || "away team must not be empty"
        "1;Team 1;Team 2;"                                               || "date must not be empty"
        "1;Team 1;Team 2;NOTADATE"                                       || 'date must match dd.MM.yyyy HH:mm but was "NOTADATE"'
        "1;Team 1;Team 2;15.08.2018;Jan Kowalski;2;1;8.3"                || 'date must match dd.MM.yyyy HH:mm but was "15.08.2018"'
        "1;Team 1;Team 2;15.08.2018 17:00;Kowalski;2;1;8.3"              || 'referee must be given as "<first name> <last name>" but was "Kowalski"'
        "1;Team 1;Team 2;15.08.2018 17:00;Jan Maria Kowalski;2;1;8.3"    || 'referee must be given as "<first name> <last name>" but was "Jan Maria Kowalski"'
        "1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;;8.3"           || "both team scores must be given or both left empty"
        "1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;;1;8.3"           || "both team scores must be given or both left empty"
        "1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;x;1;8.3"          || 'home team score must be a number but was "x"'
        "1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;x;8.3"          || 'away team score must be a number but was "x"'
        "1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;8.3/"         || "$GRADE_HINT \"8.3/\""
        "1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;/8.3"         || "$GRADE_HINT \"/8.3\""
        "1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;7.9/8.3/8.5"  || "$GRADE_HINT \"7.9/8.3/8.5\""
        "1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;abc"          || "$GRADE_HINT \"abc\""
        "1;Team 1;Team 2"                                                || "expected at least 4 columns (queue, home team, away team, date) but got 3"
        "1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;8.3;extra"    || "expected at most 8 columns but got 9"
    }

    def "should report the line and the reason when the CSV quoting is broken"() {
        when:
        parse("$HEADER\n$row")

        then:
        def e = thrown(ImportException)
        e.message == String.format(ImportException.ROW_ERROR_MESSAGE, "import.csv", 2L,
                "could not be read as CSV: $reason")

        where:
        description             | row                                                                   || reason
        "content after a quote" | "1;\"Team 1\" oops;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;8.3"       || "Invalid character between encapsulated token and delimiter at line: 2, position: 93"
        "unterminated quote"    | "1;\"Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;8.3"              || "(startline 2) EOF reached before encapsulated token finished"
    }

    def "should reject values that are numbers to Double or Short but not valid data"() {
        when:
        parse("$HEADER\n1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;8.3\n$row")

        then:
        def e = thrown(ImportException)
        e.message == String.format(ImportException.ROW_ERROR_MESSAGE, "import.csv", 3L, detail)

        where:
        row                                                            || detail
        // Double.parseDouble would take all of these; NaN is the dangerous one, because it reaches
        // RefereeService.countAverageGrade and poisons every score touching that referee
        "1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;NaN"        || "$GRADE_HINT \"NaN\""
        "1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;Infinity"   || "$GRADE_HINT \"Infinity\""
        "1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;1e400"      || "$GRADE_HINT \"1e400\""
        "1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;8.3d"       || "$GRADE_HINT \"8.3d\""
        "1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;0x1p3"      || "$GRADE_HINT \"0x1p3\""
        "1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;-5"         || "$GRADE_HINT \"-5\""
        "1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;10.1"       || "$GRADE_HINT \"10.1\""
        "1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;7.9/NaN"    || "$GRADE_HINT \"7.9/NaN\""
        // Short.parseShort would take a negative queue and negative scores
        "-3;Team 1;Team 2;15.08.2018 17:00"                            || "queue must be at least 1 but was -3"
        "0;Team 1;Team 2;15.08.2018 17:00"                             || "queue must be at least 1 but was 0"
        "1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;-1;2;8.3"       || "home team score must be at least 0 but was -1"
        "1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;-2;8.3"       || "away team score must be at least 0 but was -2"
    }

    def "should reject a result or grade on a row with no referee"() {
        when:
        parse("$HEADER\n1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;8.3\n$row")

        then:
        def e = thrown(ImportException)
        e.message == String.format(ImportException.ROW_ERROR_MESSAGE, "import.csv", 3L,
                "a result or grade requires a referee")

        where:
        description        | row
        "result only"     | "1;Team 1;Team 2;15.08.2018 17:00;;2;1;"
        "grade only"      | "1;Team 1;Team 2;15.08.2018 17:00;;;;8.3"
        "result and grade"| "1;Team 1;Team 2;15.08.2018 17:00;;2;1;8.3"
    }

    def "should reject a file whose first row is data rather than a header"() {
        when:
        parse(csv)

        then:
        def e = thrown(ImportException)
        e.message == String.format(ImportException.ROW_ERROR_MESSAGE, "import.csv", 1L,
                'the first row must be a header, but "1" looks like data')

        where:
        // A headerless export would otherwise silently lose its first match, and a single-row
        // headerless file would report a successful import of nothing
        csv << ["1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;8.3",
                "1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;8.3\n2;Team 2;Team 1;22.08.2018 16:00;Jan Kowalski;0;0;8.1"]
    }

    def "should truncate a long cell in the error message"() {
        given:
        def longCell = "x" * 200

        when:
        parse("$HEADER\n1;Team 1;Team 2;$longCell")

        then:
        def e = thrown(ImportException)
        e.message == String.format(ImportException.ROW_ERROR_MESSAGE, "import.csv", 2L,
                "date must match dd.MM.yyyy HH:mm but was \"${"x" * 50}…\"")
        e.message.length() < 200
    }

    def "should accept the boundary grades"() {
        when:
        def rows = parse("$HEADER\n1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;0;0;$grade")

        then:
        rows[0].gradeValue == expected

        where:
        grade || expected
        "0"   || 0d
        "10"  || 10d
        "10.0"|| 10d
        "8"   || 8d
    }

    private static List<ImportRow> parse(String csv) {
        ImportCsvParser.parse(new MockMultipartFile("file", "import.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)))
    }
}
