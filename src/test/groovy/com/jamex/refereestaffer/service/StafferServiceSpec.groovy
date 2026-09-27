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
                matches.collect { it.queue }.findAll() as Set,
                referees.collect { it.id }.findAll() as Set,
                refereeMatches, vacations)
    }

    def "should assign referees to matches in queue"() {
        given:
        short queue = 2
        def team1 = Team.builder()
                .name("test team")
                .build()
        def team2 = Team.builder()
                .name("test team 123213")
                .build()
        // After RefereeService.calculateStats, averageGrade is always non-null — the
        // no-grades fallback (DEFAULT_GRADE = 8.3) is applied there. Setting it
        // explicitly here mirrors the real invariant. Ids are needed because the
        // staffer deduplicates already-assigned referees by id.
        def ref1 = Referee.builder()
                .id(1L)
                .averageGrade(8.1d)
                .teamsRefereed(Map.of(team1, (short) 1, team2, (short) 1))
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

    def "should pin locked pair and auto-staff only the remaining matches"() {
        given:
        short queue = 3
        def team1 = Team.builder().name("team A").build()
        def team2 = Team.builder().name("team B").build()
        def lockedReferee = Referee.builder().id(11l).firstName("Locked").lastName("Referee").build()
        def freeReferee = Referee.builder().id(22l).averageGrade(8.0d).teamsRefereed([:]).numberOfMatchesInRound((short) 0).build()
        // Match.date is nullable = false in the entity, and the rule snapshot is keyed by
        // match day, so the fixtures carry a real date.
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
        1 * matchService.getMatchesToAssignInQueue(queue) >> matches
        1 * refereeService.getAvailableRefereesForQueue(queue) >> referees
        1 * staffingRuleChecker.rulesFor(matches, referees) >> rules(matches, referees, [:], vacations)
        // Clean pairs are left out entirely — only (match1, ref1) conflicts.
        result.size() == 1
        result[0].matchId() == 11L
        result[0].refereeId() == 1L
        result[0].violations()*.rule() == [StaffingRule.VACATION]
    }

    def "should report no violations for a queue with nothing to staff"() {
        given:
        short queue = 7

        when:
        def result = stafferService.findCandidateViolationsForQueue(queue)

        then:
        1 * matchService.getMatchesToAssignInQueue(queue) >> []
        1 * refereeService.getAvailableRefereesForQueue(queue) >> []
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
