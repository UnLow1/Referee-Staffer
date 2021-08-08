package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.converter.MatchConverter;
import com.jamex.refereestaffer.model.dto.MatchDto;
import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Referee;
import com.jamex.refereestaffer.model.entity.Team;
import com.jamex.refereestaffer.model.exception.StafferException;
import com.jamex.refereestaffer.repository.ConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class StafferService {

    private static final double DEFAULT_AVERAGE_GRADE = 8.3;

    private final ConfigurationRepository configurationRepository;
    private final MatchConverter matchConverter;
    private final MatchService matchService;
    private final RefereeService refereeService;

    public Collection<MatchDto> staffReferees(Short queue) {
        var referees = refereeService.getAvailableRefereesForQueue(queue);
        var sortedMatchesInQueue = matchService.getMatchesInQueue(queue);

        assignRefereesToMatches(referees, sortedMatchesInQueue);

        return matchConverter.convertFromEntities(sortedMatchesInQueue);
    }

    private void assignRefereesToMatches(List<Referee> referees, List<Match> matches) {
        for (var match : matches) {
            var refereesPotentialLvlMap = new HashMap<Referee, Double>();

            var availableReferees = referees.stream()
                    .filter(ref -> !ref.isBusy())
                    .collect(Collectors.toList());

            for (var referee : availableReferees) {
                var potentialLvl = countRefereePotentialLvl(referee, match.getHome(), match.getAway());
                refereesPotentialLvlMap.put(referee, potentialLvl);
            }
            var sortedRefereesPotentialLvlMap = refereesPotentialLvlMap.entrySet().stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

            log.info(String.format("Match: %s - %s; hardnessLvl = %f", match.getHome(), match.getAway(), match.getHardnessLvl()));
            log.info("Referees with their potential:");
            log.info(sortedRefereesPotentialLvlMap.toString());
            var chosenReferee = sortedRefereesPotentialLvlMap.keySet().stream()
                    .findFirst()
                    .orElseThrow(StafferException::new);
            chosenReferee.setBusy(true);

            match.setReferee(chosenReferee);
        }
    }

    private double countRefereePotentialLvl(Referee referee, Team homeTeam, Team awayTeam) {
        var averageGradeMultiplier = configurationRepository.findByName("average grade multiplier");
        var experienceMultiplier = configurationRepository.findByName("experience multiplier");
        var numberOfMatchesMultiplier = configurationRepository.findByName("number of matches multiplier");
        var homeTeamRefereedMatchesMultiplier = configurationRepository.findByName("home team refereed matches multiplier");
        var awayTeamRefereedMatchesMultiplier = configurationRepository.findByName("away team refereed matches multiplier");
        var numberOfHomeTeamRefereedMatches = referee.getTeamsRefereed().getOrDefault(homeTeam, (short) 0);
        var numberOfAwayTeamRefereedMatches = referee.getTeamsRefereed().getOrDefault(awayTeam, (short) 0);

        var averageGrade = referee.getAverageGrade() != 0 ? referee.getAverageGrade() : DEFAULT_AVERAGE_GRADE;

        return averageGradeMultiplier.getValue() * averageGrade +
                experienceMultiplier.getValue() * referee.getExperience() -
                numberOfMatchesMultiplier.getValue() * referee.getNumberOfMatchesInRound() -
                homeTeamRefereedMatchesMultiplier.getValue() * numberOfHomeTeamRefereedMatches -
                awayTeamRefereedMatchesMultiplier.getValue() * numberOfAwayTeamRefereedMatches;
    }
}
