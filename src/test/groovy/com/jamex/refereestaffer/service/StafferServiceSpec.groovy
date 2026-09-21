package com.jamex.refereestaffer.service

import com.jamex.refereestaffer.model.converter.MatchConverter
import com.jamex.refereestaffer.model.dto.MatchDto
import com.jamex.refereestaffer.model.entity.*
import com.jamex.refereestaffer.model.exception.RefereeNotFoundException
import com.jamex.refereestaffer.model.exception.StafferException
import com.jamex.refereestaffer.model.request.StaffingLockRequest
import com.jamex.refereestaffer.repository.ConfigurationRepository
import com.jamex.refereestaffer.repository.MatchRepository
import com.jamex.refereestaffer.repository.RefereeRepository
import com.jamex.refereestaffer.repository.VacationRepository
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDateTime

class StafferServiceSpec extends Specification {

    @Subject
    StafferService stafferService

    ConfigurationRepository configurationRepository = Mock()
    VacationRepository vacationRepository = Mock()
    MatchRepository matchRepository = Mock()
    RefereeRepository refereeRepository = Mock()
    MatchConverter matchConverter = Mock()
    MatchService matchService = Mock()
    RefereeService refereeService = Mock()

    def setup() {
        stafferService = new StafferService(configurationRepository, vacationRepository, matchRepository,
                refereeRepository, matchConverter, matchService, refereeService)
    }

    def "should report assignable matches that already carry an assignment as overwritten"() {
        given:
        short queue = 3
        // Deliberately out of id order: the preview is consumed as a set by the UI, but a
        // stable order keeps the payload (and this expectation) predictable.
        def assignedLate = Match.builder().id(7l).referee(new Referee("John", "Doe")).build()
        def unassigned = Match.builder().id(8l).build()
        def assignedEarly = Match.builder().id(5l).referee(new Referee("Jane", "Smith")).build()

        when:
        def result = stafferService.getOverwrittenAssignments(queue)

        then:
        // Central and finished assignments never reach here — getAssignableMatchesInQueue
        // filters them out, which is exactly why the preview reuses it (MatchServiceSpec
        // covers that filter).
        1 * matchService.getAssignableMatchesInQueue(queue) >> [assignedLate, unassigned, assignedEarly]
        result.queue() == queue
        result.assignedMatchIds() == [5l, 7l]
        // A preview must not touch the cast it describes.
        0 * matchRepository._
        0 * refereeRepository._
    }

    def "should report nothing to overwrite for a queue that has no assignments"() {
        given:
        short queue = 4
        def unassigned = Match.builder().id(1l).build()

        when:
        def result = stafferService.getOverwrittenAssignments(queue)

        then:
        1 * matchService.getAssignableMatchesInQueue(queue) >> [unassigned]
        result.assignedMatchIds().isEmpty()
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
        def matchDateTime = LocalDateTime.of(2022, 10, 12, 16, 0)
        def match1 = Match.builder()
                .home(team1)
                .away(team2)
                .date(matchDateTime)
                .build()
        def match2 = Match.builder()
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
        2 * vacationRepository.findAllByStartDateIsLessThanEqualAndEndDateIsGreaterThanEqual(_) >> []
        2 * matchRepository.findAllByRefereeInAndDateOnDay([ref1, ref2], matchDateTime) >> []
        1 * refereeService.getAvailableRefereesForQueue(queue) >> [ref1, ref2]
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

    def "should not assign referees to matches if referee has vacation"() {
        given:
        def ref1 = Referee.builder().id(1L).averageGrade(8.6d).build()
        def ref2 = Referee.builder().id(2L).averageGrade(RefereeService.DEFAULT_GRADE).teamsRefereed([:]).numberOfMatchesInRound((short) 0).build()
        def ref3 = Referee.builder().id(3L).averageGrade(RefereeService.DEFAULT_GRADE).teamsRefereed([:]).numberOfMatchesInRound((short) 0).build()
        def ref4 = Referee.builder().id(4L).averageGrade(8.6d).build()
        def ref5 = Referee.builder().id(5L).averageGrade(8.6d).build()
        def ref6 = Referee.builder().id(6L).averageGrade(8.6d).build()
        def referees = [ref1, ref2, ref3, ref4, ref5, ref6]
        def matchDateTime = LocalDateTime.of(2022, 10, 12, 16, 0)
        def matchDate = matchDateTime.toLocalDate()
        def match1 = [date: matchDateTime] as Match
        def match2 = [date: matchDateTime] as Match
        def matches = [match1, match2]
        def futureDate = matchDate.plusDays(1)
        def pastDate = matchDate.minusDays(1)
        def vacations = [
                Vacation.builder().referee(ref1).startDate(matchDate).endDate(matchDate).build(),
                Vacation.builder().referee(ref4).startDate(matchDate).endDate(futureDate).build(),
                Vacation.builder().referee(ref5).startDate(pastDate).endDate(matchDate).build(),
                Vacation.builder().referee(ref6).startDate(pastDate).endDate(futureDate).build()]

        when:
        stafferService.staffReferees(2 as short)

        then:
        match1.referee == ref2 || match1.referee == ref3
        match2.referee == ref2 || match2.referee == ref3
        2 * vacationRepository.findAllByStartDateIsLessThanEqualAndEndDateIsGreaterThanEqual(matchDateTime) >> vacations
        2 * matchRepository.findAllByRefereeInAndDateOnDay(referees, matchDateTime) >> []
        1 * refereeService.getAvailableRefereesForQueue(_) >> referees
        1 * refereeService.calculateStats(referees)
        1 * matchService.getMatchesToAssignInQueue(_) >> matches
        1 * matchConverter.convertFromEntities(matches)
        1 * configurationRepository.findAllAsMap() >> [
                (ConfigName.AVERAGE_GRADE_MULTIPLIER)  : 1.0d,
                (ConfigName.EXPERIENCE_MULTIPLIER)     : 1.0d,
                (ConfigName.NUMBER_OF_MATCHES_MULTIPLIER): 1.0d,
                (ConfigName.HOME_TEAM_REFEREED_MULTIPLIER): 1.0d,
                (ConfigName.AWAY_TEAM_REFEREED_MULTIPLIER): 1.0d
        ]
    }

    def "should throw StafferException when no referees are available for queue"() {
        given:
        short queue = 5
        def match = Match.builder()
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
        1 * vacationRepository.findAllByStartDateIsLessThanEqualAndEndDateIsGreaterThanEqual(_) >> []
        // Empty referee pool short-circuits the same-day lookup — no query with an empty IN list.
        0 * matchRepository.findAllByRefereeInAndDateOnDay(_, _)
        0 * matchConverter.convertFromEntities(_)
        thrown(StafferException)
    }

    def "should throw StafferException when all available referees are on vacation for match date"() {
        given:
        short queue = 5
        def referee = [averageGrade: 8.0d, teamsRefereed: [:], numberOfMatchesInRound: 0] as Referee
        def matchDateTime = LocalDateTime.of(2026, 5, 4, 15, 0)
        def matchDate = matchDateTime.toLocalDate()
        def match = Match.builder()
                .home(Team.builder().name("home").build())
                .away(Team.builder().name("away").build())
                .date(matchDateTime)
                .build()
        def vacation = Vacation.builder()
                .referee(referee)
                .startDate(matchDate)
                .endDate(matchDate)
                .build()

        when:
        stafferService.staffReferees(queue)

        then:
        1 * refereeService.getAvailableRefereesForQueue(queue) >> [referee]
        1 * refereeService.calculateStats([referee])
        1 * matchService.getMatchesToAssignInQueue(queue) >> [match]
        1 * configurationRepository.findAllAsMap() >> [:]
        1 * vacationRepository.findAllByStartDateIsLessThanEqualAndEndDateIsGreaterThanEqual(matchDateTime) >> [vacation]
        1 * matchRepository.findAllByRefereeInAndDateOnDay([referee], matchDateTime) >> []
        0 * matchConverter.convertFromEntities(_)
        thrown(StafferException)
    }

    def "should not assign referee who already has a match on the same day"() {
        given:
        short queue = 3
        def matchDateTime = LocalDateTime.of(2026, 5, 4, 15, 0)
        // ref1 would win on potential, but already officiates a match rescheduled onto this day
        def ref1 = [averageGrade: 9.0d, teamsRefereed: [:], numberOfMatchesInRound: 0] as Referee
        def ref2 = [averageGrade: 7.0d, teamsRefereed: [:], numberOfMatchesInRound: 0] as Referee
        def referees = [ref1, ref2]
        def match = Match.builder()
                .home(Team.builder().name("home").build())
                .away(Team.builder().name("away").build())
                .date(matchDateTime)
                .build()
        def conflictingMatch = Match.builder()
                .queue((short) 2)
                .referee(ref1)
                .date(LocalDateTime.of(2026, 5, 4, 11, 0))
                .build()

        when:
        stafferService.staffReferees(queue)

        then:
        match.referee == ref2
        1 * refereeService.getAvailableRefereesForQueue(queue) >> referees
        1 * refereeService.calculateStats(referees)
        1 * matchService.getMatchesToAssignInQueue(queue) >> [match]
        1 * configurationRepository.findAllAsMap() >> [
                (ConfigName.AVERAGE_GRADE_MULTIPLIER)  : 1.0d,
                (ConfigName.EXPERIENCE_MULTIPLIER)     : 0.0d,
                (ConfigName.NUMBER_OF_MATCHES_MULTIPLIER): 0.0d,
                (ConfigName.HOME_TEAM_REFEREED_MULTIPLIER): 0.0d,
                (ConfigName.AWAY_TEAM_REFEREED_MULTIPLIER): 0.0d
        ]
        1 * vacationRepository.findAllByStartDateIsLessThanEqualAndEndDateIsGreaterThanEqual(matchDateTime) >> []
        1 * matchRepository.findAllByRefereeInAndDateOnDay(referees, matchDateTime) >> [conflictingMatch]
        1 * matchConverter.convertFromEntities([match])
    }

    def "should throw StafferException when all available referees have a match on the same day"() {
        given:
        short queue = 3
        def matchDateTime = LocalDateTime.of(2026, 5, 4, 15, 0)
        def referee = [averageGrade: 8.0d, teamsRefereed: [:], numberOfMatchesInRound: 0] as Referee
        def match = Match.builder()
                .home(Team.builder().name("home").build())
                .away(Team.builder().name("away").build())
                .date(matchDateTime)
                .build()
        def conflictingMatch = Match.builder()
                .queue((short) 2)
                .referee(referee)
                .date(LocalDateTime.of(2026, 5, 4, 11, 0))
                .build()

        when:
        stafferService.staffReferees(queue)

        then:
        1 * refereeService.getAvailableRefereesForQueue(queue) >> [referee]
        1 * refereeService.calculateStats([referee])
        1 * matchService.getMatchesToAssignInQueue(queue) >> [match]
        1 * configurationRepository.findAllAsMap() >> [:]
        1 * vacationRepository.findAllByStartDateIsLessThanEqualAndEndDateIsGreaterThanEqual(matchDateTime) >> []
        1 * matchRepository.findAllByRefereeInAndDateOnDay([referee], matchDateTime) >> [conflictingMatch]
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
        // Match.date is nullable = false in the entity, and the staffer queries same-day
        // matches per candidate, so the fixtures carry a real date.
        def matchDateTime = LocalDateTime.of(2026, 5, 4, 11, 0)
        def lockedMatch = Match.builder().id(1l).home(team1).away(team2).date(matchDateTime).build()
        def otherMatch = Match.builder().id(2l).home(team2).away(team1).date(matchDateTime).build()
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
        1 * vacationRepository.findAllByStartDateIsLessThanEqualAndEndDateIsGreaterThanEqual(_) >> []
        1 * matchRepository.findAllByRefereeInAndDateOnDay([freeReferee], matchDateTime) >> []
        1 * matchConverter.convertFromEntities([lockedMatch, otherMatch])
        1 * configurationRepository.findAllAsMap() >> allOnesConfig()
    }

    def "should clear stale assignment and re-staff the match on regenerate"() {
        given:
        short queue = 3
        def staleReferee = Referee.builder().id(1l).firstName("Stale").lastName("Assignment").build()
        def newReferee = Referee.builder().id(2l).averageGrade(8.0d).teamsRefereed([:]).numberOfMatchesInRound((short) 0).build()
        // Match.date is nullable = false in the entity, and the staffer queries same-day
        // matches per candidate, so the fixture carries a real date.
        def matchDateTime = LocalDateTime.of(2026, 5, 4, 11, 0)
        def match = Match.builder()
                .id(5l)
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
        1 * vacationRepository.findAllByStartDateIsLessThanEqualAndEndDateIsGreaterThanEqual(_) >> []
        1 * matchRepository.findAllByRefereeInAndDateOnDay([newReferee], matchDateTime) >> []
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
