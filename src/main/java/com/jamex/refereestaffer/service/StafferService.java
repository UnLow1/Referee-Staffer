package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.converter.MatchConverter;
import com.jamex.refereestaffer.model.converter.RefereeConverter;
import com.jamex.refereestaffer.model.dto.MatchDto;
import com.jamex.refereestaffer.model.dto.RefereeDto;
import com.jamex.refereestaffer.model.entity.Grade;
import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Referee;
import com.jamex.refereestaffer.model.entity.Team;
import com.jamex.refereestaffer.repository.MatchRepository;
import com.jamex.refereestaffer.repository.RefereeRepository;
import com.jamex.refereestaffer.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class StafferService {

    private static final double AVERAGE_GRADE_MULTIPLIER = 4;
    private static final double NUMBER_OF_MATCHES_MULTIPLIER = 3;
    private static final double HOME_TEAM_REFEREED_MATCHES_MULTIPLIER = 1.3;
    private static final double AWAY_TEAM_REFEREED_MATCHES_MULTIPLIER = 1.3;

    private final RefereeRepository refereeRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;

    private final RefereeConverter refereeConverter;
    private final MatchConverter matchConverter;

    public Collection<MatchDto> staffReferees(Short queue) {
        var referees = getReferees();
        var matchesInQueue = getMatches(queue);

        matchesInQueue.sort(Comparator.comparingDouble(MatchDto::getHardnessLvl).reversed());
        assignRefereesToMatches(referees, matchesInQueue);
        saveMatchesToDb(matchesInQueue);

        return matchesInQueue;
    }

    private void saveMatchesToDb(List<MatchDto> matchesInQueue) {
        var matchesEntities = matchConverter.convertFromDtos(matchesInQueue);
        matchRepository.saveAll(matchesEntities);
    }

    private List<MatchDto> getMatches(Short queue) {
        var result = new ArrayList<MatchDto>();
        var matchesInQueue = matchRepository.findAllByQueue(queue);
        for (var match : matchesInQueue) {
            var hardnessLvl = countHardnessLvl(match);

            var matchDto = createMatchDto(match, hardnessLvl);
            result.add(matchDto);
        }

        return result;
    }

    private MatchDto createMatchDto(Match match, double hardnessLvl) {
        var matchDto = matchConverter.convertFromEntity(match);
        matchDto.setHardnessLvl(hardnessLvl);
        return matchDto;
    }

    private double countHardnessLvl(Match match) {
        // TODO
        var random = new Random();
        return random.nextDouble() * 100;
    }

    private void assignRefereesToMatches(LinkedList<RefereeDto> referees, List<MatchDto> matches) {
        for (var match : matches) {
            var refereesPotentialLvlMap = new HashMap<RefereeDto, Double>();

            var availableReferees = referees.stream()
                    .filter(ref -> !ref.isBusy())
                    .collect(Collectors.toList());

            for (var referee : availableReferees) {
                var potentialLvl = countRefereePotentialLvl(referee, match.getHomeTeamId(), match.getAwayTeamId());
                refereesPotentialLvlMap.put(referee, potentialLvl);
            }
            var sortedRefereesPotentialLvlMap = refereesPotentialLvlMap.entrySet().stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

            // TODO add a little bit random, not only first
            var chosenReferee = sortedRefereesPotentialLvlMap.keySet().stream()
                    .findFirst()
                    .orElseThrow(); // TODO custom exception - not enough referees
            chosenReferee.setBusy(true);

            match.setRefereeId(chosenReferee.getId());
        }
    }

    private double countRefereePotentialLvl(RefereeDto referee, Long homeTeamId, Long awayTeamId) {
        var homeTeam = teamRepository.findById(homeTeamId).orElseThrow(); // TODO add custom exception when team not found in db
        var awayTeam = teamRepository.findById(awayTeamId).orElseThrow();

        var numberOfHomeTeamRefereedMatches = referee.getTeamsRefereed().getOrDefault(homeTeam, (short) 0);
        var numberOfAwayTeamRefereedMatches = referee.getTeamsRefereed().getOrDefault(awayTeam, (short) 0);

        return AVERAGE_GRADE_MULTIPLIER * referee.getAverageGrade() -
                NUMBER_OF_MATCHES_MULTIPLIER * referee.getNumberOfMatchesInRound() -
                HOME_TEAM_REFEREED_MATCHES_MULTIPLIER * numberOfHomeTeamRefereedMatches -
                AWAY_TEAM_REFEREED_MATCHES_MULTIPLIER * numberOfAwayTeamRefereedMatches;
    }

    private LinkedList<RefereeDto> getReferees() {
        var result = new LinkedList<RefereeDto>();
        var referees = refereeRepository.findAll();
        for (var referee : referees) {
            var matchesForReferee = matchRepository.findAllByReferee(referee);

            var averageGrade = countAverageGrade(matchesForReferee);
            var teamsRefereedMap = createTeamsRefereedMap(matchesForReferee);

            var refereeDto = createRefereeDto(referee, matchesForReferee.size(), averageGrade, teamsRefereedMap);
            result.add(refereeDto);
        }
        return result;
    }

    private Map<Team, Short> createTeamsRefereedMap(List<Match> matchesForReferee) {
        var result = new HashMap<Team, Short>();

        for (var match : matchesForReferee) {
            result.merge(match.getHome(), (short) 1, (a, b) -> (short) (a + b));
            result.merge(match.getAway(), (short) 1, (a, b) -> (short) (a + b));
        }

        return result;
    }

    private RefereeDto createRefereeDto(Referee referee, int numberOfMatchesInRound, double averageGrade, Map<Team, Short> teamsRefereedMap) {
        var refereeDto = refereeConverter.convertFromEntity(referee);
        refereeDto.setAverageGrade(averageGrade);
        refereeDto.setTeamsRefereed(teamsRefereedMap);
        refereeDto.setNumberOfMatchesInRound((short) numberOfMatchesInRound);
        return refereeDto;
    }

    private double countAverageGrade(List<Match> matchesForReferee) {
        var refereeGrades = matchesForReferee.stream()
                .map(Match::getGrade)
                .map(Grade::getValue)
                .reduce(0.0, Double::sum);
        return refereeGrades / matchesForReferee.size();
    }
}
