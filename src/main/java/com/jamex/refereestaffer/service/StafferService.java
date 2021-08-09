package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.converter.MatchConverter;
import com.jamex.refereestaffer.model.dto.MatchDto;
import com.jamex.refereestaffer.model.entity.ConfigName;
import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Referee;
import com.jamex.refereestaffer.model.entity.Team;
import com.jamex.refereestaffer.model.exception.StafferException;
import com.jamex.refereestaffer.repository.ConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        var sortedMatchesInQueue = matchService.getMatchesToAssignInQueue(queue);

        assignRefereesToMatches(referees, sortedMatchesInQueue);

        return matchConverter.convertFromEntities(sortedMatchesInQueue);
    }

    private void assignRefereesToMatches(List<Referee> referees, List<Match> matches) {
        for (var match : matches) {
            var refereesPotentialLvlMap = new HashMap<Referee, Double>();

            var availableReferees = referees.stream()
                    .filter(ref -> !ref.isBusy())
                    .toList();

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
        var averageGradeMultiplier = configurationRepository.findByName(ConfigName.AVERAGE_GRADE_MULTIPLIER);
        var experienceMultiplier = configurationRepository.findByName(ConfigName.EXPERIENCE_MULTIPLIER);
        var numberOfMatchesMultiplier = configurationRepository.findByName(ConfigName.NUMBER_OF_MATCHES_MULTIPLIER);
        var homeTeamRefereedMatchesMultiplier = configurationRepository.findByName(ConfigName.HOME_TEAM_REFEREED_MULTIPLIER);
        var awayTeamRefereedMatchesMultiplier = configurationRepository.findByName(ConfigName.AWAY_TEAM_REFEREED_MULTIPLIER);
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
