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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
     */
    public StaffingRules rulesFor(Collection<Match> matches, Collection<Referee> referees) {
        var days = matches.stream()
                .map(match -> match.getDate().toLocalDate())
                .collect(Collectors.toSet());
        var queues = matches.stream()
                .map(Match::getQueue)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        var refereeIds = referees.stream()
                .map(Referee::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (days.isEmpty() || referees.isEmpty()) {
            // Nothing to check — and an empty IN list would be invalid SQL anyway.
            return new StaffingRules(days, queues, refereeIds, Map.of(), Map.of());
        }

        return new StaffingRules(days, queues, refereeIds,
                loadRefereeMatches(referees, days, queues), loadVacations(days));
    }

    /**
     * The referees' matches that any rule can refer to: everything on the days being staffed
     * (SAME_DAY_MATCH, which spans queues since a match can be rescheduled onto the day) plus
     * everything in the queues being staffed (DOUBLE_MATCH_IN_QUEUE, which spans days for the
     * same reason). Two queries, deduplicated by match id.
     */
    private Map<Long, List<Match>> loadRefereeMatches(Collection<Referee> referees, Set<LocalDate> days,
                                                      Set<Short> queues) {
        var dayStart = Collections.min(days).atStartOfDay();
        var dayEnd = Collections.max(days).plusDays(1).atStartOfDay();

        var matchesById = new LinkedHashMap<Long, Match>();
        matchRepository.findAllByRefereeInAndDateGreaterThanEqualAndDateLessThan(referees, dayStart, dayEnd)
                .forEach(match -> matchesById.put(match.getId(), match));
        if (!queues.isEmpty()) {
            matchRepository.findAllByRefereeInAndQueueIn(referees, queues)
                    .forEach(match -> matchesById.put(match.getId(), match));
        }

        return matchesById.values().stream()
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
