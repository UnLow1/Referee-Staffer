package com.jamex.refereestaffer.service

import com.jamex.refereestaffer.model.entity.Match
import com.jamex.refereestaffer.model.entity.Referee
import com.jamex.refereestaffer.model.entity.Team
import com.jamex.refereestaffer.model.exception.MatchNotFoundException
import com.jamex.refereestaffer.repository.MatchRepository
import org.openpdf.text.pdf.PdfReader
import org.openpdf.text.pdf.parser.PdfTextExtractor
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDateTime

class AssignmentPdfServiceSpec extends Specification {

    MatchRepository matchRepository = Mock()

    @Subject
    AssignmentPdfService assignmentPdfService = new AssignmentPdfService(matchRepository)

    def "should render a PDF listing every match of the queue with its referee"() {
        given:
        def queue = 3 as short
        def matches = [
                match(1l, home("Wisła", "Stadion Miejski", "Krakow, Reymonta 22"), "Cracovia",
                        LocalDateTime.of(2026, 3, 1, 12, 30), referee("Sędzia", "Główny")),
                match(2l, home("Lech", null, null), "Warta",
                        LocalDateTime.of(2026, 3, 2, 17, 0), null)
        ]

        when:
        def pdf = assignmentPdfService.generateAssignmentsPdf(queue)

        then:
        1 * matchRepository.findAllByQueueOrderByDateAsc(queue) >> matches
        new String(pdf, 0, 5) == "%PDF-"
        def text = extractText(pdf)
        text.contains("Referee assignments - Queue 3")
        text.contains("Wisła")
        text.contains("Cracovia")
        !text.contains("#")
        // Column order is deliberate and matches published assignment sheets: who plays
        // whom, then when, then who referees it. Plain contains() would pass on any
        // order, so assert the header cells line up left to right.
        def header = text.readLines().find { it.contains("Referee") && it.contains("Home") }
        header.indexOf("Home") < header.indexOf("Away")
        header.indexOf("Away") < header.indexOf("Venue")
        header.indexOf("Venue") < header.indexOf("Date")
        header.indexOf("Date") < header.indexOf("Time")
        header.indexOf("Time") < header.indexOf("Referee")
        // Kick-off is split across the Date and Time columns, so the two halves land
        // separately rather than as one "01.03.2026 12:30" string.
        text.contains("01.03.2026")
        text.contains("12:30")
        text.contains("02.03.2026")
        text.contains("17:00")
        text.contains("Sędzia Główny")
        text.contains("Lech")
        text.contains("Warta")
        text.contains(AssignmentPdfService.UNASSIGNED)
    }

    def "should print the home team venue as name plus address"() {
        given:
        def queue = 3 as short
        def matches = [match(1l, home("Wisła", "Stadion Miejski", "Krakow, Reymonta 22"), "Cracovia",
                LocalDateTime.of(2026, 3, 1, 12, 30), null)]

        when:
        def pdf = assignmentPdfService.generateAssignmentsPdf(queue)

        then:
        1 * matchRepository.findAllByQueueOrderByDateAsc(queue) >> matches
        extractText(pdf).contains("Stadion Miejski (Krakow, Reymonta 22)")
    }

    def "should print only the half of the venue that is stored"() {
        given: "one team with just the object name, one with just the address"
        def queue = 3 as short
        def matches = [
                match(1l, home("Wisła", "Stadion Miejski", null), "Cracovia",
                        LocalDateTime.of(2026, 3, 1, 12, 30), null),
                match(2l, home("Lech", null, "Poznan, Bulgarska 17"), "Warta",
                        LocalDateTime.of(2026, 3, 2, 17, 0), null)
        ]

        when:
        def pdf = assignmentPdfService.generateAssignmentsPdf(queue)

        then:
        1 * matchRepository.findAllByQueueOrderByDateAsc(queue) >> matches
        def text = extractText(pdf)
        and: "no dangling parentheses for the missing half"
        text.contains("Stadion Miejski")
        !text.contains("Stadion Miejski (")
        text.contains("Poznan, Bulgarska 17")
        !text.contains("(Poznan, Bulgarska 17)")
    }

    def "should fall back to a placeholder when the home team has no venue"() {
        given: "a team as the CSV importer creates it — name only"
        def queue = 3 as short
        def matches = [match(1l, home("Lech", null, "   "), "Warta",
                LocalDateTime.of(2026, 3, 2, 17, 0), null)]

        when:
        def pdf = assignmentPdfService.generateAssignmentsPdf(queue)

        then:
        1 * matchRepository.findAllByQueueOrderByDateAsc(queue) >> matches
        and: "the placeholder sits in the venue column, between the away team and the date"
        def row = extractText(pdf).readLines().find { it.contains("Lech") }
        row.indexOf("Warta") < row.indexOf(AssignmentPdfService.UNKNOWN_VENUE)
        row.indexOf(AssignmentPdfService.UNKNOWN_VENUE) < row.indexOf("02.03.2026")
    }

    def "should throw when the queue has no matches"() {
        given:
        def queue = 44 as short

        when:
        assignmentPdfService.generateAssignmentsPdf(queue)

        then:
        1 * matchRepository.findAllByQueueOrderByDateAsc(queue) >> []
        def ex = thrown(MatchNotFoundException)
        ex.message == String.format(MatchNotFoundException.QUEUE_EMPTY, queue)
    }

    private static Match match(Long id, Team homeTeam, String awayName, LocalDateTime date, Referee referee) {
        Match.builder()
                .id(id)
                .queue(3 as Short)
                .home(homeTeam)
                .away(Team.builder().name(awayName).build())
                .date(date)
                .referee(referee)
                .build()
    }

    private static Team home(String name, String venueName, String venueAddress) {
        Team.builder()
                .name(name)
                .venueName(venueName)
                .venueAddress(venueAddress)
                .build()
    }

    private static Referee referee(String firstName, String lastName) {
        Referee.builder()
                .firstName(firstName)
                .lastName(lastName)
                .build()
    }

    /**
     * Beware when extending the diacritics assertions: PdfTextExtractor resolves a code
     * that also exists in the font's built-in encoding through that encoding, ignoring the
     * /Differences entry. Uppercase Ł is Cp1250 0xA3, which is "sterling" in the built-in
     * encoding, so it extracts as £ even though the PDF maps 163 to /Lslash and every
     * viewer renders it correctly. Lowercase ł (0xB3) has no such clash and round-trips.
     */
    private static String extractText(byte[] pdf) {
        def reader = new PdfReader(pdf)
        try {
            def extractor = new PdfTextExtractor(reader)
            (1..reader.numberOfPages)
                    .collect { extractor.getTextFromPage(it) }
                    .join("\n")
        } finally {
            reader.close()
        }
    }
}
