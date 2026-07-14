package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.dto.DifficultyBreakdownDto;
import com.jamex.refereestaffer.model.entity.ConfigName;
import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Team;
import com.jamex.refereestaffer.model.exception.MatchNotFoundException;
import com.jamex.refereestaffer.repository.ConfigurationRepository;
import com.jamex.refereestaffer.repository.GradeRepository;
import com.jamex.refereestaffer.repository.MatchRepository;
import com.jamex.refereestaffer.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
public class MatchService {

    private static final Logger log = LoggerFactory.getLogger(MatchService.class);

    static final short POINTS_FOR_WIN_MATCH = 3;
    static final short POINTS_FOR_DRAW_MATCH = 1;

    private final MatchRepository matchRepository;
    private final GradeRepository gradeRepository;
    private final ConfigurationRepository configurationRepository;
    private final TeamRepository teamRepository;

    public MatchService(MatchRepository matchRepository, GradeRepository gradeRepository,
                        ConfigurationRepository configurationRepository, TeamRepository teamRepository) {
        this.matchRepository = matchRepository;
        this.gradeRepository = gradeRepository;
        this.configurationRepository = configurationRepository;
        this.teamRepository = teamRepository;
    }

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
            log.info("Deleting grade with id = {}", match.getGrade().getId());
            gradeRepository.delete(match.getGrade());
        }
        matchRepository.delete(match);
    }

    public List<Match> getMatchesToAssignInQueue(Short queue) {
        var allFinishedMatches = matchRepository.findAllByHomeScoreNotNullAndAwayScoreNotNull();
        calculatePointsForTeams(allFinishedMatches);

        var matchesToAssignInQueue = matchRepository.findAllByQueueAndRefereeIsNull(queue);

        // Config values and the team count are constant for the whole request — load them
        // once here instead of per match (used to be 4-5 findByName + count per iteration).
        var config = configurationRepository.findAllAsMap();
        var numberOfTeams = teamRepository.count();
        matchesToAssignInQueue.forEach(match -> match.setHardnessLvl(computeBreakdown(match, config, numberOfTeams).total()));
        return matchesToAssignInQueue.stream()
                .sorted(Comparator.comparingDouble(Match::getHardnessLvl).reversed())
                .toList();
    }

    /**
     * Public entry-point for the redesigned Staffer drawer + Match detail screens. Loads
     * the match (404 if missing), then recomputes points/places against the latest finished
     * matches so `place` is fresh, and returns the per-component breakdown.
     */
    public DifficultyBreakdownDto computeDifficultyBreakdown(Long matchId) {
        var match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));

        // Refresh standings so place-based bonuses are computed against current data — same
        // pattern getMatchesToAssignInQueue uses before scoring.
        var finishedMatches = matchRepository.findAllByHomeScoreNotNullAndAwayScoreNotNull();
        calculatePointsForTeams(finishedMatches);

        return computeBreakdown(match, configurationRepository.findAllAsMap(), teamRepository.count());
    }

    private DifficultyBreakdownDto computeBreakdown(Match match, Map<ConfigName, Double> config, long numberOfTeams) {
        var matchHardnessLvlMultiplier = config.get(ConfigName.DIFFICULTY_LEVEL_MULTIPLIER);
        var matchHardnessIncrementer = config.get(ConfigName.DIFFICULTY_LEVEL_INCREMENTER);
        var homeTeam = match.getHome();
        var awayTeam = match.getAway();
        var pointsDiff = Math.abs(homeTeam.getPoints() - awayTeam.getPoints());

        var base = (matchHardnessIncrementer - pointsDiff) * matchHardnessLvlMultiplier;
        var sameCity = isDerby(homeTeam, awayTeam)
                ? config.get(ConfigName.DIFFICULTY_LEVEL_SAME_CITY_INCREMENTER)
                : 0.0;

        var topAndBottom = computeEdgeMatchParts(homeTeam, awayTeam, config, numberOfTeams);
        var top = topAndBottom[0];
        var bottom = topAndBottom[1];

        var total = base + sameCity + top + bottom;
        var flags = new DifficultyBreakdownDto.Flags(
                isDerby(homeTeam, awayTeam),
                top > 0,
                bottom > 0,
                pointsDiff
        );
        var parts = new DifficultyBreakdownDto.Parts(base, sameCity, top, bottom);
        return new DifficultyBreakdownDto(match.getId(), total, parts, flags);
    }

    private boolean isDerby(Team homeTeam, Team awayTeam) {
        return homeTeam.getCity() != null && homeTeam.getCity().equals(awayTeam.getCity());
    }

    /** Returns [top, bottom] — at most one of them can be non-zero. */
    private double[] computeEdgeMatchParts(Team homeTeam, Team awayTeam, Map<ConfigName, Double> config, long numberOfTeams) {
        // place is null for any team that hasn't appeared in a finished match yet
        // (calculatePointsForTeams only ranks teams from the finished set), so an unranked
        // team can never be classified as a top- or bottom-of-table fixture.
        if (homeTeam.getPlace() == null || awayTeam.getPlace() == null) {
            return new double[]{0.0, 0.0};
        }
        var numberOfTeamsOnEdge = config.get(ConfigName.NUMBER_OF_EDGE_TEAMS).longValue();
        if (homeTeam.getPlace() <= numberOfTeamsOnEdge && awayTeam.getPlace() <= numberOfTeamsOnEdge) {
            return new double[]{config.get(ConfigName.DIFFICULTY_LEVEL_MATCH_ON_TOP_INCREMENTER), 0.0};
        }
        if (homeTeam.getPlace() > numberOfTeams - numberOfTeamsOnEdge && awayTeam.getPlace() > numberOfTeams - numberOfTeamsOnEdge) {
            return new double[]{0.0, config.get(ConfigName.DIFFICULTY_LEVEL_MATCH_ON_BOTTOM_INCREMENTER)};
        }
        return new double[]{0.0, 0.0};
    }
}
