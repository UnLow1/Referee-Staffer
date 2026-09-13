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
                match(1l, "Wisła", "Cracovia", LocalDateTime.of(2026, 3, 1, 12, 30), referee("Sędzia", "Główny")),
                match(2l, "Lech", "Warta", LocalDateTime.of(2026, 3, 2, 17, 0), null)
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
        text.contains("01.03.2026 12:30")
        text.contains("Sędzia Główny")
        text.contains("Lech")
        text.contains("Warta")
        text.contains(AssignmentPdfService.UNASSIGNED)
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

    private static Match match(Long id, String homeName, String awayName, LocalDateTime date, Referee referee) {
        Match.builder()
                .id(id)
                .queue(3 as Short)
                .home(Team.builder().name(homeName).build())
                .away(Team.builder().name(awayName).build())
                .date(date)
                .referee(referee)
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
