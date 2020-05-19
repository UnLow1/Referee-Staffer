package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.converter.MatchConverter;
import com.jamex.refereestaffer.model.dto.MatchDto;
import com.jamex.refereestaffer.model.entity.Grade;
import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Referee;
import com.jamex.refereestaffer.model.entity.Team;
import com.jamex.refereestaffer.model.exception.StafferException;
import com.jamex.refereestaffer.repository.MatchRepository;
import com.jamex.refereestaffer.repository.RefereeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class StafferService {

    private static final double AVERAGE_GRADE_MULTIPLIER = 50;
    private static final double EXPERIENCE_MULTIPLIER = 0.01;
    private static final double NUMBER_OF_MATCHES_MULTIPLIER = 3;
    private static final double HOME_TEAM_REFEREED_MATCHES_MULTIPLIER = 1.3;
    private static final double AWAY_TEAM_REFEREED_MATCHES_MULTIPLIER = 1.3;
    public static final double MATCH_HARDNESS_LEVEL_MULTIPLIER = 12;
    public static final double INCREMENT_HARDNESS_WHEN_SAME_CITY = 3;
    public static final double INCREMENT_HARDNESS_WHEN_MATCH_AT_TOP = 3;
    public static final double INCREMENT_HARDNESS_WHEN_MATCH_AT_BOTTOM = 2;
    public static final int NUMBER_OF_TEAMS_ON_EDGE = 3;

    private final RefereeRepository refereeRepository;
    private final MatchRepository matchRepository;
    private final MatchConverter matchConverter;
    private final MatchService matchService;
    private final TeamService teamService;

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
                .peek(match -> match.setHardnessLvl(countHardnessLvl(match)))
                .sorted(Comparator.comparingDouble(Match::getHardnessLvl).reversed())
                .collect(Collectors.toList());
    }

    private double countHardnessLvl(Match match) {
        var homeTeam = match.getHome();
        var awayTeam = match.getAway();

        var pointDifference = Math.abs(homeTeam.getPoints() - awayTeam.getPoints());

        var hardnessLvl = Math.pow(2 / Math.exp(1), pointDifference);
        hardnessLvl *= MATCH_HARDNESS_LEVEL_MULTIPLIER;

        hardnessLvl += calculateHardnessLvlForDerby(homeTeam, awayTeam);
        hardnessLvl += calculateHardnessLvlForEdgeMatch(homeTeam, awayTeam);

        return hardnessLvl;
    }

    private double calculateHardnessLvlForDerby(Team homeTeam, Team awayTeam) {
        if (homeTeam.getCity() != null && homeTeam.getCity().equals(awayTeam.getCity()))
            return INCREMENT_HARDNESS_WHEN_SAME_CITY;
        return 0;
    }

    private double calculateHardnessLvlForEdgeMatch(Team homeTeam, Team awayTeam) {
        var standings = teamService.getStandings();
        var teams = List.of(homeTeam, awayTeam);
        var topTeams = standings.stream()
                .limit(NUMBER_OF_TEAMS_ON_EDGE)
                .collect(Collectors.toList());
        if (topTeams.containsAll(teams))
            return INCREMENT_HARDNESS_WHEN_MATCH_AT_TOP;
        else {
            var bottomTeams = standings.stream()
                    .skip(standings.size() - NUMBER_OF_TEAMS_ON_EDGE)
                    .collect(Collectors.toList());
            if (bottomTeams.containsAll(teams))
                return INCREMENT_HARDNESS_WHEN_MATCH_AT_BOTTOM;
        }
        return 0;
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
                    .orElseThrow(StafferException::new);
            chosenReferee.setBusy(true);

            match.setReferee(chosenReferee);
        }
    }

    private double countRefereePotentialLvl(Referee referee, Team homeTeam, Team awayTeam) {
        var numberOfHomeTeamRefereedMatches = referee.getTeamsRefereed().getOrDefault(homeTeam, (short) 0);
        var numberOfAwayTeamRefereedMatches = referee.getTeamsRefereed().getOrDefault(awayTeam, (short) 0);

        return AVERAGE_GRADE_MULTIPLIER * referee.getAverageGrade() +
                EXPERIENCE_MULTIPLIER * referee.getExperience() -
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
