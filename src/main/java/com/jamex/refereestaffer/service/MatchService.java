package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.entity.ConfigName;
import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Team;
import com.jamex.refereestaffer.model.exception.MatchNotFoundException;
import com.jamex.refereestaffer.repository.ConfigurationRepository;
import com.jamex.refereestaffer.repository.GradeRepository;
import com.jamex.refereestaffer.repository.MatchRepository;
import com.jamex.refereestaffer.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Service
public class MatchService {

    static final short POINTS_FOR_WIN_MATCH = 3;
    static final short POINTS_FOR_DRAW_MATCH = 1;

    private final MatchRepository matchRepository;
    private final GradeRepository gradeRepository;
    private final ConfigurationRepository configurationRepository;
    private final TeamRepository teamRepository;

    public void calculatePointsForTeams(List<Match> matches) {
        for (var match : matches) {
            if (match.getHomeScore() > match.getAwayScore())
                match.getHome().addPoints(POINTS_FOR_WIN_MATCH);
            else if (match.getHomeScore() < match.getAwayScore())
                match.getAway().addPoints(POINTS_FOR_WIN_MATCH);
            else {
                match.getHome().addPoints(POINTS_FOR_DRAW_MATCH);
                match.getAway().addPoints(POINTS_FOR_DRAW_MATCH);
            }
        }
        // TODO extract to separate method. Maybe refactor this place
        var teams = matches.stream()
                .flatMap(match -> Stream.of(match.getHome(), match.getAway()))
                .distinct()
                .sorted(Comparator.comparingInt(Team::getPoints).reversed())
                .toList();

        IntStream.range(0, teams.size())
                .forEach(i -> teams.get(i).setPlace((short) (i + 1)));
    }

    public void deleteMatch(Long matchId) {
        var match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));
        if (match.getGrade() != null) {
            log.info("Deleting grade with id = " + match.getGrade().getId());
            gradeRepository.delete(match.getGrade());
        }
        matchRepository.delete(match);
    }

    public List<Match> getMatchesToAssignInQueue(Short queue) {
        var allFinishedMatches = matchRepository.findAllByHomeScoreNotNullAndAwayScoreNotNull();
        calculatePointsForTeams(allFinishedMatches);

        var matchesToAssignInQueue = matchRepository.findAllByQueueAndRefereeIsNull(queue);

        matchesToAssignInQueue.forEach(match -> match.setHardnessLvl(countHardnessLvl(match)));
        return matchesToAssignInQueue.stream()
                .sorted(Comparator.comparingDouble(Match::getHardnessLvl).reversed())
                .toList();
    }

    private double countHardnessLvl(Match match) {
        var matchHardnessLvlMultiplier = configurationRepository.findByName(ConfigName.DIFFICULTY_LEVEL_MULTIPLIER);
        var matchHardnessIncrementer = configurationRepository.findByName(ConfigName.DIFFICULTY_LEVEL_INCREMENTER);
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
            return configurationRepository.findByName(ConfigName.DIFFICULTY_LEVEL_SAME_CITY_INCREMENTER).getValue();
        return 0;
    }

    private double calculateHardnessLvlForEdgeMatch(Team homeTeam, Team awayTeam) {
        var numberOfTeamsOnEdgeConfig = configurationRepository.findByName(ConfigName.NUMBER_OF_EDGE_TEAMS);
        var numberOfTeamsOnEdge = numberOfTeamsOnEdgeConfig.getValue().longValue();
        var numberOfTeams = teamRepository.count();
        if (homeTeam.getPlace() <= numberOfTeamsOnEdge && awayTeam.getPlace() <= numberOfTeamsOnEdge)
            return configurationRepository.findByName(ConfigName.DIFFICULTY_LEVEL_MATCH_ON_TOP_INCREMENTER).getValue();
        else if (homeTeam.getPlace() > numberOfTeams - numberOfTeamsOnEdge && awayTeam.getPlace() > numberOfTeams - numberOfTeamsOnEdge)
            return configurationRepository.findByName(ConfigName.DIFFICULTY_LEVEL_MATCH_ON_BOTTOM_INCREMENTER).getValue();
        return 0;
    }
}
