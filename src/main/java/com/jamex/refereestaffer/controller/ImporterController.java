package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.entity.Grade;
import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Referee;
import com.jamex.refereestaffer.model.entity.Team;
import com.jamex.refereestaffer.repository.GradeRepository;
import com.jamex.refereestaffer.repository.MatchRepository;
import com.jamex.refereestaffer.repository.RefereeRepository;
import com.jamex.refereestaffer.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/importer")
public class ImporterController {

    private final TeamRepository teamRepository;
    private final RefereeRepository refereeRepository;
    private final MatchRepository matchRepository;
    private final GradeRepository gradeRepository;

    @PostMapping
    public void importData(@RequestParam("file") MultipartFile file) {
        BufferedReader br;
        List<String> result = new ArrayList<>();
        try {

            String line;
            InputStream is = file.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));
            br.readLine(); // skip headers
            while ((line = br.readLine()) != null) {
                result.add(line);
            }

            createTeams(result);
            createReferees(result);
            createMatchesAndGrades(result);

            System.out.println("Imported: ");
            System.out.println("referees = " + refereeRepository.findAll().size());
            System.out.println("grades = " + gradeRepository.findAll().size());
            System.out.println("teams = " + teamRepository.findAll().size());
            System.out.println("matches = " + matchRepository.findAll().size());

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void createMatchesAndGrades(List<String> lines) {
        var splittedLines = lines.stream()
                .map(line -> line.split(";"))
                .collect(Collectors.toList());

        for (var line : splittedLines) {
            var homeTeam = teamRepository.findByName(line[1]).orElseThrow(); // TODO add custom exception
            var awayTeam = teamRepository.findByName(line[2]).orElseThrow(); // TODO add custom exception
            var refereeFirstName = line[3].split(" ")[0];
            var refereeLastName = line[3].split(" ")[1];
            var referee = refereeRepository.findByFirstNameAndLastName(refereeFirstName, refereeLastName).orElseThrow(); // TODO add custom exception
            var match = new Match(Short.parseShort(line[0]), homeTeam, awayTeam, referee, Short.valueOf(line[4]), Short.valueOf(line[5]));
            matchRepository.save(match);

            if (line.length == 7) {
                var grade = new Grade(match, Double.parseDouble(line[6]));
                gradeRepository.save(grade);
            }
        }
    }

    private void createReferees(List<String> lines) {
        var referees = lines.stream()
                .map(line -> line.split(";"))
                .map(line -> line[3])
                .distinct()
                .map(refereeName -> new Referee(refereeName.split(" ")[0], refereeName.split(" ")[1]))
                .collect(Collectors.toList());

        refereeRepository.saveAll(referees);
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
                .collect(Collectors.toList());

        teamRepository.saveAll(teams);
    }
}
