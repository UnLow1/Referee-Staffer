package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Team;
import com.jamex.refereestaffer.model.exception.MatchNotFoundException;
import com.jamex.refereestaffer.repository.MatchRepository;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Renders the referee assignment sheet for a queue as a PDF — the printable/mailable
 * counterpart of the Staffer screen's cast table. Reads whatever is persisted, so the
 * sheet reflects the last saved cast, not unsaved edits in the UI.
 */
@Service
public class AssignmentPdfService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentPdfService.class);

    static final String UNASSIGNED = "unassigned";
    /** Printed when the home team has no venue stored — the importer never fills one in. */
    static final String UNKNOWN_VENUE = "-";

    /** Matches the date format of the CSV importer, so sheets read like the source data. */
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    /** Kick-off is split across two columns, the way published assignment sheets print it. */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Color HEADER_BACKGROUND = new Color(240, 240, 240);
    private static final Color MUTED_TEXT = new Color(120, 120, 120);

    private final MatchRepository matchRepository;

    public AssignmentPdfService(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    public byte[] generateAssignmentsPdf(short queue) {
        var matches = matchRepository.findAllByQueueOrderByDateAsc(queue);
        if (matches.isEmpty()) {
            throw new MatchNotFoundException(queue);
        }
        log.info("Rendering assignment PDF for queue {} with {} matches", queue, matches.size());
        return render(queue, matches);
    }

    private byte[] render(short queue, List<Match> matches) {
        try {
            // Cp1250 (Central European) instead of the default Cp1252 — team and referee
            // names imported from the CSV carry Polish diacritics.
            var baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1250, BaseFont.NOT_EMBEDDED);
            var boldFont = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1250, BaseFont.NOT_EMBEDDED);
            var title = new Font(boldFont, 16);
            var subtitle = new Font(baseFont, 9, Font.NORMAL, MUTED_TEXT);
            var tableHeader = new Font(boldFont, 10);
            var cell = new Font(baseFont, 10);
            var mutedCell = new Font(baseFont, 10, Font.ITALIC, MUTED_TEXT);

            var out = new ByteArrayOutputStream();
            // Landscape: the venue column carries a full object name plus street address,
            // which squeezes the six columns unreadably on portrait A4.
            var document = new Document(PageSize.A4.rotate(), 36, 36, 42, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            var heading = new Paragraph("Referee assignments - Queue " + queue, title);
            document.add(heading);
            var generatedAt = new Paragraph("Generated on " + LocalDateTime.now().format(DATE_TIME_FORMAT), subtitle);
            generatedAt.setSpacingAfter(14);
            document.add(generatedAt);

            var table = new PdfPTable(new float[]{2.6f, 2.6f, 4.4f, 1.6f, 1f, 2.6f});
            table.setWidthPercentage(100);
            table.setHeaderRows(1);
            for (var header : List.of("Home", "Away", "Venue", "Date", "Time", "Referee")) {
                var headerCell = new PdfPCell(new Phrase(header, tableHeader));
                headerCell.setBackgroundColor(HEADER_BACKGROUND);
                headerCell.setPadding(6);
                table.addCell(headerCell);
            }

            for (var match : matches) {
                table.addCell(bodyCell(teamName(match.getHome()), cell));
                table.addCell(bodyCell(teamName(match.getAway()), cell));
                // The venue follows the home team - there is no per-match override yet.
                var venue = venue(match.getHome());
                table.addCell(venue == null
                        ? bodyCell(UNKNOWN_VENUE, mutedCell)
                        : bodyCell(venue, cell));
                table.addCell(bodyCell(match.getDate().format(DATE_FORMAT), cell));
                table.addCell(bodyCell(match.getDate().format(TIME_FORMAT), cell));
                var referee = match.getReferee();
                table.addCell(referee == null
                        ? bodyCell(UNASSIGNED, mutedCell)
                        : bodyCell(referee.getFirstName() + " " + referee.getLastName(), cell));
            }
            document.add(table);
            document.close();

            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            // BaseFont.createFont declares IOException, but built-in fonts never touch disk —
            // any failure here is a programming error, not a recoverable condition.
            throw new IllegalStateException("Failed to render assignment PDF for queue " + queue, e);
        }
    }

    private PdfPCell bodyCell(String text, Font font) {
        var pdfCell = new PdfPCell(new Phrase(text, font));
        pdfCell.setPadding(6);
        pdfCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return pdfCell;
    }

    private String teamName(Team team) {
        return team == null ? "-" : team.getName();
    }

    /**
     * Renders the home team's ground the way published assignment sheets print it:
     * {@code "Stadion Miejski (Krakow, sw. Andrzeja 1)"}. Either half may be missing, so
     * a team with only one of them still gets a usable cell; {@code null} means neither
     * is stored and the caller prints {@link #UNKNOWN_VENUE} instead.
     */
    private String venue(Team home) {
        if (home == null) {
            return null;
        }
        var name = blankToNull(home.getVenueName());
        var address = blankToNull(home.getVenueAddress());
        if (name != null && address != null) {
            return name + " (" + address + ")";
        }
        return name != null ? name : address;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
