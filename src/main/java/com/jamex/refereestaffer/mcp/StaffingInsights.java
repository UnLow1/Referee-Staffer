package com.jamex.refereestaffer.mcp;

import com.jamex.refereestaffer.mcp.view.AlgorithmParameter;
import com.jamex.refereestaffer.mcp.view.MatchPage;
import com.jamex.refereestaffer.mcp.view.MatchSummary;
import com.jamex.refereestaffer.mcp.view.RefereeMatch;
import com.jamex.refereestaffer.mcp.view.RefereeProfile;
import com.jamex.refereestaffer.mcp.view.RefereeSummary;
import com.jamex.refereestaffer.mcp.view.VacationPeriod;
import com.jamex.refereestaffer.model.dto.DifficultyBreakdownDto;
import com.jamex.refereestaffer.model.dto.StandingsDto;
import com.jamex.refereestaffer.model.entity.ConfigName;
import com.jamex.refereestaffer.model.entity.Grade;
import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Referee;
import com.jamex.refereestaffer.model.entity.Team;
import com.jamex.refereestaffer.model.exception.RefereeNotFoundException;
import com.jamex.refereestaffer.repository.ConfigurationRepository;
import com.jamex.refereestaffer.repository.MatchRepository;
import com.jamex.refereestaffer.repository.RefereeRepository;
import com.jamex.refereestaffer.repository.VacationRepository;
import com.jamex.refereestaffer.service.MatchService;
import com.jamex.refereestaffer.service.RefereeService;
import com.jamex.refereestaffer.service.TeamService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Read-only answers about staffing data, shaped for an AI client.
 *
 * <p>Deliberately free of any MCP or Spring AI import: {@link StaffingMcpTools} is the only
 * thing that knows about the protocol, and it does nothing but delegate here. The split
 * exists so the same methods can later back an in-app assistant that talks to the Claude
 * API directly (RS-A-6) without dragging the MCP transport along.
 *
 * <p>Everything here reads through the existing services and repositories — no second copy
 * of a formula, no write path. Where the REST DTOs are wider than a model needs, a
 * compact record from {@code mcp.view} is returned instead; where they are already minimal
 * ({@link StandingsDto}, {@link DifficultyBreakdownDto}) they are reused as they are.
 */
@Service
public class StaffingInsights {

    /** Cap on {@code pageSize} for the match listing — a model's context is the scarce resource. */
    static final int MAX_PAGE_SIZE = 100;
    static final int DEFAULT_PAGE_SIZE = 25;

    private final RefereeRepository refereeRepository;
    private final MatchRepository matchRepository;
    private final VacationRepository vacationRepository;
    private final ConfigurationRepository configurationRepository;
    private final RefereeService refereeService;
    private final MatchService matchService;
    private final TeamService teamService;

    public StaffingInsights(RefereeRepository refereeRepository, MatchRepository matchRepository,
                            VacationRepository vacationRepository, ConfigurationRepository configurationRepository,
                            RefereeService refereeService, MatchService matchService, TeamService teamService) {
        this.refereeRepository = refereeRepository;
        this.matchRepository = matchRepository;
        this.vacationRepository = vacationRepository;
        this.configurationRepository = configurationRepository;
        this.refereeService = refereeService;
        this.matchService = matchService;
        this.teamService = teamService;
    }

    /** Every referee with the statistics the Referee list screen shows. */
    public List<RefereeSummary> listReferees() {
        var referees = refereeRepository.findAll();
        refereeService.enrichWithStats(referees);
        return referees.stream()
                .map(StaffingInsights::toSummary)
                .toList();
    }

    /**
     * One referee with their full match history and vacation windows.
     *
     * @throws RefereeNotFoundException when no referee has that id — surfaces to the client
     *                                  as a tool error, which is what we want: a model
     *                                  guessing an id should be told, not handed an empty
     *                                  profile it might report as "referee has no matches"
     */
    public RefereeProfile getRefereeProfile(long refereeId) {
        var referee = refereeRepository.findById(refereeId)
                .orElseThrow(() -> new RefereeNotFoundException(refereeId));
        refereeService.enrichWithStats(List.of(referee));

        var matches = matchRepository.findAllByRefereeIn(List.of(referee)).stream()
                .sorted(Comparator.comparing(Match::getDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(StaffingInsights::toRefereeMatch)
                .toList();
        var vacations = vacationRepository.findAllByRefereeIdOrderByStartDateAsc(refereeId).stream()
                .map(vacation -> new VacationPeriod(vacation.getId(), vacation.getStartDate(), vacation.getEndDate()))
                .toList();

        return new RefereeProfile(toSummary(referee), nullToZero(referee.getHomeWins()),
                nullToZero(referee.getAwayWins()), matches, vacations);
    }

    /**
     * A page of matches, optionally narrowed to one queue. Always paged — see
     * {@link MatchPage}. Out-of-range values are clamped rather than rejected: a model that
     * asks for page -1 or 500 rows should get usable data back, not a validation error it
     * has to recover from.
     */
    public MatchPage listMatches(Short queue, Integer page, Integer pageSize) {
        var pageNumber = Math.max(0, page != null ? page : 0);
        var size = Math.clamp(pageSize != null ? pageSize : DEFAULT_PAGE_SIZE, 1, MAX_PAGE_SIZE);
        var pageable = PageRequest.of(pageNumber, size, Sort.by(Sort.Direction.ASC, "queue", "date"));

        Page<Match> matches = queue != null
                ? matchRepository.findAllByQueue(queue, pageable)
                : matchRepository.findAll(pageable);

        return new MatchPage(queue, pageNumber, size, matches.getTotalElements(), matches.getTotalPages(),
                matches.getContent().stream().map(StaffingInsights::toMatchSummary).toList());
    }

    /** Per-component difficulty breakdown of one match, recomputed against the current table. */
    public DifficultyBreakdownDto explainMatchDifficulty(long matchId) {
        return matchService.computeDifficultyBreakdown(matchId);
    }

    /** The league table as computed from finished matches. */
    public StandingsDto getStandings() {
        return teamService.getStandings();
    }

    /**
     * Every staffing weight with its current value, grouped and described. Ordered by
     * {@link ConfigName}'s declaration order so the three groups stay contiguous.
     */
    public List<AlgorithmParameter> getAlgorithmConfig() {
        var valuesByName = configurationRepository.findAllAsMap();
        return java.util.Arrays.stream(ConfigName.values())
                .filter(valuesByName::containsKey)
                .map(name -> new AlgorithmParameter(name.name(), valuesByName.get(name),
                        name.group(), name.description()))
                .toList();
    }

    /**
     * Plain-text description of the three scoring formulas — the text form of the Staffer
     * screen's "Algorithm explainer" panel, served as an MCP resource so a client can
     * attach it as context instead of calling a tool.
     */
    public String describeAlgorithm() {
        return """
                Referee Staffer — how assignments are scored

                Every weight named below is configurable; call get_algorithm_config (or read
                refstaffer://config) for the values currently in use.

                1. Referee potential — how strong a referee is
                   P = AVERAGE_GRADE_MULTIPLIER * avg(grade) + EXPERIENCE_MULTIPLIER * experience
                   avg(grade) is the mean of the referee's observer grades; a split grade
                   (7.9/8.3) counts as the mean of its two components. A referee with no graded
                   match yet is treated as league-average (8.3) so they can still be ranked.

                2. Match difficulty — how hard a match is to officiate
                   D = DIFFICULTY_LEVEL_MULTIPLIER * (DIFFICULTY_LEVEL_INCREMENTER - |points difference|)
                       + DIFFICULTY_LEVEL_SAME_CITY_INCREMENTER        (both teams from one city)
                       + DIFFICULTY_LEVEL_MATCH_ON_TOP_INCREMENTER     (both teams in the top edge)
                       + DIFFICULTY_LEVEL_MATCH_ON_BOTTOM_INCREMENTER  (both teams in the bottom edge)
                   The edge zones are NUMBER_OF_EDGE_TEAMS places wide at each end of the table,
                   and the top and bottom bonuses are mutually exclusive. A team that has not
                   played a finished match yet is unranked and can never make an edge fixture.

                3. Effective value — who gets this particular match
                   E = P - NUMBER_OF_MATCHES_MULTIPLIER * matches already officiated
                         - HOME_TEAM_REFEREED_MULTIPLIER * times the home team was refereed
                         - AWAY_TEAM_REFEREED_MULTIPLIER * times the away team was refereed
                   These are fairness penalties: heavy users and repeated pairings get pushed
                   down.

                Staffing order: matches in a queue are sorted by difficulty, hardest first, and
                each one goes to the available referee with the highest effective value.
                Unavailable means on vacation, already assigned in the same queue, or already
                officiating another match on the same calendar day. Central ("S C") assignments
                are fixed by the federation and are never reassigned.
                """;
    }

    /** The same weights {@link #getAlgorithmConfig()} returns, rendered as plain text. */
    public String describeConfiguration() {
        var text = new StringBuilder("Referee Staffer — current staffing weights\n");
        var lastGroup = "";
        for (var parameter : getAlgorithmConfig()) {
            if (!parameter.group().equals(lastGroup)) {
                text.append("\n[").append(parameter.group()).append("]\n");
                lastGroup = parameter.group();
            }
            text.append(parameter.name()).append(" = ").append(parameter.value())
                    .append("\n    ").append(parameter.description()).append('\n');
        }
        return text.toString();
    }

    private static RefereeSummary toSummary(Referee referee) {
        return new RefereeSummary(referee.getId(), fullName(referee), referee.getEmail(),
                referee.getExperience(), referee.getAverageGrade(), referee.getPotential(),
                referee.getNumberOfMatchesInRound(), referee.getLastQueue(), referee.isCentralSentinel());
    }

    private static RefereeMatch toRefereeMatch(Match match) {
        return new RefereeMatch(match.getId(), match.getQueue(), match.getDate(),
                teamName(match.getHome()), teamName(match.getAway()), score(match),
                effectiveGrade(match.getGrade()), gradeAsAwarded(match.getGrade()));
    }

    private static MatchSummary toMatchSummary(Match match) {
        var referee = match.getReferee();
        return new MatchSummary(match.getId(), match.getQueue(), match.getDate(),
                teamName(match.getHome()), teamName(match.getAway()),
                match.getHomeScore(), match.getAwayScore(),
                referee != null ? referee.getId() : null,
                referee != null ? fullName(referee) : null,
                referee != null && referee.isCentralSentinel(),
                effectiveGrade(match.getGrade()));
    }

    private static String fullName(Referee referee) {
        return referee.getFirstName() + " " + referee.getLastName();
    }

    private static String teamName(Team team) {
        return team != null ? team.getName() : null;
    }

    private static String score(Match match) {
        if (match.getHomeScore() == null || match.getAwayScore() == null) {
            return null;
        }
        return match.getHomeScore() + ":" + match.getAwayScore();
    }

    private static Double effectiveGrade(Grade grade) {
        return grade != null ? grade.getEffectiveValue() : null;
    }

    private static String gradeAsAwarded(Grade grade) {
        if (grade == null || grade.getValue() == null) {
            return null;
        }
        return grade.getSecondValue() != null
                ? grade.getValue() + "/" + grade.getSecondValue()
                : String.valueOf(grade.getValue());
    }

    private static short nullToZero(Short value) {
        return value != null ? value : (short) 0;
    }
}
