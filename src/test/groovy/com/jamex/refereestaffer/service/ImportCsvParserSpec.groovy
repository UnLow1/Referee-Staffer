package com.jamex.refereestaffer.service

import com.jamex.refereestaffer.model.exception.ImportException
import org.springframework.mock.web.MockMultipartFile
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.time.LocalDateTime

class ImportCsvParserSpec extends Specification {

    static final String HEADER = "Kolejka;Gospodarze;Goście;Data;Sędzia główny;Wynik gospodarze;Wynik goście;Ocena"

    def "should parse a fully populated row"() {
        when:
        def rows = parse("$HEADER\n3;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;8.3")

        then:
        rows.size() == 1
        with(rows[0]) {
            rowNumber == 2
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
        description             | row
        "trailing delimiters"  | "1;Team 1;Team 2;15.08.2018 17:00;;;;"
        "only required cells"  | "1;Team 1;Team 2;15.08.2018 17:00"
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
        "1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;8.3/"         || 'grade must be "8.3" or "7.9/8.3" but was "8.3/"'
        "1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;/8.3"         || 'grade must be "8.3" or "7.9/8.3" but was "/8.3"'
        "1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;7.9/8.3/8.5"  || 'grade must be "8.3" or "7.9/8.3" but was "7.9/8.3/8.5"'
        "1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;abc"          || 'grade must be "8.3" or "7.9/8.3" but was "abc"'
        "1;Team 1;Team 2"                                                || "expected at least 4 columns (queue, home team, away team, date) but got 3"
        "1;Team 1;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;8.3;extra"    || "expected at most 8 columns but got 9"
    }

    def "should fail with an ImportException instead of an IO error when the CSV quoting is broken"() {
        when:
        parse("$HEADER\n1;\"Team 1\" oops;Team 2;15.08.2018 17:00;Jan Kowalski;2;1;8.3")

        then:
        def e = thrown(ImportException)
        e.message.startsWith(String.format(ImportException.ERROR_MESSAGE, "import.csv"))
    }

    private static List<ImportRow> parse(String csv) {
        ImportCsvParser.parse(new MockMultipartFile("file", "import.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)))
    }
}
