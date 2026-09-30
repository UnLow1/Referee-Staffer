package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.entity.Grade;
import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Referee;
import com.jamex.refereestaffer.model.entity.Team;
import com.jamex.refereestaffer.model.exception.RefereeNotFoundException;
import com.jamex.refereestaffer.model.exception.TeamNotFoundException;
import com.jamex.refereestaffer.model.request.ImportResponse;
import com.jamex.refereestaffer.repository.GradeRepository;
import com.jamex.refereestaffer.repository.MatchRepository;
import com.jamex.refereestaffer.repository.RefereeRepository;
import com.jamex.refereestaffer.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashSet;
import java.util.List;

@Service
public class ImporterService {

    private static final Logger log = LoggerFactory.getLogger(ImporterService.class);

    private static final String CREATED = "Created ";

    private final TeamRepository teamRepository;
    private final RefereeRepository refereeRepository;
    private final MatchRepository matchRepository;
    private final GradeRepository gradeRepository;

    public ImporterService(TeamRepository teamRepository, RefereeRepository refereeRepository,
                           MatchRepository matchRepository, GradeRepository gradeRepository) {
        this.teamRepository = teamRepository;
        this.refereeRepository = refereeRepository;
        this.matchRepository = matchRepository;
        this.gradeRepository = gradeRepository;
    }

    /**
     * Imports a whole season CSV. The file is parsed and validated up front by
     * {@link ImportCsvParser} — any format problem fails the import with a 400 naming the offending
     * row, before a single entity is written. Referees and teams are created from every row;
     * results and grades are only taken from queues up to {@code numberOfQueuesToImport}, so a file
     * covering a full season can be imported as a partially played one.
     */
    @Transactional
    public ImportResponse importData(MultipartFile file, Short numberOfQueuesToImport) {
        var rows = ImportCsvParser.parse(file);

        createTeams(rows);
        createReferees(rows);
        createMatchesAndGrades(rows, numberOfQueuesToImport);

        var noOfMatches = matchRepository.findAll().size();
        var noOfReferees = refereeRepository.findAll().size();
        var noOfGrades = gradeRepository.findAll().size();
        var noOfTeams = teamRepository.findAll().size();

        return new ImportResponse(noOfMatches, noOfReferees, noOfGrades, noOfTeams);
    }

    private void createMatchesAndGrades(List<ImportRow> rows, Short numberOfQueuesToImport) {
        for (var row : rows) {
            var homeTeamName = row.homeTeamName();
            var homeTeam = teamRepository.findByName(homeTeamName)
                    .orElseThrow(() -> new TeamNotFoundException(homeTeamName));
            var awayTeamName = row.awayTeamName();
            var awayTeam = teamRepository.findByName(awayTeamName)
                    .orElseThrow(() -> new TeamNotFoundException(awayTeamName));

            // Queues past the requested count are imported as fixtures only — no referee, no
            // result, no grade — even when the file already carries them.
            var imported = row.queue() <= numberOfQueuesToImport;

            Referee referee = null;
            if (imported && row.hasReferee()) {
                var name = row.referee();
                referee = refereeRepository.findByFirstNameAndLastName(name.firstName(), name.lastName())
                        .orElseThrow(() -> new RefereeNotFoundException(name.firstName(), name.lastName()));
            }
            var homeTeamScore = imported && row.hasResult() ? row.homeTeamScore() : null;
            var awayTeamScore = imported && row.hasResult() ? row.awayTeamScore() : null;

            var match = new Match(row.queue(), homeTeam, awayTeam, row.date(), referee, homeTeamScore, awayTeamScore);
            matchRepository.save(match);

            if (imported && row.hasGrade()) {
                gradeRepository.save(new Grade(match, row.gradeValue(), row.gradeSecondValue()));
            }
        }
        var grades = gradeRepository.findAll();
        var matches = matchRepository.findAll();
        log.info(CREATED + grades.size() + " grades");
        log.info(CREATED + matches.size() + " matches");
    }

    private void createReferees(List<ImportRow> rows) {
        var referees = rows.stream()
                .filter(ImportRow::hasReferee)
                .map(ImportRow::referee)
                .distinct()
                .map(name -> new Referee(name.firstName(), name.lastName()))
                .toList();

        refereeRepository.saveAll(referees);
        log.info(CREATED + referees.size() + " referees");
    }

    private void createTeams(List<ImportRow> rows) {
        // LinkedHashSet so the teams are created in the order the file lists them, which keeps
        // generated ids stable for a given file instead of depending on hash order.
        var teamNames = new LinkedHashSet<String>();
        for (var row : rows) {
            teamNames.add(row.homeTeamName());
            teamNames.add(row.awayTeamName());
        }

        var teams = teamNames.stream()
                .map(Team::new)
                .toList();

        teamRepository.saveAll(teams);
        log.info(CREATED + teams.size() + " teams");
    }
}
