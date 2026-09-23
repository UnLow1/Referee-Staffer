package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.converter.MatchConverter;
import com.jamex.refereestaffer.model.dto.DifficultyBreakdownDto;
import com.jamex.refereestaffer.model.dto.MatchDto;
import com.jamex.refereestaffer.model.dto.StandingsDto;
import com.jamex.refereestaffer.model.entity.ConfigName;
import com.jamex.refereestaffer.model.entity.Grade;
import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Referee;
import com.jamex.refereestaffer.model.entity.Team;
import com.jamex.refereestaffer.model.exception.MatchNotFoundException;
import com.jamex.refereestaffer.model.exception.TeamNotFoundException;
import com.jamex.refereestaffer.repository.ConfigurationRepository;
import com.jamex.refereestaffer.repository.GradeRepository;
import com.jamex.refereestaffer.repository.MatchRepository;
import com.jamex.refereestaffer.repository.RefereeRepository;
import com.jamex.refereestaffer.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MatchService {

    private static final Logger log = LoggerFactory.getLogger(MatchService.class);

    private final MatchRepository matchRepository;
    private final GradeRepository gradeRepository;
    private final ConfigurationRepository configurationRepository;
    private final TeamRepository teamRepository;
    private final RefereeRepository refereeRepository;
    private final MatchConverter matchConverter;
    private final TeamService teamService;

    public MatchService(MatchRepository matchRepository, GradeRepository gradeRepository,
                        ConfigurationRepository configurationRepository, TeamRepository teamRepository,
                        RefereeRepository refereeRepository, MatchConverter matchConverter,
                        TeamService teamService) {
        this.matchRepository = matchRepository;
        this.gradeRepository = gradeRepository;
        this.configurationRepository = configurationRepository;
        this.teamRepository = teamRepository;
        this.refereeRepository = refereeRepository;
        this.matchConverter = matchConverter;
        this.teamService = teamService;
    }

    public MatchDto saveMatch(MatchDto matchDto) {
        var match = resolveAndConvert(List.of(matchDto)).get(0);
        var savedMatch = matchRepository.save(match);
        return matchConverter.convertFromEntity(savedMatch);
    }

    public void updateMatches(List<MatchDto> matchesDtos) {
        var matches = resolveAndConvert(matchesDtos);
        matchRepository.saveAll(matches);
    }

    /**
     * Resolves the id references of each dto (teams, referee, grade) with one bulk
     * query per repository and hands the ready entities to the converter — the
     * converter itself does no repository access. A missing team is an error
     * (404 via {@link TeamNotFoundException}); a missing referee or grade id maps
     * to null, which is what the pre-refactor per-id lookups did too.
     */
    private List<Match> resolveAndConvert(List<MatchDto> matchesDtos) {
        var teams = findByIds(teamRepository::findAllById, matchesDtos.stream()
                .flatMap(dto -> Stream.of(dto.homeTeamId(), dto.awayTeamId())), Team::getId);
        var referees = findByIds(refereeRepository::findAllById, matchesDtos.stream()
                .map(MatchDto::refereeId), Referee::getId);
        var grades = findByIds(gradeRepository::findAllById, matchesDtos.stream()
                .map(MatchDto::gradeId), Grade::getId);

        return matchesDtos.stream()
                .map(dto -> matchConverter.convertFromDto(dto,
                        requireTeam(teams, dto.homeTeamId()),
                        requireTeam(teams, dto.awayTeamId()),
                        resolveOptional(referees, dto.refereeId()),
                        resolveOptional(grades, dto.gradeId())))
                .toList();
    }

    private static <E> E resolveOptional(Map<Long, E> entitiesById, Long id) {
        return id == null ? null : entitiesById.get(id);
    }

    private <E> Map<Long, E> findByIds(Function<List<Long>, List<E>> bulkFinder, Stream<Long> ids, Function<E, Long> idGetter) {
        var distinctIds = ids.filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty())
            return Map.of();
        return bulkFinder.apply(distinctIds).stream()
                .collect(Collectors.toMap(idGetter, Function.identity()));
    }

    private Team requireTeam(Map<Long, Team> teams, Long teamId) {
        var team = teams.get(teamId);
        if (team == null)
            throw new TeamNotFoundException(teamId);
        return team;
    }

    /**
     * Points and table places keyed by team id, projected from the league table the
     * read-model computes ({@link TeamService#getStandings()}).
     *
     * <p>Keyed by id rather than by entity on purpose: with {@code open-in-view: false} the
     * match being scored and the table come from different persistence contexts, so the
     * {@code Team} instances are different objects and {@code Team} has no
     * {@code equals}/{@code hashCode} (see RS-75).
     *
     * <p>The read-model ranks every persisted team, so in production {@link #placeOf} is
     * null only for a team that is not in the table at all — a match referencing a team
     * deleted between the two queries. Such a match simply gets no edge-zone bonus.
     *
     * @param size number of ranked teams, i.e. the divisor the bottom zone is measured
     *             against. It is the table's own row count rather than a separate
     *             {@code teamRepository.count()}: before RS-99 those were two different
     *             populations (only teams with a finished match were ranked, but the
     *             divisor counted all of them), which kept the bottom-of-table bonus from
     *             ever firing early in a season.
     */
    public record LeagueTable(Map<Long, Short> pointsByTeamId, Map<Long, Short> placeByTeamId, int size) {

        /** Projects the standings rows onto the two lookups the scoring needs. */
        public static LeagueTable from(StandingsDto standings) {
            var pointsByTeamId = standings.rows().stream()
                    .collect(Collectors.toMap(StandingsDto.Row::id, StandingsDto.Row::points));
            var placeByTeamId = standings.rows().stream()
                    .collect(Collectors.toMap(StandingsDto.Row::id, StandingsDto.Row::place));
            return new LeagueTable(pointsByTeamId, placeByTeamId, standings.rows().size());
        }

        public short pointsOf(Team team) {
            return pointsByTeamId.getOrDefault(team.getId(), (short) 0);
        }

        public Short placeOf(Team team) {
            return placeByTeamId.get(team.getId());
        }
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

    /**
     * Matches the staffer is allowed to (re)assign in a queue, hardest first. Staffing
     * persists assignments immediately, so a regenerate must be able to reclaim matches
     * cast by a previous run — hence "assignable" covers previously assigned matches too,
     * with two exceptions that keep their referee:
     * <ul>
     *   <li>central assignments (the "S C" sentinel — see {@link Referee#isCentralSentinel()}),</li>
     *   <li>finished matches (both scores present) — history must not be rewritten.</li>
     * </ul>
     * Unassigned matches are always included, finished or not, matching the old
     * referee-is-null behavior.
     */
    public List<Match> getMatchesToAssignInQueue(Short queue) {
        var table = LeagueTable.from(teamService.getStandings());

        var matchesToAssignInQueue = matchRepository.findAllByQueue(queue).stream()
                .filter(this::isAssignable)
                .toList();

        // Config values are constant for the whole request — load them once here instead
        // of per match.
        var config = configurationRepository.findAllAsMap();
        matchesToAssignInQueue.forEach(match -> match.setHardnessLvl(computeBreakdown(match, table, config).total()));
        return matchesToAssignInQueue.stream()
                .sorted(Comparator.comparingDouble(Match::getHardnessLvl).reversed())
                .toList();
    }

    private boolean isAssignable(Match match) {
        if (match.getReferee() == null) {
            return true;
        }
        if (match.getReferee().isCentralSentinel()) {
            return false;
        }
        return match.getHomeScore() == null && match.getAwayScore() == null;
    }

    /**
     * Public entry-point for the Staffer drawer + Match detail screens. Loads the match
     * (404 if missing), then scores it against a freshly computed league table.
     *
     * <p>Note the match and the table come from separate repository calls, and with
     * {@code open-in-view: false} nothing keeps them in one persistence context — hence the
     * scoring below looks the numbers up in the {@link LeagueTable} by team id rather than
     * reading them off {@code match.getHome()}.
     */
    public DifficultyBreakdownDto computeDifficultyBreakdown(Long matchId) {
        var match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));

        // Recompute the table so place-based bonuses reflect current data — same pattern
        // getMatchesToAssignInQueue uses before scoring.
        var table = LeagueTable.from(teamService.getStandings());

        return computeBreakdown(match, table, configurationRepository.findAllAsMap());
    }

    /**
     * Scores a single match against an already-computed table. Package-private so specs can
     * drive the zone/derby permutations from an explicit {@link LeagueTable} without going
     * through a full standings computation for every case.
     */
    DifficultyBreakdownDto computeBreakdown(Match match, LeagueTable table,
                                            Map<ConfigName, Double> config) {
        var matchHardnessLvlMultiplier = config.get(ConfigName.DIFFICULTY_LEVEL_MULTIPLIER);
        var matchHardnessIncrementer = config.get(ConfigName.DIFFICULTY_LEVEL_INCREMENTER);
        var homeTeam = match.getHome();
        var awayTeam = match.getAway();
        var pointsDiff = Math.abs(table.pointsOf(homeTeam) - table.pointsOf(awayTeam));

        var base = (matchHardnessIncrementer - pointsDiff) * matchHardnessLvlMultiplier;
        var sameCity = isDerby(homeTeam, awayTeam)
                ? config.get(ConfigName.DIFFICULTY_LEVEL_SAME_CITY_INCREMENTER)
                : 0.0;

        var topAndBottom = computeEdgeMatchParts(homeTeam, awayTeam, table, config);
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
                                           Map<ConfigName, Double> config) {
        // place is null only for a team the table does not know at all; the read-model
        // gives every persisted team a row, so this is the deleted-between-queries case.
        // Such a match cannot be classified as a top- or bottom-of-table fixture.
        var homePlace = table.placeOf(homeTeam);
        var awayPlace = table.placeOf(awayTeam);
        if (homePlace == null || awayPlace == null) {
            return new double[]{0.0, 0.0};
        }
        var numberOfTeamsOnEdge = config.get(ConfigName.NUMBER_OF_EDGE_TEAMS).longValue();
        if (homePlace <= numberOfTeamsOnEdge && awayPlace <= numberOfTeamsOnEdge) {
            return new double[]{config.get(ConfigName.DIFFICULTY_LEVEL_MATCH_ON_TOP_INCREMENTER), 0.0};
        }
        if (homePlace > table.size() - numberOfTeamsOnEdge && awayPlace > table.size() - numberOfTeamsOnEdge) {
            return new double[]{0.0, config.get(ConfigName.DIFFICULTY_LEVEL_MATCH_ON_BOTTOM_INCREMENTER)};
        }
        return new double[]{0.0, 0.0};
    }
}
