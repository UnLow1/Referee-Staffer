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
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    /**
     * Points and table places keyed by team id, for the teams that appear in the finished
     * matches a ranking pass was built from.
     *
     * <p>Keyed by id rather than by entity on purpose: with {@code open-in-view: false} the
     * match being scored and the matches the table was computed from can come from different
     * persistence contexts, so the {@code Team} instances are different objects and
     * {@code Team} has no {@code equals}/{@code hashCode}. Reading the numbers off the entity
     * would then silently yield the un-enriched values (0 points, no place) — see RS-75.
     *
     * <p>Teams absent from the ranking (no finished match yet) report 0 points and a
     * {@code null} place, matching what a freshly loaded entity used to carry.
     */
    public record LeagueTable(Map<Long, Short> pointsByTeamId, Map<Long, Short> placeByTeamId) {

        public short pointsOf(Team team) {
            return pointsByTeamId.getOrDefault(team.getId(), (short) 0);
        }

        public Short placeOf(Team team) {
            return placeByTeamId.get(team.getId());
        }
    }

    /**
     * Ranks the teams taking part in {@code matches} and returns the resulting table.
     *
     * <p>Ranking rules are unchanged from when this method only mutated entities: only teams
     * with a finished match are ranked, ordering is by points alone, and ties keep the order
     * in which the teams were first encountered. Replacing them with the richer tie-break
     * chain of {@link TeamService#getStandings} would move matches between table zones and
     * therefore change match hardness — that unification is RS-99, deliberately not this
     * change.
     *
     * <p>The transient {@code points}/{@code place} fields are still written to the entities
     * for the benefit of callers that read them off {@code Team}; the returned table is the
     * authoritative, session-independent copy. Dropping the entity mutation altogether is
     * likewise RS-99.
     */
    public LeagueTable calculatePointsForTeams(List<Match> matches) {
        // Insertion-ordered so that the sort below — which is stable — leaves teams on equal
        // points in first-encountered order, exactly as the previous stream pipeline did.
        var pointsByTeamId = new LinkedHashMap<Long, Short>();
        for (var match : matches) {
            var home = match.getHome();
            var away = match.getAway();
            pointsByTeamId.putIfAbsent(home.getId(), (short) 0);
            pointsByTeamId.putIfAbsent(away.getId(), (short) 0);

            if (match.getHomeScore() > match.getAwayScore())
                award(pointsByTeamId, home, POINTS_FOR_WIN_MATCH);
            else if (match.getHomeScore() < match.getAwayScore())
                award(pointsByTeamId, away, POINTS_FOR_WIN_MATCH);
            else {
                award(pointsByTeamId, home, POINTS_FOR_DRAW_MATCH);
                award(pointsByTeamId, away, POINTS_FOR_DRAW_MATCH);
            }
        }

        var rankedTeamIds = pointsByTeamId.entrySet().stream()
                .sorted(Map.Entry.<Long, Short>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();
        var placeByTeamId = new HashMap<Long, Short>();
        IntStream.range(0, rankedTeamIds.size())
                .forEach(i -> placeByTeamId.put(rankedTeamIds.get(i), (short) (i + 1)));

        var table = new LeagueTable(pointsByTeamId, placeByTeamId);
        matches.stream()
                .flatMap(match -> Stream.of(match.getHome(), match.getAway()))
                .distinct()
                .forEach(team -> {
                    team.addPoints(table.pointsOf(team));
                    team.setPlace(table.placeOf(team));
                });
        return table;
    }

    private static void award(Map<Long, Short> pointsByTeamId, Team team, short points) {
        pointsByTeamId.merge(team.getId(), points, (a, b) -> (short) (a + b));
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
        var table = calculatePointsForTeams(allFinishedMatches);

        var matchesToAssignInQueue = matchRepository.findAllByQueueAndRefereeIsNull(queue);

        // Config values and the team count are constant for the whole request — load them
        // once here instead of per match (used to be 4-5 findByName + count per iteration).
        var config = configurationRepository.findAllAsMap();
        var numberOfTeams = teamRepository.count();
        matchesToAssignInQueue.forEach(match -> match.setHardnessLvl(computeBreakdown(match, table, config, numberOfTeams).total()));
        return matchesToAssignInQueue.stream()
                .sorted(Comparator.comparingDouble(Match::getHardnessLvl).reversed())
                .toList();
    }

    /**
     * Public entry-point for the redesigned Staffer drawer + Match detail screens. Loads
     * the match (404 if missing), then recomputes points/places against the latest finished
     * matches so `place` is fresh, and returns the per-component breakdown.
     *
     * <p>Note the match and the table come from two separate repository calls, and with
     * {@code open-in-view: false} nothing keeps them in one persistence context — hence the
     * scoring below reads the numbers out of the returned {@link LeagueTable} rather than
     * off {@code match.getHome()}, whose transient fields belong to the other session.
     */
    public DifficultyBreakdownDto computeDifficultyBreakdown(Long matchId) {
        var match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));

        // Refresh standings so place-based bonuses are computed against current data — same
        // pattern getMatchesToAssignInQueue uses before scoring.
        var finishedMatches = matchRepository.findAllByHomeScoreNotNullAndAwayScoreNotNull();
        var table = calculatePointsForTeams(finishedMatches);

        return computeBreakdown(match, table, configurationRepository.findAllAsMap(), teamRepository.count());
    }

    /**
     * Scores a single match against an already-computed table. Package-private so specs can
     * exercise the zone/derby permutations with an explicit {@link LeagueTable} instead of
     * fabricating transient state on {@code Team} entities — a state production never
     * produces, since those fields are only ever written by a ranking pass.
     */
    DifficultyBreakdownDto computeBreakdown(Match match, LeagueTable table,
                                            Map<ConfigName, Double> config, long numberOfTeams) {
        var matchHardnessLvlMultiplier = config.get(ConfigName.DIFFICULTY_LEVEL_MULTIPLIER);
        var matchHardnessIncrementer = config.get(ConfigName.DIFFICULTY_LEVEL_INCREMENTER);
        var homeTeam = match.getHome();
        var awayTeam = match.getAway();
        var pointsDiff = Math.abs(table.pointsOf(homeTeam) - table.pointsOf(awayTeam));

        var base = (matchHardnessIncrementer - pointsDiff) * matchHardnessLvlMultiplier;
        var sameCity = isDerby(homeTeam, awayTeam)
                ? config.get(ConfigName.DIFFICULTY_LEVEL_SAME_CITY_INCREMENTER)
                : 0.0;

        var topAndBottom = computeEdgeMatchParts(homeTeam, awayTeam, table, config, numberOfTeams);
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
    private double[] computeEdgeMatchParts(Team homeTeam, Team awayTeam, LeagueTable table,
                                           Map<ConfigName, Double> config, long numberOfTeams) {
        // place is null for any team that hasn't appeared in a finished match yet
        // (calculatePointsForTeams only ranks teams from the finished set), so an unranked
        // team can never be classified as a top- or bottom-of-table fixture.
        var homePlace = table.placeOf(homeTeam);
        var awayPlace = table.placeOf(awayTeam);
        if (homePlace == null || awayPlace == null) {
            return new double[]{0.0, 0.0};
        }
        var numberOfTeamsOnEdge = config.get(ConfigName.NUMBER_OF_EDGE_TEAMS).longValue();
        if (homePlace <= numberOfTeamsOnEdge && awayPlace <= numberOfTeamsOnEdge) {
            return new double[]{config.get(ConfigName.DIFFICULTY_LEVEL_MATCH_ON_TOP_INCREMENTER), 0.0};
        }
        if (homePlace > numberOfTeams - numberOfTeamsOnEdge && awayPlace > numberOfTeams - numberOfTeamsOnEdge) {
            return new double[]{0.0, config.get(ConfigName.DIFFICULTY_LEVEL_MATCH_ON_BOTTOM_INCREMENTER)};
        }
        return new double[]{0.0, 0.0};
    }
}
