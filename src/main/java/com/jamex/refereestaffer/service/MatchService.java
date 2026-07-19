package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.converter.MatchConverter;
import com.jamex.refereestaffer.model.dto.DifficultyBreakdownDto;
import com.jamex.refereestaffer.model.dto.MatchDto;
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
    private final RefereeRepository refereeRepository;
    private final MatchConverter matchConverter;

    public MatchService(MatchRepository matchRepository, GradeRepository gradeRepository,
                        ConfigurationRepository configurationRepository, TeamRepository teamRepository,
                        RefereeRepository refereeRepository, MatchConverter matchConverter) {
        this.matchRepository = matchRepository;
        this.gradeRepository = gradeRepository;
        this.configurationRepository = configurationRepository;
        this.teamRepository = teamRepository;
        this.refereeRepository = refereeRepository;
        this.matchConverter = matchConverter;
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
                .flatMap(dto -> Stream.of(dto.getHomeTeamId(), dto.getAwayTeamId())), Team::getId);
        var referees = findByIds(refereeRepository::findAllById, matchesDtos.stream()
                .map(MatchDto::getRefereeId), Referee::getId);
        var grades = findByIds(gradeRepository::findAllById, matchesDtos.stream()
                .map(MatchDto::getGradeId), Grade::getId);

        return matchesDtos.stream()
                .map(dto -> matchConverter.convertFromDto(dto,
                        requireTeam(teams, dto.getHomeTeamId()),
                        requireTeam(teams, dto.getAwayTeamId()),
                        resolveOptional(referees, dto.getRefereeId()),
                        resolveOptional(grades, dto.getGradeId())))
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
        // place is a @Transient field defaulting to 0 for any team that hasn't appeared in
        // a finished match yet (calculatePointsForTeams only ranks teams from the finished set).
        // Without this guard, place=0 satisfies `place <= numberOfTeamsOnEdge` (0 <= 3 by default),
        // so a fresh team's match would be classified as a top-of-table fixture by accident.
        if (homeTeam.getPlace() == 0 || awayTeam.getPlace() == 0) {
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
