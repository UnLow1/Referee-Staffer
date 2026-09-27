package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Referee;
import com.jamex.refereestaffer.model.entity.Vacation;
import com.jamex.refereestaffer.model.staffing.StaffingRule;
import com.jamex.refereestaffer.model.staffing.StaffingViolation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * An immutable snapshot of everything the staffing rules need, so that checking a single
 * {@code (match, referee)} pair costs no queries. Built by {@link StaffingRuleChecker}.
 *
 * <p>The pair signature is the point of this class: the cast table asks about the
 * <em>current</em> state ("what does this match break with the referee it already has?")
 * while the candidate list asks a <em>hypothetical</em> ("what would break if I gave this
 * match that referee?"). Both are the same question about a pair, so both go through
 * {@link #check(Match, Referee)}; {@link #check(Match)} is only a thin wrapper over it.
 *
 * <p>Rules are evaluated independently — one conflicting match can trip both
 * {@link StaffingRule#SAME_DAY_MATCH} and {@link StaffingRule#DOUBLE_MATCH_IN_QUEUE}
 * (same day, same queue). Suppressing one because of the other would make the result depend
 * on rule order, so callers get both and decide how to render them.
 */
public final class StaffingRules {

    static final String VACATION_MESSAGE = "%s is on vacation from %s to %s";
    static final String SAME_DAY_MATCH_MESSAGE = "%s already has a match on %s at %s (queue %s)";
    static final String DOUBLE_MATCH_IN_QUEUE_MESSAGE = "%s already has another match in queue %s";
    static final String REFEREE_NOT_IN_SNAPSHOT = "Referee with id = %s is outside this rule snapshot";
    static final String MATCH_DAY_NOT_IN_SNAPSHOT = "Match day %s is outside this rule snapshot";
    static final String MATCH_QUEUE_NOT_IN_SNAPSHOT = "Queue %s is outside this rule snapshot";

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final Set<LocalDate> coveredDays;
    private final Set<Short> coveredQueues;
    private final Set<Long> coveredRefereeIds;
    private final Map<Long, List<Match>> matchesByRefereeId;
    private final Map<Long, List<Vacation>> vacationsByRefereeId;

    StaffingRules(Set<LocalDate> coveredDays, Set<Short> coveredQueues, Set<Long> coveredRefereeIds,
                  Map<Long, List<Match>> matchesByRefereeId, Map<Long, List<Vacation>> vacationsByRefereeId) {
        this.coveredDays = Set.copyOf(coveredDays);
        this.coveredQueues = Set.copyOf(coveredQueues);
        this.coveredRefereeIds = Set.copyOf(coveredRefereeIds);
        this.matchesByRefereeId = Map.copyOf(matchesByRefereeId);
        this.vacationsByRefereeId = Map.copyOf(vacationsByRefereeId);
    }

    /**
     * Every rule the pair breaks, in {@link StaffingRule} declaration order. Empty means the
     * pair is clean.
     *
     * @throws IllegalArgumentException when the pair was not part of the snapshot — a silent
     *                                  "no violations" there would look like a clean pair
     *                                  while the data to judge it was never loaded.
     */
    public List<StaffingViolation> check(Match match, Referee referee) {
        var matchDay = match.getDate().toLocalDate();
        if (!coveredDays.contains(matchDay)) {
            throw new IllegalArgumentException(String.format(MATCH_DAY_NOT_IN_SNAPSHOT, matchDay));
        }
        // Explicit null checks first: the covered sets are immutable, and contains(null)
        // throws NPE on those instead of returning false.
        if (match.getQueue() == null || !coveredQueues.contains(match.getQueue())) {
            throw new IllegalArgumentException(String.format(MATCH_QUEUE_NOT_IN_SNAPSHOT, match.getQueue()));
        }
        if (referee.getId() == null || !coveredRefereeIds.contains(referee.getId())) {
            throw new IllegalArgumentException(String.format(REFEREE_NOT_IN_SNAPSHOT, referee.getId()));
        }

        var violations = new ArrayList<StaffingViolation>();
        var refereeName = referee.getFirstName() + " " + referee.getLastName();

        vacationCovering(referee, matchDay).ifPresent(vacation -> violations.add(new StaffingViolation(
                StaffingRule.VACATION,
                String.format(VACATION_MESSAGE, refereeName, vacation.getStartDate(), vacation.getEndDate()))));

        var otherMatches = otherMatchesOf(referee, match);
        otherMatches.stream()
                .filter(other -> matchDay.equals(other.getDate().toLocalDate()))
                .findFirst()
                .ifPresent(other -> violations.add(new StaffingViolation(
                        StaffingRule.SAME_DAY_MATCH,
                        String.format(SAME_DAY_MATCH_MESSAGE, refereeName, other.getDate().toLocalDate(),
                                other.getDate().format(TIME), other.getQueue()))));

        otherMatches.stream()
                .filter(other -> Objects.equals(other.getQueue(), match.getQueue()))
                .findFirst()
                .ifPresent(other -> violations.add(new StaffingViolation(
                        StaffingRule.DOUBLE_MATCH_IN_QUEUE,
                        String.format(DOUBLE_MATCH_IN_QUEUE_MESSAGE, refereeName, match.getQueue()))));

        return List.copyOf(violations);
    }

    /**
     * Rules broken by the referee the match already carries. Empty for an unassigned match —
     * "nobody is on it yet" is not a violation.
     */
    public List<StaffingViolation> check(Match match) {
        var referee = match.getReferee();
        return referee == null ? List.of() : check(match, referee);
    }

    /** Shorthand for the auto-staffer's candidate filter. */
    public boolean isClear(Match match, Referee referee) {
        return check(match, referee).isEmpty();
    }

    private Optional<Vacation> vacationCovering(Referee referee, LocalDate day) {
        return vacationsByRefereeId.getOrDefault(referee.getId(), List.of()).stream()
                .filter(vacation -> !day.isBefore(vacation.getStartDate()) && !day.isAfter(vacation.getEndDate()))
                .findFirst();
    }

    /**
     * The referee's other matches inside the snapshot. The match under test is excluded by id
     * so that asking about an already-saved assignment does not report the match against
     * itself. A match without an id (never persisted) cannot be excluded that way, so it is
     * compared by instance as well.
     */
    private List<Match> otherMatchesOf(Referee referee, Match match) {
        return matchesByRefereeId.getOrDefault(referee.getId(), List.of()).stream()
                .filter(other -> other != match)
                .filter(other -> match.getId() == null || !match.getId().equals(other.getId()))
                .toList();
    }
}
