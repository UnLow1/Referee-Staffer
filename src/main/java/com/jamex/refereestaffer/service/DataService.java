package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.dto.ClearDataSummaryDto;
import com.jamex.refereestaffer.repository.GradeRepository;
import com.jamex.refereestaffer.repository.MatchRepository;
import com.jamex.refereestaffer.repository.RefereeRepository;
import com.jamex.refereestaffer.repository.TeamRepository;
import com.jamex.refereestaffer.repository.VacationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Wipes all domain data in one transaction (RS-112). Replaces the five per-entity
 * {@code DELETE /api/{entity}} endpoints, which called {@code repository.deleteAll()}
 * in isolation and therefore could not work: whichever one a client called first hit
 * either a foreign key (match → referee/team) or, for the grade/match pair, a
 * flush-time transient-reference check on the bidirectional one-to-one.
 */
@Service
public class DataService {

    private static final Logger log = LoggerFactory.getLogger(DataService.class);

    private final GradeRepository gradeRepository;
    private final VacationRepository vacationRepository;
    private final MatchRepository matchRepository;
    private final RefereeRepository refereeRepository;
    private final TeamRepository teamRepository;

    public DataService(GradeRepository gradeRepository, VacationRepository vacationRepository,
                       MatchRepository matchRepository, RefereeRepository refereeRepository,
                       TeamRepository teamRepository) {
        this.gradeRepository = gradeRepository;
        this.vacationRepository = vacationRepository;
        this.matchRepository = matchRepository;
        this.refereeRepository = refereeRepository;
        this.teamRepository = teamRepository;
    }

    /**
     * Deletes every domain row: grades, vacations, matches, referees, teams — in that
     * order, which is the only one the foreign keys allow (grade → match,
     * vacation → referee, match → referee/team).
     *
     * <p>Configuration rows ({@code config}) are deliberately left alone: they are the
     * algorithm's tuning parameters seeded by {@code data.sql}, not user data, and
     * staffing breaks without them.
     *
     * <p>{@code deleteAllInBatch()} rather than {@code deleteAll()} on purpose. The batch
     * variant issues a plain {@code delete from <table>} instead of loading every row into
     * the persistence context first — which is exactly what made the old endpoints fail:
     * loading a Grade pulls in its Match through the eager one-to-one (and vice versa), and
     * the flush-time check then sees a managed entity pointing at a removed one.
     */
    @Transactional
    public ClearDataSummaryDto clearAllData() {
        var summary = new ClearDataSummaryDto(
                gradeRepository.count(),
                vacationRepository.count(),
                matchRepository.count(),
                refereeRepository.count(),
                teamRepository.count());

        gradeRepository.deleteAllInBatch();
        vacationRepository.deleteAllInBatch();
        matchRepository.deleteAllInBatch();
        refereeRepository.deleteAllInBatch();
        teamRepository.deleteAllInBatch();

        log.info("Cleared all domain data: {} grades, {} vacations, {} matches, {} referees, {} teams",
                summary.grades(), summary.vacations(), summary.matches(), summary.referees(), summary.teams());
        return summary;
    }
}
