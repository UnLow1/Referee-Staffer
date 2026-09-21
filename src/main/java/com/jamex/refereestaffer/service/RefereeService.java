package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.entity.ConfigName;
import com.jamex.refereestaffer.model.entity.Grade;
import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Referee;
import com.jamex.refereestaffer.model.entity.Team;
import com.jamex.refereestaffer.repository.ConfigurationRepository;
import com.jamex.refereestaffer.repository.MatchRepository;
import com.jamex.refereestaffer.repository.RefereeRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RefereeService {

    // Fallback used when a referee has no graded matches yet (rookies, future-only schedule,
    // matches where Grade hasn't been entered post-game). Treating "no track record" as the
    // league-average score lets the staffer still rank such a referee against others rather
    // than letting NaN propagate through the potential calculation. Package-private so tests
    // can reference it without hardcoding 8.3.
    static final double DEFAULT_GRADE = 8.3;

    private final RefereeRepository refereeRepository;
    private final MatchRepository matchRepository;
    private final ConfigurationRepository configurationRepository;

    public RefereeService(RefereeRepository refereeRepository, MatchRepository matchRepository,
                          ConfigurationRepository configurationRepository) {
        this.refereeRepository = refereeRepository;
        this.matchRepository = matchRepository;
        this.configurationRepository = configurationRepository;
    }

    /**
     * Referees that may be cast in a queue: everyone except the central "S C" sentinel and
     * those already tied to a match in the queue the staffer must not touch (a central or a
     * finished one — see {@link Match#isReassignable()}).
     *
     * <p>Deliberately ignores assignments on reassignable matches: since RS-105 generating a
     * cast is a draft that does not write to the database, so the stored assignments of the
     * very matches being re-decided are not evidence that a referee is taken. The old
     * "referee has no match in this queue" query could not express that — it would have made
     * every referee of a saved cast unavailable for its own regenerate, and left the Staffer
     * drawer with an empty candidate list on a queue that is already staffed.
     */
    public List<Referee> getAvailableRefereesForQueue(Short queue) {
        var keptRefereeIds = matchRepository.findAllByQueue(queue).stream()
                .filter(match -> !match.isReassignable())
                // A non-reassignable match always has a referee — see Match#isReassignable.
                .map(match -> match.getReferee().getId())
                .collect(Collectors.toSet());

        return refereeRepository.findAll().stream()
                // Central "S C" assignments already have a referee set in the imported data
                // and must not be reassigned by the staffer — see Referee#isCentralSentinel.
                // TODO longer-term: model this as a Referee flag / separate column instead of a name sentinel.
                .filter(referee -> !referee.isCentralSentinel())
                .filter(referee -> !keptRefereeIds.contains(referee.getId()))
                .toList();
    }

    public void calculateStats(List<Referee> referees) {
        // Collections.emptySet(), not Set.of(): the latter throws on a contains(null) lookup,
        // which an unsaved match would trigger in the filter below.
        calculateStats(referees, Collections.emptySet());
    }

    /**
     * @param ignoredMatchIds matches that must not count towards the stats. The staffer passes
     *                        the queue it is staffing, so a regenerate scores referees as if
     *                        that queue were still empty — otherwise the assignment a referee
     *                        currently holds there would penalise them (matches refereed,
     *                        teams refereed) for a pairing that is being re-decided anyway.
     *                        Before RS-105 staffing got this for free: it cleared and flushed
     *                        the assignments before reading them back.
     */
    public void calculateStats(List<Referee> referees, Set<Long> ignoredMatchIds) {
        if (referees.isEmpty()) {
            return;
        }
        // One bulk query instead of a findAllByReferee per referee (N+1 — this runs on every
        // referee list/profile GET, not just staffing). Group by id, not by entity: with OSIV
        // off, the referees passed in by a controller come from a different (already closed)
        // session than this query, so Hibernate returns different instances — and Referee
        // compares by identity, which would make every lookup below miss.
        var matchesByRefereeId = matchRepository.findAllByRefereeIn(referees).stream()
                .filter(match -> !ignoredMatchIds.contains(match.getId()))
                .collect(Collectors.groupingBy(match -> match.getReferee().getId()));

        for (var referee : referees) {
            var matchesForReferee = matchesByRefereeId.getOrDefault(referee.getId(), List.of());

            var averageGrade = countAverageGrade(matchesForReferee);
            var teamsRefereedMap = createTeamsRefereedMap(matchesForReferee);
            var lastQueue = matchesForReferee.stream()
                    .map(Match::getQueue)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(null);

            // Win-distribution counters: split played matches by who won. Draws and
            // unfinished matches contribute to neither — they don't tell us anything about
            // home/away balance. The profile screen renders these as a side-by-side bar
            // (fairness signal: lopsided counts can flag a referee worth a closer look,
            // though they don't prove bias on their own).
            short homeWins = 0;
            short awayWins = 0;
            for (var m : matchesForReferee) {
                if (m.getHomeScore() == null || m.getAwayScore() == null) continue;
                if (m.getHomeScore() > m.getAwayScore()) homeWins++;
                else if (m.getAwayScore() > m.getHomeScore()) awayWins++;
            }

            referee.setAverageGrade(averageGrade);
            referee.setTeamsRefereed(teamsRefereedMap);
            referee.setNumberOfMatchesInRound((short) matchesForReferee.size());
            referee.setLastQueue(lastQueue);
            referee.setHomeWins(homeWins);
            referee.setAwayWins(awayWins);
        }
    }

    /**
     * Populates everything the Referee list / Profile / Dashboard screens need:
     * averages, last queue, and computed potential. Use for read-only endpoints that serve
     * the UI (`GET /api/referees`, `GET /api/referees/{id}`).
     *
     * <p>The potential formula is {@code P = α·avg + β·experience}, where
     * α = AVERAGE_GRADE_MULTIPLIER and β = EXPERIENCE_MULTIPLIER. This is a simpler
     * variant than {@link StafferService#staffReferees} uses internally — staffer's score
     * subtracts fairness penalties that depend on the candidate match.
     */
    public void enrichWithStats(List<Referee> referees) {
        calculateStats(referees);
        var avgMultiplier = configurationRepository.findByName(ConfigName.AVERAGE_GRADE_MULTIPLIER).getValue();
        var expMultiplier = configurationRepository.findByName(ConfigName.EXPERIENCE_MULTIPLIER).getValue();
        for (var referee : referees) {
            var avg = referee.getAverageGrade() != null ? referee.getAverageGrade() : DEFAULT_GRADE;
            referee.setPotential(avgMultiplier * avg + expMultiplier * referee.getExperience());
        }
    }

    private double countAverageGrade(List<Match> matchesForReferee) {
        var matchesWithGrade = matchesForReferee.stream()
                .map(Match::getGrade)
                .filter(Objects::nonNull)
                .toList();
        if (matchesWithGrade.isEmpty()) {
            // Without this guard the next line evaluates to 0.0 / 0 = NaN, which then
            // poisons every potential calculation that touches this referee.
            return DEFAULT_GRADE;
        }
        var refereeGrades = matchesWithGrade.stream()
                .map(Grade::getEffectiveValue)
                .reduce(0.0, Double::sum);
        return refereeGrades / matchesWithGrade.size();
    }

    private Map<Team, Short> createTeamsRefereedMap(List<Match> matchesForReferee) {
        var result = new HashMap<Team, Short>();

        for (var match : matchesForReferee) {
            result.merge(match.getHome(), (short) 1, (a, b) -> (short) (a + b));
            result.merge(match.getAway(), (short) 1, (a, b) -> (short) (a + b));
        }

        return result;
    }
}
