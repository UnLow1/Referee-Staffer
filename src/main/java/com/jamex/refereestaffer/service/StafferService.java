package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.converter.MatchConverter;
import com.jamex.refereestaffer.model.dto.MatchDto;
import com.jamex.refereestaffer.model.entity.ConfigName;
import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Referee;
import com.jamex.refereestaffer.model.entity.Team;
import com.jamex.refereestaffer.model.entity.Vacation;
import com.jamex.refereestaffer.model.exception.RefereeNotFoundException;
import com.jamex.refereestaffer.model.exception.StafferException;
import com.jamex.refereestaffer.model.request.StaffingLockRequest;
import com.jamex.refereestaffer.repository.ConfigurationRepository;
import com.jamex.refereestaffer.repository.MatchRepository;
import com.jamex.refereestaffer.repository.RefereeRepository;
import com.jamex.refereestaffer.repository.VacationRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StafferService {

    static final String LOCKED_MATCH_NOT_ASSIGNABLE = "Locked match with id = %d is not assignable in queue %d";
    static final String DUPLICATE_LOCKED_MATCH = "Match with id = %d is locked more than once";
    static final String DUPLICATE_LOCKED_REFEREE = "Referee with id = %d is locked to more than one match";
    static final String LOCKED_REFEREE_UNAVAILABLE = "Referee with id = %d already has a non-reassignable match in queue %d";

    private static final Logger log = LoggerFactory.getLogger(StafferService.class);

    private final ConfigurationRepository configurationRepository;
    private final VacationRepository vacationRepository;
    private final MatchRepository matchRepository;
    private final RefereeRepository refereeRepository;
    private final MatchConverter matchConverter;
    private final MatchService matchService;
    private final RefereeService refereeService;
    private final EntityManager entityManager;

    public StafferService(ConfigurationRepository configurationRepository, VacationRepository vacationRepository,
                          MatchRepository matchRepository, RefereeRepository refereeRepository,
                          MatchConverter matchConverter, MatchService matchService, RefereeService refereeService,
                          EntityManager entityManager) {
        this.configurationRepository = configurationRepository;
        this.vacationRepository = vacationRepository;
        this.matchRepository = matchRepository;
        this.refereeRepository = refereeRepository;
        this.matchConverter = matchConverter;
        this.matchService = matchService;
        this.refereeService = refereeService;
        this.entityManager = entityManager;
    }

    /**
     * The cast a queue currently has stored, over the same match set a generate covers
     * ({@link MatchService#getMatchesToAssignInQueue}). The Staffer screen loads it on entry
     * and on every queue change, so a saved cast can be reviewed and exported without being
     * regenerated — before RS-105 regenerating was the only way to see one, and it destroyed
     * what it was meant to show.
     */
    @Transactional(readOnly = true)
    public Collection<MatchDto> getStoredCast(short queue) {
        return matchConverter.convertFromEntities(matchService.getMatchesToAssignInQueue(queue));
    }

    // @Transactional(readOnly = true) must sit on this overload too: the delegation below is a
    // self-invocation, so it bypasses the Spring proxy and would otherwise run outside the
    // read-only transaction the draft relies on.
    @Transactional(readOnly = true)
    public Collection<MatchDto> staffReferees(short queue) {
        return staffReferees(queue, List.of());
    }

    /**
     * (Re)generates the cast for a queue and returns it as a <strong>draft</strong>: nothing
     * is written to the database, so the caller can review, swap and discard it freely.
     * Persisting is the explicit job of "Save cast" (PUT /api/matches). Before RS-105 this
     * method mutated managed entities inside a read-write transaction, which meant clicking
     * "Generate cast" already overwrote a saved cast — silently and irreversibly.
     *
     * <p>Two things keep the draft a draft: the transaction is read-only (Hibernate runs the
     * session with {@code FlushMode.MANUAL}, so dirty checking never writes), and every match
     * the algorithm touches is detached from the persistence context before it is mutated.
     *
     * <p>Locked pairs pin a referee to a match up front: the match keeps that exact referee
     * and the referee is unavailable for the rest of the cast. Every other assignable match
     * is staffed from scratch, so a regenerate reshuffles previous auto-assignments instead
     * of silently keeping them.
     *
     * <p>Locks deliberately bypass the vacation filter — a pinned pair is an explicit user
     * decision, and rejecting it here would make the UI's lock state impossible to restore.
     */
    @Transactional(readOnly = true)
    public Collection<MatchDto> staffReferees(short queue, List<StaffingLockRequest> locks) {
        var sortedMatchesToStaff = matchService.getMatchesToAssignInQueue(queue);
        // The algorithm uses Match.referee as its working state, so the matches leave the
        // persistence context before anything is assigned to them — a managed entity would be
        // flushed on commit and the draft would not be a draft. Every association the cast
        // reads (home, away, referee) is eagerly fetched, so detaching costs nothing here.
        sortedMatchesToStaff.forEach(entityManager::detach);
        applyLocks(queue, sortedMatchesToStaff, locks);

        // Referees pinned by a lock leave the pool explicitly. The pool query cannot see it
        // any more: it reads the database, and the pinning only ever happened in memory.
        var pinnedRefereeIds = sortedMatchesToStaff.stream()
                .map(Match::getReferee)
                .filter(Objects::nonNull)
                .map(Referee::getId)
                .collect(Collectors.toSet());
        var referees = refereeService.getAvailableRefereesForQueue(queue).stream()
                .filter(referee -> !pinnedRefereeIds.contains(referee.getId()))
                .toList();
        // Everything downstream must look at the queue as if it were still empty: the stored
        // assignments of these very matches are what the run is re-deciding, so they may
        // neither penalise a referee (matches/teams already refereed) nor book them for the
        // day. Before RS-105 staffing got that for free by clearing and flushing first.
        var staffedMatchIds = matchIds(sortedMatchesToStaff);
        refereeService.calculateStats(referees, staffedMatchIds);
        // Load all config values up front instead of hitting the DB per (referee × match) — see
        // countRefereePotentialLvl. With ~15 referees × ~8 matches that's 600 → 1 query.
        var config = configurationRepository.findAllAsMap();

        var matchesToAutoStaff = sortedMatchesToStaff.stream()
                .filter(match -> match.getReferee() == null)
                .toList();
        assignRefereesToMatches(referees, matchesToAutoStaff, config, staffedMatchIds);

        return matchConverter.convertFromEntities(sortedMatchesToStaff);
    }

    private static Set<Long> matchIds(List<Match> matches) {
        return matches.stream()
                .map(Match::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Clears previous auto-assignments and pins the locked pairs. Clearing happens first so
     * a lock may freely move a referee between matches within the queue.
     */
    private void applyLocks(short queue, List<Match> matchesToStaff, List<StaffingLockRequest> locks) {
        validateLocks(queue, matchesToStaff, locks);

        matchesToStaff.forEach(match -> match.setReferee(null));
        if (locks.isEmpty()) {
            return;
        }

        var matchesById = matchesToStaff.stream()
                .collect(Collectors.toMap(Match::getId, Function.identity()));
        for (var lock : locks) {
            var referee = refereeRepository.findById(lock.refereeId())
                    .orElseThrow(() -> new RefereeNotFoundException(lock.refereeId()));
            matchesById.get(lock.matchId()).setReferee(referee);
        }
    }

    private void validateLocks(short queue, List<Match> matchesToStaff, List<StaffingLockRequest> locks) {
        if (locks.isEmpty()) {
            return;
        }
        var assignableMatchIds = matchesToStaff.stream()
                .map(Match::getId)
                .collect(Collectors.toSet());
        // Referees keeping an assignment in this queue (finished or central matches) cannot be
        // pinned to another match — that would double-book them within the round. Unreachable
        // through the UI (its candidate pool already excludes them) but reachable via raw API.
        var unavailableRefereeIds = matchRepository.findAllByQueue(queue).stream()
                .filter(match -> !assignableMatchIds.contains(match.getId()))
                .map(Match::getReferee)
                .filter(Objects::nonNull)
                .map(Referee::getId)
                .collect(Collectors.toSet());
        var lockedMatchIds = new HashSet<Long>();
        var lockedRefereeIds = new HashSet<Long>();
        for (var lock : locks) {
            if (!assignableMatchIds.contains(lock.matchId())) {
                throw new StafferException(String.format(LOCKED_MATCH_NOT_ASSIGNABLE, lock.matchId(), queue));
            }
            if (unavailableRefereeIds.contains(lock.refereeId())) {
                throw new StafferException(String.format(LOCKED_REFEREE_UNAVAILABLE, lock.refereeId(), queue));
            }
            if (!lockedMatchIds.add(lock.matchId())) {
                throw new StafferException(String.format(DUPLICATE_LOCKED_MATCH, lock.matchId()));
            }
            if (!lockedRefereeIds.add(lock.refereeId())) {
                throw new StafferException(String.format(DUPLICATE_LOCKED_REFEREE, lock.refereeId()));
            }
        }
    }

    private void assignRefereesToMatches(List<Referee> referees, List<Match> matches, Map<ConfigName, Double> config,
                                         Set<Long> staffedMatchIds) {
        var assignedRefereeIds = new HashSet<Long>();
        for (var match : matches) {
            var refereesPotentialLvlMap = new HashMap<Referee, Double>();
            var vacations = vacationRepository.findAllByStartDateIsLessThanEqualAndEndDateIsGreaterThanEqual(match.getDate());

            var refereesWithVacations = vacations.stream()
                    .map(Vacation::getReferee)
                    .toList();

            var refereesWithMatchOnSameDay = findRefereesWithMatchOnDay(referees, match.getDate(), staffedMatchIds);

            var availableReferees = referees.stream()
                    .filter(ref -> !assignedRefereeIds.contains(ref.getId()))
                    .filter(ref -> !refereesWithVacations.contains(ref))
                    .filter(ref -> !refereesWithMatchOnSameDay.contains(ref))
                    .toList();

            for (var referee : availableReferees) {
                var potentialLvl = countRefereePotentialLvl(referee, match.getHome(), match.getAway(), config);
                refereesPotentialLvlMap.put(referee, potentialLvl);
            }
            var sortedRefereesPotentialLvlMap = refereesPotentialLvlMap.entrySet().stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

            log.debug("Match: {} - {}; hardnessLvl = {}", match.getHome(), match.getAway(), match.getHardnessLvl());
            log.debug("Referees with their potential: {}", sortedRefereesPotentialLvlMap);
            var chosenReferee = sortedRefereesPotentialLvlMap.keySet().stream()
                    .findFirst()
                    .orElseThrow(StafferException::new);
            assignedRefereeIds.add(chosenReferee.getId());

            match.setReferee(chosenReferee);
        }
    }

    /**
     * Referees that already officiate another match on the same calendar day. The queue-level
     * uniqueness check (getAvailableRefereesForQueue) does not cover this: a match from a
     * different queue can be rescheduled onto this day, and one referee must never have two
     * matches on one day (RS-57).
     *
     * <p>The matches this run is staffing are skipped: their stored assignments are being
     * re-decided right now, so counting them would let a saved cast block its own regenerate.
     */
    private Set<Referee> findRefereesWithMatchOnDay(List<Referee> referees, LocalDateTime date,
                                                    Set<Long> staffedMatchIds) {
        if (referees.isEmpty()) {
            return Set.of();
        }
        return matchRepository.findAllByRefereeInAndDateOnDay(referees, date).stream()
                .filter(match -> match.getId() == null || !staffedMatchIds.contains(match.getId()))
                .map(Match::getReferee)
                .collect(Collectors.toSet());
    }

    private double countRefereePotentialLvl(Referee referee, Team homeTeam, Team awayTeam, Map<ConfigName, Double> config) {
        var numberOfHomeTeamRefereedMatches = referee.getTeamsRefereed().getOrDefault(homeTeam, (short) 0);
        var numberOfAwayTeamRefereedMatches = referee.getTeamsRefereed().getOrDefault(awayTeam, (short) 0);

        // After RefereeService.calculateStats this is always non-null — the no-grades fallback
        // (DEFAULT_GRADE) is applied there.
        var averageGrade = referee.getAverageGrade();

        return config.get(ConfigName.AVERAGE_GRADE_MULTIPLIER) * averageGrade +
                config.get(ConfigName.EXPERIENCE_MULTIPLIER) * referee.getExperience() -
                config.get(ConfigName.NUMBER_OF_MATCHES_MULTIPLIER) * referee.getNumberOfMatchesInRound() -
                config.get(ConfigName.HOME_TEAM_REFEREED_MULTIPLIER) * numberOfHomeTeamRefereedMatches -
                config.get(ConfigName.AWAY_TEAM_REFEREED_MULTIPLIER) * numberOfAwayTeamRefereedMatches;
    }
}
