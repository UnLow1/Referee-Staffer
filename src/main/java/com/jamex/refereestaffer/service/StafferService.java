package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.converter.MatchConverter;
import com.jamex.refereestaffer.model.dto.MatchDto;
import com.jamex.refereestaffer.model.entity.Grade;
import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Referee;
import com.jamex.refereestaffer.model.entity.Team;
import com.jamex.refereestaffer.model.exception.StafferException;
import com.jamex.refereestaffer.repository.ConfigurationRepository;
import com.jamex.refereestaffer.repository.MatchRepository;
import com.jamex.refereestaffer.repository.RefereeRepository;
import com.jamex.refereestaffer.repository.TeamRepository;
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

    private final RefereeRepository refereeRepository;
    private final MatchRepository matchRepository;
    private final ConfigurationRepository configurationRepository;
    private final MatchConverter matchConverter;
    private final MatchService matchService;
    private final TeamRepository teamRepository;

    public Collection<MatchDto> staffReferees(Short queue) {
        var referees = getReferees(queue);
        var sortedMatchesInQueue = getMatches(queue);

        assignRefereesToMatches(referees, sortedMatchesInQueue);

        return matchConverter.convertFromEntities(sortedMatchesInQueue);
    }

    private List<Match> getMatches(Short queue) {
        var allMatches = matchRepository.findAllByHomeScoreNotNullAndAwayScoreNotNull();
        matchService.calculatePointsForTeams(allMatches);

        var matchesToAssign = matchRepository.findAllByQueueAndRefereeIsNull(queue);

        return matchesToAssign.stream()
                .peek(match -> match.setHardnessLvl(countHardnessLvl(match)))
                .sorted(Comparator.comparingDouble(Match::getHardnessLvl).reversed())
                .collect(Collectors.toList());
    }

    private double countHardnessLvl(Match match) {
        var matchHardnessLvlMultiplier = configurationRepository.findByName("match hardness level multiplier");
        var matchHardnessIncrementer = configurationRepository.findByName("match hardness incrementer");
        var homeTeam = match.getHome();
        var awayTeam = match.getAway();

        var pointDifference = Math.abs(homeTeam.getPoints() - awayTeam.getPoints());

        var hardnessLvl = matchHardnessIncrementer.getValue() - pointDifference;
        hardnessLvl *= matchHardnessLvlMultiplier.getValue();

        hardnessLvl += calculateHardnessLvlForDerby(homeTeam, awayTeam);
        hardnessLvl += calculateHardnessLvlForEdgeMatch(homeTeam, awayTeam);

        return hardnessLvl;
    }

    private double calculateHardnessLvlForDerby(Team homeTeam, Team awayTeam) {
        if (homeTeam.getCity() != null && homeTeam.getCity().equals(awayTeam.getCity()))
            return configurationRepository.findByName("increment hardness level when same city").getValue();
        return 0;
    }

    private double calculateHardnessLvlForEdgeMatch(Team homeTeam, Team awayTeam) {
        var numberOfTeamsOnEdgeConfig = configurationRepository.findByName("number of teams on edge");
        var numberOfTeamsOnEdge = numberOfTeamsOnEdgeConfig.getValue().longValue();
        var numberOfTeams = teamRepository.findAll().size();
        if (homeTeam.getPlace() <= numberOfTeamsOnEdge && awayTeam.getPlace() <= numberOfTeamsOnEdge)
            return configurationRepository.findByName("increment hardness when match on top").getValue();
        else if (homeTeam.getPlace() > numberOfTeams - numberOfTeamsOnEdge && awayTeam.getPlace() > numberOfTeams - numberOfTeamsOnEdge)
            return configurationRepository.findByName("increment hardness when match on bottom").getValue();
        return 0;
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

    private List<Referee> getReferees(Short queue) {
        // TODO improve filtering SC referees
        var referees = refereeRepository.findAllWithNoMatchInQueue(queue).stream()
                .filter(referee -> !referee.getFirstName().equals("S"))
                .filter(referee -> !referee.getLastName().equals("C"))
                .collect(Collectors.toList());
        for (var referee : referees) {
            var matchesForReferee = matchRepository.findAllByReferee(referee);

            var averageGrade = countAverageGrade(matchesForReferee);
            var teamsRefereedMap = createTeamsRefereedMap(matchesForReferee);

            referee.setAverageGrade(averageGrade);
            referee.setTeamsRefereed(teamsRefereedMap);
            referee.setNumberOfMatchesInRound((short) matchesForReferee.size());
        }
        return referees;
    }

    private Map<Team, Short> createTeamsRefereedMap(List<Match> matchesForReferee) {
        var result = new HashMap<Team, Short>();

        for (var match : matchesForReferee) {
            result.merge(match.getHome(), (short) 1, (a, b) -> (short) (a + b));
            result.merge(match.getAway(), (short) 1, (a, b) -> (short) (a + b));
        }

        return result;
    }

    private double countAverageGrade(List<Match> matchesForReferee) {
        var refereeGrades = matchesForReferee.stream()
                .map(Match::getGrade)
                .filter(Objects::nonNull)
                .map(Grade::getValue)
                .reduce(0.0, Double::sum);
        return refereeGrades / matchesForReferee.size();
    }
}
