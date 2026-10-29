package com.jamex.refereestaffer.service

import com.jamex.refereestaffer.model.converter.MatchConverter
import com.jamex.refereestaffer.model.dto.MatchDto
import com.jamex.refereestaffer.model.entity.*
import com.jamex.refereestaffer.model.exception.RefereeNotFoundException
import com.jamex.refereestaffer.model.exception.StafferException
import com.jamex.refereestaffer.model.request.StaffingLockRequest
import com.jamex.refereestaffer.model.staffing.StaffingRule
import com.jamex.refereestaffer.repository.ConfigurationRepository
import com.jamex.refereestaffer.repository.MatchRepository
import com.jamex.refereestaffer.repository.RefereeRepository
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDateTime

class StafferServiceSpec extends Specification {

    @Subject
    StafferService stafferService

    ConfigurationRepository configurationRepository = Mock()
    MatchRepository matchRepository = Mock()
    RefereeRepository refereeRepository = Mock()
    MatchConverter matchConverter = Mock()
    MatchService matchService = Mock()
    RefereeService refereeService = Mock()
    StaffingRuleChecker staffingRuleChecker = Mock()

    def setup() {
        stafferService = new StafferService(configurationRepository, matchRepository, refereeRepository,
                matchConverter, matchService, refereeService, staffingRuleChecker)
    }

    /**
     * A real rule snapshot over the given matrix — the rules themselves are covered by
     * StaffingRuleCheckerSpec, so these features only need the checker to answer truthfully
     * about the data they set up. Passing real snapshots (rather than mocking StaffingRules)
     * also keeps the coverage checks in StaffingRules exercised from this side.
     */
    private static StaffingRules rules(List<Match> matches, List<Referee> referees,
                                       Map<Long, List<Match>> refereeMatches = [:],
                                       Map<Long, List<Vacation>> vacations = [:]) {
        new StaffingRules(matches.collect { it.date.toLocalDate() } as Set,
                // findAll { it != null } rather than bare findAll(): the latter filters by Groovy
                // truth and would also drop a legitimate queue / id of 0.
                matches.collect { it.queue }.findAll { it != null } as Set,
                referees.collect { it.id }.findAll { it != null } as Set,
                refereeMatches, vacations)
    }

    def "should assign referees to matches in queue"() {
        given:
        short queue = 2
        def team1 = Team.builder()
                .id(101L)
                .name("test team")
                .build()
        def team2 = Team.builder()
                .id(102L)
                .name("test team 123213")
                .build()
        // After RefereeService.calculateStats, averageGrade is always non-null — the
        // no-grades fallback (DEFAULT_GRADE = 8.3) is applied there. Setting it
        // explicitly here mirrors the real invariant. Ids are needed because the
        // staffer deduplicates already-assigned referees by id, and teamsRefereed is
        // keyed by team id (RS-127).
        def ref1 = Referee.builder()
                .id(1L)
                .averageGrade(8.1d)
                .teamsRefereed(Map.of(team1.id, (short) 1, team2.id, (short) 1))
                .numberOfMatchesInRound((short) 7)
                .build()
        def ref2 = Referee.builder()
                .id(2L)
                .averageGrade(RefereeService.DEFAULT_GRADE)
                .experience(100)
                .teamsRefereed([:])
                .numberOfMatchesInRound((short) 0)
                .build()
        def referees = [ref1, ref2]
        def matchDateTime = LocalDateTime.of(2022, 10, 12, 16, 0)
        def match1 = Match.builder()
                .id(1L)
                .queue(queue)
                .home(team1)
                .away(team2)
                .date(matchDateTime)
                .build()
        def match2 = Match.builder()
                .id(2L)
                .queue(queue)
                .home(team2)
                .away(team1)
                .date(matchDateTime)
                .build()
        def matches = [match1, match2]
        List<MatchDto> matchesDtos = []

        when:
        def result = stafferService.staffReferees(queue)

        then:
        result == matchesDtos
        match1.referee == ref2
        match2.referee == ref1
        1 * staffingRuleChecker.rulesFor(matches, referees) >> rules(matches, referees)
        1 * refereeService.getAvailableRefereesForQueue(queue) >> referees
        1 * matchService.getMatchesToAssignInQueue(queue) >> matches
        1 * matchConverter.convertFromEntities(matches) >> matchesDtos
        1 * configurationRepository.findAllAsMap() >> [
                (ConfigName.AVERAGE_GRADE_MULTIPLIER)  : gradeMultiplier as Double,
                (ConfigName.EXPERIENCE_MULTIPLIER)     : expMultiplier as Double,
                (ConfigName.NUMBER_OF_MATCHES_MULTIPLIER): noOfMatchesMultiplier as Double,
                (ConfigName.HOME_TEAM_REFEREED_MULTIPLIER): homeTeamMultiplier as Double,
                (ConfigName.AWAY_TEAM_REFEREED_MULTIPLIER): awayTeamMultiplier as Double
        ]

        where:
        gradeMultiplier | expMultiplier | noOfMatchesMultiplier | homeTeamMultiplier | awayTeamMultiplier
        1               | 0             | 0                     | 0                  | 0
        0               | 1             | 0                     | 0                  | 0
        0               | 0             | 1                     | 0                  | 0
        0               | 0             | 0                     | 1                  | 0
        0               | 0             | 0                     | 0                  | 1
        50              | 0.01          | 3                     | 1.3                | 1.3
    }

    def "should skip referees the staffing rules reject"() {
        given:
        short queue = 2
        def matchDateTime = LocalDateTime.of(2026, 10, 12, 16, 0)
        // ref1 would win on potential, but the rules disqualify them: a vacation covering the
        // match day, and (ref3) another match already booked on that day.
        def ref1 = Referee.builder().id(1L).averageGrade(9.5d).teamsRefereed([:]).numberOfMatchesInRound((short) 0).build()
        def ref2 = Referee.builder().id(2L).averageGrade(8.0d).teamsRefereed([:]).numberOfMatchesInRound((short) 0).build()
        def ref3 = Referee.builder().id(3L).averageGrade(9.0d).teamsRefereed([:]).numberOfMatchesInRound((short) 0).build()
        def referees = [ref1, ref2, ref3]
        def match = Match.builder().id(1L).queue(queue)
                .home(Team.builder().name("home").build())
                .away(Team.builder().name("away").build())
                .date(matchDateTime)
                .build()
        def vacations = [1L: [Vacation.builder().referee(ref1)
                                      .startDate(matchDateTime.toLocalDate())
                                      .endDate(matchDateTime.toLocalDate()).build()]]
        def refereeMatches = [3L: [Match.builder().id(9L).queue((short) 1).referee(ref3)
                                           .date(matchDateTime.minusHours(5)).build()]]

        when:
        stafferService.staffReferees(queue)

        then:
        match.referee == ref2
        1 * staffingRuleChecker.rulesFor([match], referees) >> rules([match], referees, refereeMatches, vacations)
        1 * refereeService.getAvailableRefereesForQueue(queue) >> referees
        1 * refereeService.calculateStats(referees)
        1 * matchService.getMatchesToAssignInQueue(queue) >> [match]
        1 * matchConverter.convertFromEntities([match])
        1 * configurationRepository.findAllAsMap() >> allOnesConfig()
    }

    def "should throw StafferException when no referees are available for queue"() {
        given:
        short queue = 5
        def match = Match.builder()
                .id(1L)
                .queue(queue)
                .home(Team.builder().name("home").build())
                .away(Team.builder().name("away").build())
                .date(LocalDateTime.of(2026, 5, 4, 15, 0))
                .build()

        when:
        stafferService.staffReferees(queue)

        then:
        1 * refereeService.getAvailableRefereesForQueue(queue) >> []
        1 * refereeService.calculateStats([])
        1 * matchService.getMatchesToAssignInQueue(queue) >> [match]
        1 * configurationRepository.findAllAsMap() >> [:]
        1 * staffingRuleChecker.rulesFor([match], []) >> rules([match], [])
        0 * matchConverter.convertFromEntities(_)
        thrown(StafferException)
    }

    def "should throw StafferException when every available referee breaks a rule"() {
        given:
        short queue = 5
        def matchDateTime = LocalDateTime.of(2026, 5, 4, 15, 0)
        def referee = Referee.builder().id(1L).averageGrade(8.0d).teamsRefereed([:]).numberOfMatchesInRound((short) 0).build()
        def match = Match.builder()
                .id(1L)
                .queue(queue)
                .home(Team.builder().name("home").build())
                .away(Team.builder().name("away").build())
                .date(matchDateTime)
                .build()
        def vacations = [1L: [Vacation.builder().referee(referee)
                                      .startDate(matchDateTime.toLocalDate())
                                      .endDate(matchDateTime.toLocalDate()).build()]]

        when:
        stafferService.staffReferees(queue)

        then:
        1 * refereeService.getAvailableRefereesForQueue(queue) >> [referee]
        1 * refereeService.calculateStats([referee])
        1 * matchService.getMatchesToAssignInQueue(queue) >> [match]
        1 * configurationRepository.findAllAsMap() >> [:]
        1 * staffingRuleChecker.rulesFor([match], [referee]) >> rules([match], [referee], [:], vacations)
        0 * matchConverter.convertFromEntities(_)
        thrown(StafferException)
    }

    def "should penalize a referee for teams already refereed when the match holds detached team copies"() {
        given:
        short queue = 3
        def matchDateTime = LocalDateTime.of(2026, 5, 4, 15, 0)
        // refWithHistory wins on grade alone, so only the two team penalties can keep it out
        // of the cast: 9.0 - 3 - 3 = 3.0 against the free referee's 7.0.
        def refWithHistory = Referee.builder()
                .id(1L)
                .averageGrade(9.0d)
                .teamsRefereed([(101L): (short) 3, (102L): (short) 3])
                .numberOfMatchesInRound((short) 0)
                .build()
        def freeReferee = Referee.builder()
                .id(2L)
                .averageGrade(7.0d)
                .teamsRefereed([:])
                .numberOfMatchesInRound((short) 0)
                .build()
        def referees = [refWithHistory, freeReferee]
        // Same teams, different instances — teamsRefereed is built from the matches of
        // another query (RefereeService.calculateStats) than the ones being staffed, so this is
        // what a reloaded entity, a defensive copy or a converted DTO would hand the staffer.
        // Team has no equals/hashCode, so entity keys would compare references here, miss both
        // counts and silently drop the penalties to zero (RS-127).
        def match = Match.builder()
                .queue(queue)
                .home(Team.builder().id(101L).name("home").build())
                .away(Team.builder().id(102L).name("away").build())
                .date(matchDateTime)
                .build()

        when:
        stafferService.staffReferees(queue)

        then:
        match.referee == freeReferee
        1 * refereeService.getAvailableRefereesForQueue(queue) >> referees
        1 * refereeService.calculateStats(referees)
        1 * matchService.getMatchesToAssignInQueue(queue) >> [match]
        1 * configurationRepository.findAllAsMap() >> allOnesConfig()
        1 * staffingRuleChecker.rulesFor([match], referees) >> rules([match], referees)
        1 * matchConverter.convertFromEntities([match])
    }

    def "should pin locked pair and auto-staff only the remaining matches"() {
        given:
        short queue = 3
        def team1 = Team.builder().name("team A").build()
        def team2 = Team.builder().name("team B").build()
        def lockedReferee = Referee.builder().id(11l).firstName("Locked").lastName("Referee").build()
        def freeReferee = Referee.builder().id(22l).averageGrade(8.0d).teamsRefereed([:]).numberOfMatchesInRound((short) 0).build()
        // The rule snapshot is keyed by match day and the staffer skips dateless matches, so
        // the fixtures carry a real date.
        def matchDateTime = LocalDateTime.of(2026, 5, 4, 11, 0)
        def lockedMatch = Match.builder().id(1l).queue(queue).home(team1).away(team2).date(matchDateTime).build()
        def otherMatch = Match.builder().id(2l).queue(queue).home(team2).away(team1).date(matchDateTime).build()
        def locks = [new StaffingLockRequest(1l, 11l)]

        when:
        stafferService.staffReferees(queue, locks)

        then:
        lockedMatch.referee == lockedReferee
        otherMatch.referee == freeReferee
        1 * matchService.getMatchesToAssignInQueue(queue) >> [lockedMatch, otherMatch]
        1 * matchRepository.findAllByQueue(queue) >> [lockedMatch, otherMatch]
        1 * refereeRepository.findById(11l) >> Optional.of(lockedReferee)
        // Pinned state must be flushed before the native availability query runs.
        1 * matchRepository.flush()
        // The pool already excludes the locked referee — only the auto-staffed match draws from it.
        1 * refereeService.getAvailableRefereesForQueue(queue) >> [freeReferee]
        1 * refereeService.calculateStats([freeReferee])
        // Only the unlocked match goes through the rules; the pinned pair bypasses them.
        1 * staffingRuleChecker.rulesFor([otherMatch], [freeReferee]) >> rules([otherMatch], [freeReferee])
        1 * matchConverter.convertFromEntities([lockedMatch, otherMatch])
        1 * configurationRepository.findAllAsMap() >> allOnesConfig()
    }

    def "should clear stale assignment and re-staff the match on regenerate"() {
        given:
        short queue = 3
        def staleReferee = Referee.builder().id(1l).firstName("Stale").lastName("Assignment").build()
        def newReferee = Referee.builder().id(2l).averageGrade(8.0d).teamsRefereed([:]).numberOfMatchesInRound((short) 0).build()
        def matchDateTime = LocalDateTime.of(2026, 5, 4, 11, 0)
        def match = Match.builder()
                .id(5l)
                .queue(queue)
                .home(Team.builder().name("home").build())
                .away(Team.builder().name("away").build())
                .referee(staleReferee)
                .date(matchDateTime)
                .build()

        when:
        stafferService.staffReferees(queue)

        then:
        match.referee == newReferee
        1 * matchService.getMatchesToAssignInQueue(queue) >> [match]
        1 * matchRepository.flush()
        1 * refereeService.getAvailableRefereesForQueue(queue) >> [newReferee]
        1 * staffingRuleChecker.rulesFor([match], [newReferee]) >> rules([match], [newReferee])
        1 * matchConverter.convertFromEntities([match])
        1 * configurationRepository.findAllAsMap() >> allOnesConfig()
    }

    def "should return a match without a date unassigned and auto-staff the rest"() {
        given:
        short queue = 3
        def team1 = Team.builder().name("team A").build()
        def team2 = Team.builder().name("team B").build()
        def staleReferee = Referee.builder().id(1l).firstName("Stale").lastName("Assignment").build()
        def freeReferee = Referee.builder().id(2l).averageGrade(8.0d).teamsRefereed([:]).numberOfMatchesInRound((short) 0).build()
        def matchDateTime = LocalDateTime.of(2026, 5, 4, 11, 0)
        def datedMatch = Match.builder().id(1l).queue(queue).home(team1).away(team2).date(matchDateTime).build()
        // Planned fixture whose kick-off is not scheduled yet, still carrying an earlier pick.
        def datelessMatch = Match.builder().id(2l).queue(queue).home(team2).away(team1).referee(staleReferee).build()

        when:
        stafferService.staffReferees(queue)

        then:
        datedMatch.referee == freeReferee
        datelessMatch.referee == null
        1 * matchService.getMatchesToAssignInQueue(queue) >> [datelessMatch, datedMatch]
        1 * matchRepository.flush()
        1 * refereeService.getAvailableRefereesForQueue(queue) >> [freeReferee]
        // The rules run for the dated match only — rulesFor rejects a match without a date.
        1 * staffingRuleChecker.rulesFor([datedMatch], [freeReferee]) >> rules([datedMatch], [freeReferee])
        // The dateless match stays in the cast so the Staffer screen still lists it.
        1 * matchConverter.convertFromEntities([datelessMatch, datedMatch])
        1 * configurationRepository.findAllAsMap() >> allOnesConfig()
    }

    def "should keep a locked referee on a match without a date"() {
        given:
        short queue = 3
        def pickedReferee = Referee.builder().id(11l).firstName("Picked").lastName("ByHand").build()
        def datelessMatch = Match.builder()
                .id(1l)
                .queue(queue)
                .home(Team.builder().name("home").build())
                .away(Team.builder().name("away").build())
                .build()

        when:
        stafferService.staffReferees(queue, [new StaffingLockRequest(1l, 11l)])

        then:
        datelessMatch.referee == pickedReferee
        1 * matchService.getMatchesToAssignInQueue(queue) >> [datelessMatch]
        1 * matchRepository.findAllByQueue(queue) >> [datelessMatch]
        1 * refereeRepository.findById(11l) >> Optional.of(pickedReferee)
        1 * refereeService.getAvailableRefereesForQueue(queue) >> []
        // Nothing is left to auto-staff, so the dateless match never reaches the rules.
        1 * staffingRuleChecker.rulesFor([], []) >> rules([], [])
        1 * matchConverter.convertFromEntities([datelessMatch])
        1 * configurationRepository.findAllAsMap() >> allOnesConfig()
    }

    def "should reject lock referencing a match that is not assignable in the queue"() {
        given:
        short queue = 4
        def assignableMatch = Match.builder().id(1l).build()
        def locks = [new StaffingLockRequest(99l, 11l)]

        when:
        stafferService.staffReferees(queue, locks)

        then:
        1 * matchService.getMatchesToAssignInQueue(queue) >> [assignableMatch]
        1 * matchRepository.findAllByQueue(queue) >> [assignableMatch]
        0 * refereeRepository.findById(_)
        0 * refereeService.getAvailableRefereesForQueue(_)
        def exception = thrown(StafferException)
        exception.message == String.format(StafferService.LOCKED_MATCH_NOT_ASSIGNABLE, 99l, queue)
        assignableMatch.referee == null
    }

    def "should reject lock pinning a referee who keeps a non-reassignable match in the queue"() {
        given:
        short queue = 4
        def busyReferee = Referee.builder().id(11l).firstName("Busy").lastName("Referee").build()
        def assignableMatch = Match.builder().id(1l).build()
        def finishedMatch = Match.builder()
                .id(2l)
                .referee(busyReferee)
                .homeScore((short) 1).awayScore((short) 0)
                .build()
        def locks = [new StaffingLockRequest(1l, 11l)]

        when:
        stafferService.staffReferees(queue, locks)

        then:
        1 * matchService.getMatchesToAssignInQueue(queue) >> [assignableMatch]
        1 * matchRepository.findAllByQueue(queue) >> [assignableMatch, finishedMatch]
        0 * refereeRepository.findById(_)
        def exception = thrown(StafferException)
        exception.message == String.format(StafferService.LOCKED_REFEREE_UNAVAILABLE, 11l, queue)
        assignableMatch.referee == null
        finishedMatch.referee == busyReferee
    }

    def "should reject duplicate locks"() {
        given:
        short queue = 4
        def matches = [Match.builder().id(1l).build(), Match.builder().id(2l).build()]

        when:
        stafferService.staffReferees(queue, locks)

        then:
        1 * matchService.getMatchesToAssignInQueue(queue) >> matches
        1 * matchRepository.findAllByQueue(queue) >> matches
        0 * refereeRepository.findById(_)
        def exception = thrown(StafferException)
        exception.message == expectedMessage

        where:
        locks                                                              | expectedMessage
        [new StaffingLockRequest(1l, 11l), new StaffingLockRequest(1l, 12l)] | String.format(StafferService.DUPLICATE_LOCKED_MATCH, 1l)
        [new StaffingLockRequest(1l, 11l), new StaffingLockRequest(2l, 11l)] | String.format(StafferService.DUPLICATE_LOCKED_REFEREE, 11l)
    }

    def "should throw RefereeNotFoundException when locked referee does not exist"() {
        given:
        short queue = 4
        def match = Match.builder().id(1l).build()
        def locks = [new StaffingLockRequest(1l, 77l)]

        when:
        stafferService.staffReferees(queue, locks)

        then:
        1 * matchService.getMatchesToAssignInQueue(queue) >> [match]
        1 * matchRepository.findAllByQueue(queue) >> [match]
        1 * refereeRepository.findById(77l) >> Optional.empty()
        0 * refereeService.getAvailableRefereesForQueue(_)
        thrown(RefereeNotFoundException)
    }

    def "should report violations only for the conflicting candidate pairs"() {
        given:
        short queue = 7
        def matchDateTime = LocalDateTime.of(2026, 5, 4, 15, 0)
        def ref1 = Referee.builder().id(1L).firstName("Anna").lastName("Nowak").build()
        def ref2 = Referee.builder().id(2L).firstName("Jan").lastName("Kowalski").build()
        def referees = [ref1, ref2]
        def match1 = Match.builder().id(11L).queue(queue).date(matchDateTime).build()
        def match2 = Match.builder().id(12L).queue(queue).date(matchDateTime.plusDays(1)).build()
        def matches = [match1, match2]
        // ref1 is away on match1's day only; ref2 is clean for both days.
        def vacations = [1L: [Vacation.builder().referee(ref1)
                                      .startDate(matchDateTime.toLocalDate())
                                      .endDate(matchDateTime.toLocalDate()).build()]]

        when:
        def result = stafferService.findCandidateViolationsForQueue(queue)

        then:
        // The cheap lookup, not getMatchesToAssignInQueue: nothing here reads hardnessLvl, and
        // ranking would cost a league-table pass over every finished match.
        1 * matchService.getAssignableMatchesInQueue(queue) >> matches
        0 * matchService.getMatchesToAssignInQueue(_)
        // Every staffable referee, not the queue's availability pool — the pool would make the
        // answer depend on whether the staffing POST has committed yet.
        1 * refereeService.getStaffableReferees() >> referees
        0 * refereeService.getAvailableRefereesForQueue(_)
        1 * staffingRuleChecker.rulesFor(matches, referees) >> rules(matches, referees, [:], vacations)
        // Clean pairs are left out entirely — only (match1, ref1) conflicts.
        result.size() == 1
        result[0].matchId() == 11L
        result[0].refereeId() == 1L
        result[0].violations()*.rule() == [StaffingRule.VACATION]
    }

    def "should cover a referee who already holds a match in the queue"() {
        given:
        short queue = 7
        def matchDateTime = LocalDateTime.of(2026, 5, 4, 15, 0)
        // Busy is not in the queue's availability pool at all, yet the matrix must still judge
        // them: the frontend's candidate list is built from a separately-fetched pool, and a
        // warning that goes missing because of request ordering is worse than no warning.
        def busy = Referee.builder().id(1L).firstName("Busy").lastName("Referee").build()
        def match = Match.builder().id(11L).queue(queue).date(matchDateTime).build()
        def heldMatch = Match.builder().id(12L).queue(queue).referee(busy).date(matchDateTime.plusDays(1)).build()

        when:
        def result = stafferService.findCandidateViolationsForQueue(queue)

        then:
        1 * matchService.getAssignableMatchesInQueue(queue) >> [match]
        1 * refereeService.getStaffableReferees() >> [busy]
        1 * staffingRuleChecker.rulesFor([match], [busy]) >> rules([match], [busy], [1L: [heldMatch]])
        result.size() == 1
        result[0].violations()*.rule() == [StaffingRule.DOUBLE_MATCH_IN_QUEUE]
    }

    def "should leave a match without a date out of the violations matrix"() {
        given:
        short queue = 7
        def matchDateTime = LocalDateTime.of(2026, 5, 4, 15, 0)
        def referee = Referee.builder().id(1L).firstName("Anna").lastName("Nowak").build()
        def datedMatch = Match.builder().id(11L).queue(queue).date(matchDateTime).build()
        def datelessMatch = Match.builder().id(12L).queue(queue).build()
        def vacations = [1L: [Vacation.builder().referee(referee)
                                      .startDate(matchDateTime.toLocalDate())
                                      .endDate(matchDateTime.toLocalDate()).build()]]

        when:
        def result = stafferService.findCandidateViolationsForQueue(queue)

        then:
        1 * matchService.getAssignableMatchesInQueue(queue) >> [datedMatch, datelessMatch]
        1 * refereeService.getStaffableReferees() >> [referee]
        // rulesFor rejects a dateless match, so it must never be handed one: a single such
        // match would otherwise fail the whole endpoint and wipe every warning in the queue.
        1 * staffingRuleChecker.rulesFor([datedMatch], [referee]) >> rules([datedMatch], [referee], [:], vacations)
        result*.matchId() == [11L]
    }

    def "should report no violations for a queue with nothing to staff"() {
        given:
        short queue = 7

        when:
        def result = stafferService.findCandidateViolationsForQueue(queue)

        then:
        1 * matchService.getAssignableMatchesInQueue(queue) >> []
        1 * refereeService.getStaffableReferees() >> []
        1 * staffingRuleChecker.rulesFor([], []) >> rules([], [])
        result.isEmpty()
    }

    private static Map<ConfigName, Double> allOnesConfig() {
        [
                (ConfigName.AVERAGE_GRADE_MULTIPLIER)     : 1.0d,
                (ConfigName.EXPERIENCE_MULTIPLIER)        : 1.0d,
                (ConfigName.NUMBER_OF_MATCHES_MULTIPLIER) : 1.0d,
                (ConfigName.HOME_TEAM_REFEREED_MULTIPLIER): 1.0d,
                (ConfigName.AWAY_TEAM_REFEREED_MULTIPLIER): 1.0d
        ]
    }
}
