package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.entity.Grade;
import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Referee;
import com.jamex.refereestaffer.model.entity.Team;
import com.jamex.refereestaffer.model.exception.ImportException;
import com.jamex.refereestaffer.model.exception.RefereeNotFoundException;
import com.jamex.refereestaffer.model.exception.TeamNotFoundException;
import com.jamex.refereestaffer.model.request.ImportResponse;
import com.jamex.refereestaffer.repository.GradeRepository;
import com.jamex.refereestaffer.repository.MatchRepository;
import com.jamex.refereestaffer.repository.RefereeRepository;
import com.jamex.refereestaffer.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class ImporterService {

    private static final String CREATED = "Created ";
    private static final String DATE_FORMAT = "dd.MM.yyy HH:mm";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    private final TeamRepository teamRepository;
    private final RefereeRepository refereeRepository;
    private final MatchRepository matchRepository;
    private final GradeRepository gradeRepository;

    public ImportResponse importData(MultipartFile file, Short numberOfQueuesToImport) {
        List<String> result = new ArrayList<>();
        try (var is = file.getInputStream()) {
            var br = new BufferedReader(new InputStreamReader(is));
            String line;
            br.readLine(); // skip headers
            while ((line = br.readLine()) != null) {
                result.add(line);
            }

            createTeams(result);
            createReferees(result);
            createMatchesAndGrades(result, numberOfQueuesToImport);

            var noOfMatches = matchRepository.findAll().size();
            var noOfReferees = refereeRepository.findAll().size();
            var noOfGrades = gradeRepository.findAll().size();
            var noOfTeams = teamRepository.findAll().size();

            return new ImportResponse(noOfMatches, noOfReferees, noOfGrades, noOfTeams);
        } catch (IOException e) {
            throw new ImportException(file.getOriginalFilename());
        }
    }

    private void createMatchesAndGrades(List<String> lines, Short numberOfQueuesToImport) {
        var splittedLines = lines.stream()
                .map(line -> line.split(";"))
                .toList();

        for (var line : splittedLines) {
            var queue = Short.parseShort(line[0]);
            var homeTeamName = line[1];
            var homeTeam = teamRepository.findByName(homeTeamName)
                    .orElseThrow(() -> new TeamNotFoundException(homeTeamName));
            var awayTeamName = line[2];
            var awayTeam = teamRepository.findByName(awayTeamName)
                    .orElseThrow(() -> new TeamNotFoundException(awayTeamName));
            var date = LocalDateTime.parse(line[3], FORMATTER);

            Referee referee = null;
            Short homeTeamScore = null;
            Short awayTeamScore = null;
            if (line.length > 4 && queue <= numberOfQueuesToImport) {
                var refereeFirstName = line[4].split(" ")[0];
                var refereeLastName = line[4].split(" ")[1];
                referee = refereeRepository.findByFirstNameAndLastName(refereeFirstName, refereeLastName)
                        .orElseThrow(() -> new RefereeNotFoundException(refereeFirstName, refereeLastName));
                homeTeamScore = Short.valueOf(line[5]);
                awayTeamScore = Short.valueOf(line[6]);
            }
            var match = new Match(queue, homeTeam, awayTeam, date, referee, homeTeamScore, awayTeamScore);
            matchRepository.save(match);

            if (line.length == 8 && queue <= numberOfQueuesToImport) {
                var value = Double.parseDouble(line[7]);
                var grade = new Grade(match, value);
                gradeRepository.save(grade);
            }
        }
        var grades = gradeRepository.findAll();
        var matches = matchRepository.findAll();
        log.info(CREATED + grades.size() + " grades");
        log.info(CREATED + matches.size() + " matches");
    }

    private void createReferees(List<String> lines) {
        var referees = lines.stream()
                .map(line -> line.split(";"))
                .filter(line -> line.length > 4)
                .map(line -> line[4])
                .distinct()
//                .filter(referee -> !referee.isBlank())
                .map(refereeName -> new Referee(refereeName.split(" ")[0], refereeName.split(" ")[1]))
                .toList();

        refereeRepository.saveAll(referees);
        log.info(CREATED + referees.size() + " referees");
    }

    private void createTeams(List<String> lines) {
        var teamNames = lines.stream()
                .map(line -> line.split(";"))
                .map(line -> line[1])
                .collect(Collectors.toSet());
        var awayTeams = lines.stream()
                .map(line -> line.split(";"))
                .map(line -> line[2])
                .collect(Collectors.toSet());
        teamNames.addAll(awayTeams);

        var teams = teamNames.stream()
                .map(Team::new)
                .toList();

        teamRepository.saveAll(teams);
        log.info(CREATED + teams.size() + " teams");
    }
}
