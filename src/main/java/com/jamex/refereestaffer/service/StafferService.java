package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.converter.MatchConverter;
import com.jamex.refereestaffer.model.dto.MatchDto;
import com.jamex.refereestaffer.model.entity.Grade;
import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Referee;
import com.jamex.refereestaffer.model.entity.Team;
import com.jamex.refereestaffer.repository.MatchRepository;
import com.jamex.refereestaffer.repository.RefereeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class StafferService {

    private static final double AVERAGE_GRADE_MULTIPLIER = 4;
    private static final double NUMBER_OF_MATCHES_MULTIPLIER = 3;
    private static final double HOME_TEAM_REFEREED_MATCHES_MULTIPLIER = 1.3;
    private static final double AWAY_TEAM_REFEREED_MATCHES_MULTIPLIER = 1.3;
    public static final double MATCH_HARDNESS_LEVEL_MULTIPLIER = 12;
    public static final int INCREMENT_HARDNESS_WHEN_SAME_CITY = 3;

    private final RefereeRepository refereeRepository;
    private final MatchRepository matchRepository;
    private final MatchConverter matchConverter;
    private final MatchService matchService;

    public Collection<MatchDto> staffReferees(Short queue) {
        var referees = getReferees();
        var sortedMatchesInQueue = getMatches(queue);

        assignRefereesToMatches(referees, sortedMatchesInQueue);

        return matchConverter.convertFromEntities(sortedMatchesInQueue);
    }

    private List<Match> getMatches(Short queue) {
        var allMatches = matchRepository.findAll();
        matchService.calculatePointsForTeams(allMatches);

        return allMatches.stream()
                .filter(match -> match.getQueue().equals(queue))
                .sorted(Comparator.comparingDouble(this::countHardnessLvl).reversed())
                .collect(Collectors.toList());
    }

    private double countHardnessLvl(Match match) {
        var hardnessLvl = 0.0;

        double pointDifference = Math.abs(match.getHome().getPoints() - match.getAway().getPoints());
        if (pointDifference == 0) pointDifference += 0.5;
        hardnessLvl += 1 / pointDifference;

        if (match.getHome().getCity() != null && match.getHome().getCity().equals(match.getAway().getCity()))
            hardnessLvl += INCREMENT_HARDNESS_WHEN_SAME_CITY;

        return MATCH_HARDNESS_LEVEL_MULTIPLIER * hardnessLvl;
    }

    private void assignRefereesToMatches(LinkedList<Referee> referees, List<Match> matches) {
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

            var chosenReferee = sortedRefereesPotentialLvlMap.keySet().stream()
                    .findFirst()
                    .orElseThrow(); // TODO custom exception - not enough referees
            chosenReferee.setBusy(true);

            match.setReferee(chosenReferee);
        }
    }

    private double countRefereePotentialLvl(Referee referee, Team homeTeam, Team awayTeam) {
        var numberOfHomeTeamRefereedMatches = referee.getTeamsRefereed().getOrDefault(homeTeam, (short) 0);
        var numberOfAwayTeamRefereedMatches = referee.getTeamsRefereed().getOrDefault(awayTeam, (short) 0);

        return AVERAGE_GRADE_MULTIPLIER * referee.getAverageGrade() -
                NUMBER_OF_MATCHES_MULTIPLIER * referee.getNumberOfMatchesInRound() -
                HOME_TEAM_REFEREED_MATCHES_MULTIPLIER * numberOfHomeTeamRefereedMatches -
                AWAY_TEAM_REFEREED_MATCHES_MULTIPLIER * numberOfAwayTeamRefereedMatches;
    }

    private LinkedList<Referee> getReferees() {
        var referees = refereeRepository.findAll();
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
