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

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    public List<Referee> getAvailableRefereesForQueue(Short queue) {
        return refereeRepository.findAllWithNoMatchInQueue(queue).stream()
                // "S C" = "Sędzia z Centrali" (PZPN central-level referee assigned top-down).
                // Such matches already have a referee set in the imported data and must not be
                // reassigned by the staffer. The CSV stores these assignments with firstName="S",
                // lastName="C" as a sentinel, so we filter them out of the available pool.
                // TODO longer-term: model this as a Referee flag / separate column instead of a name sentinel.
                .filter(referee -> !(referee.getFirstName().equals("S") && referee.getLastName().equals("C")))
                .toList();
    }

    public void calculateStats(List<Referee> referees) {
        if (referees.isEmpty()) {
            return;
        }
        // One bulk query instead of a findAllByReferee per referee (N+1 — this runs on every
        // referee list/profile GET, not just staffing). Grouping by the entity works because
        // Hibernate returns the same managed Referee instances that went into the IN clause.
        var matchesByReferee = matchRepository.findAllByRefereeIn(referees).stream()
                .collect(Collectors.groupingBy(Match::getReferee));

        for (var referee : referees) {
            var matchesForReferee = matchesByReferee.getOrDefault(referee, List.of());

            var averageGrade = countAverageGrade(matchesForReferee);
            var teamsRefereedMap = createTeamsRefereedMap(matchesForReferee);
            var lastQueue = matchesForReferee.stream()
                    .map(Match::getQueue)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(null);

            // Win-distribution counters: split played matches by who won. Draws and
            // unfinished matches contribute to neither — they don't tell us anything about
            // home/away balance. The redesign's profile screen renders these as a
            // side-by-side bar (fairness signal: lopsided counts can flag a referee worth
            // a closer look, though they don't prove bias on their own).
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
     * Populates everything the redesigned Referee list / Profile / Dashboard screens need:
     * averages, last queue, and computed potential. Use for read-only endpoints that serve
     * the UI (`GET /api/referees`, `GET /api/referees/{id}`).
     *
     * <p>The potential formula matches the design's prototype: {@code P = α·avg + β·experience},
     * where α = AVERAGE_GRADE_MULTIPLIER and β = EXPERIENCE_MULTIPLIER. This is a simpler
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
