package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Referee;
import com.jamex.refereestaffer.model.entity.Vacation;
import com.jamex.refereestaffer.repository.MatchRepository;
import com.jamex.refereestaffer.repository.VacationRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Single source of truth for the staffing rules (RS-111). Three callers share it: the
 * auto-staffer uses it as a candidate filter, the candidate list asks it what a hypothetical
 * assignment would break, and the cast table asks it what the stored assignment breaks.
 *
 * <p>It never queries per pair. {@link #rulesFor(Collection, Collection)} loads the whole
 * matches × referees square in a fixed three queries and hands back a {@link StaffingRules}
 * snapshot; checking a pair against that snapshot is pure computation. Reading a cast would
 * otherwise be an N+1 over the queue.
 */
@Component
public class StaffingRuleChecker {

    static final String MATCH_NOT_RULEABLE = "Match with id = %s has no date or queue and cannot be rule-checked";
    static final String REFEREE_NOT_IDENTIFIED = "A referee without an id cannot be rule-checked";

    private final MatchRepository matchRepository;
    private final VacationRepository vacationRepository;

    public StaffingRuleChecker(MatchRepository matchRepository, VacationRepository vacationRepository) {
        this.matchRepository = matchRepository;
        this.vacationRepository = vacationRepository;
    }

    /**
     * Loads the rule data for every {@code (match, referee)} pair that can be built from the
     * arguments. The returned snapshot only answers about those pairs — anything else is an
     * {@link IllegalArgumentException} rather than a misleading "no violations".
     *
     * <p>Rejects incomplete input up front rather than dropping it, so that the snapshot covers
     * exactly what was asked for: silently skipping a referee here would surface much later, as
     * {@code check} refusing a pair the caller believes it built the snapshot for.
     */
    public StaffingRules rulesFor(Collection<Match> matches, Collection<Referee> referees) {
        matches.forEach(StaffingRuleChecker::requireRuleable);
        referees.forEach(StaffingRuleChecker::requireIdentified);

        var days = matches.stream()
                .map(match -> match.getDate().toLocalDate())
                .collect(Collectors.toSet());
        var queues = matches.stream()
                .map(Match::getQueue)
                .collect(Collectors.toSet());
        var refereeIds = referees.stream()
                .map(Referee::getId)
                .collect(Collectors.toSet());

        if (days.isEmpty() || referees.isEmpty()) {
            // Nothing to check — and an empty IN list would be invalid SQL anyway.
            return new StaffingRules(days, queues, refereeIds, Map.of(), Map.of());
        }

        return new StaffingRules(days, queues, refereeIds,
                loadRefereeMatches(referees, days, queues), loadVacations(days));
    }

    private static void requireRuleable(Match match) {
        if (match.getDate() == null || match.getQueue() == null) {
            throw new IllegalArgumentException(String.format(MATCH_NOT_RULEABLE, match.getId()));
        }
    }

    private static void requireIdentified(Referee referee) {
        if (referee.getId() == null) {
            throw new IllegalArgumentException(REFEREE_NOT_IDENTIFIED);
        }
    }

    /**
     * The referees' matches that any rule can refer to: everything on the days being checked
     * (SAME_DAY_MATCH, which spans queues since a match can be rescheduled onto the day) plus
     * everything in the queues being checked (DOUBLE_MATCH_IN_QUEUE, which spans days for the
     * same reason). One query for both halves — see
     * {@link MatchRepository#findAllByRefereeInOnDaysOrInQueues}.
     *
     * <p>The day filter is the {@code min..max} <em>range</em>, not the set of days: a single
     * fixture rescheduled far out therefore widens what is loaded, and {@code check} discards
     * the extra rows by exact-day comparison. Deliberate — it keeps the query count fixed, which
     * is what matters at this scale, and narrowing it would mean an OR-ed predicate per day.
     */
    private Map<Long, List<Match>> loadRefereeMatches(Collection<Referee> referees, Set<LocalDate> days,
                                                      Set<Short> queues) {
        var dayStart = Collections.min(days).atStartOfDay();
        var dayEnd = Collections.max(days).plusDays(1).atStartOfDay();

        return matchRepository.findAllByRefereeInOnDaysOrInQueues(referees, dayStart, dayEnd, queues).stream()
                // Group by referee id, not by entity: Referee has no equals/hashCode, so with
                // open-in-view off the instances here and the ones passed in may differ.
                .collect(Collectors.groupingBy(match -> match.getReferee().getId()));
    }

    /** Vacations overlapping the staffed days, grouped by referee id. */
    private Map<Long, List<Vacation>> loadVacations(Set<LocalDate> days) {
        return vacationRepository.findAllOverlapping(Collections.min(days), Collections.max(days)).stream()
                .filter(vacation -> vacation.getReferee() != null && vacation.getReferee().getId() != null)
                .collect(Collectors.groupingBy(vacation -> vacation.getReferee().getId()));
    }
}
