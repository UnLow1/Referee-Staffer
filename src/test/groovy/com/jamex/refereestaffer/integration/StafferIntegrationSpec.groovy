package com.jamex.refereestaffer.integration

import com.jamex.refereestaffer.model.entity.Match
import com.jamex.refereestaffer.model.entity.Referee
import com.jamex.refereestaffer.model.entity.Team
import com.jamex.refereestaffer.model.request.StaffingLockRequest
import com.jamex.refereestaffer.repository.MatchRepository
import com.jamex.refereestaffer.repository.RefereeRepository
import com.jamex.refereestaffer.repository.TeamRepository
import com.jamex.refereestaffer.service.StafferService
import org.spockframework.runtime.model.parallel.ExecutionMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Execution
import spock.lang.Isolated
import spock.lang.Specification

import java.time.LocalDateTime

/**
 * Integration test for the staffing flow that exercises the real Spring context against H2.
 * The unit tests in {@link com.jamex.refereestaffer.service.StafferServiceSpec} cover the
 * algorithm with mocks — this one's job is to prove what mocks can never prove: that
 * generating a cast leaves the database alone (RS-105), and that a cast already stored there
 * neither blocks a regenerate nor gets silently overwritten by one.
 *
 * <p>{@code @Isolated} because the in-memory H2 is shared JVM-wide with the other
 * integration specs and setup() wipes the domain tables; {@code SAME_THREAD} on top
 * because {@code @Isolated} only fences off OTHER specs — features of this one would
 * still run concurrently and wipe each other's data. Both annotations have to be Spock's
 * own ({@code spock.lang}); the JUnit Jupiter ones are silently ignored by the Spock engine.
 */
@Isolated
@Execution(ExecutionMode.SAME_THREAD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class StafferIntegrationSpec extends Specification {

    @Autowired StafferService stafferService
    @Autowired MatchRepository matchRepository
    @Autowired RefereeRepository refereeRepository
    @Autowired TeamRepository teamRepository

    def setup() {
        // Spring caches the application context across test classes, and H2 in-memory state
        // carries with it. Wipe domain data on each test so we start clean. Configuration
        // rows seeded by data.sql are intentionally untouched — staffReferees needs them.
        matchRepository.deleteAll()
        refereeRepository.deleteAll()
        teamRepository.deleteAll()
    }

    def "should return the generated cast without writing it to the database"() {
        given:
        def team1 = teamRepository.save(new Team("Team1", "City1"))
        def team2 = teamRepository.save(new Team("Team2", "City2"))
        def referee = refereeRepository.save(new Referee("John", "Doe", "john@doe.com", 5))
        short queue = 1
        def matchToStaff = matchRepository.save(
                new Match(queue, team1, team2, LocalDateTime.now().plusDays(1), null, null, null))

        when:
        def result = stafferService.staffReferees(queue)

        then: "the draft carries the assignment"
        result*.id == [matchToStaff.id]
        result*.refereeId == [referee.id]

        and: "but the database is untouched — only Save cast persists a cast"
        matchRepository.findById(matchToStaff.id).orElseThrow().referee == null
    }

    def "should not overwrite a saved cast when regenerating"() {
        given:
        def team1 = teamRepository.save(new Team("Team1", "City1"))
        def team2 = teamRepository.save(new Team("Team2", "City2"))
        def team3 = teamRepository.save(new Team("Team3", "City3"))
        def team4 = teamRepository.save(new Team("Team4", "City4"))
        def strongReferee = refereeRepository.save(new Referee("Strong", "Referee", "strong@referees.com", 10))
        def weakReferee = refereeRepository.save(new Referee("Weak", "Referee", "weak@referees.com", 0))
        short queue = 1
        def matchDate = LocalDateTime.now().plusDays(1)
        and: "a cast saved by hand, the way Save cast persists one"
        def savedMatch1 = matchRepository.save(new Match(queue, team1, team2, matchDate, weakReferee, null, null))
        def savedMatch2 = matchRepository.save(new Match(queue, team3, team4, matchDate, strongReferee, null, null))

        when:
        def result = stafferService.staffReferees(queue)

        then: "the saved cast survives the regenerate untouched"
        matchRepository.findById(savedMatch1.id).orElseThrow().referee.id == weakReferee.id
        matchRepository.findById(savedMatch2.id).orElseThrow().referee.id == strongReferee.id

        and: "and both referees were still available to the draft — a saved cast is not a booking"
        result*.refereeId as Set == [weakReferee.id, strongReferee.id] as Set
    }

    def "should preserve locked assignment and re-staff the rest in the returned draft"() {
        given:
        def team1 = teamRepository.save(new Team("Team1", "City1"))
        def team2 = teamRepository.save(new Team("Team2", "City2"))
        def team3 = teamRepository.save(new Team("Team3", "City3"))
        def team4 = teamRepository.save(new Team("Team4", "City4"))
        def strongReferee = refereeRepository.save(new Referee("Strong", "Referee", "strong@referees.com", 10))
        def weakReferee = refereeRepository.save(new Referee("Weak", "Referee", "weak@referees.com", 0))
        short queue = 1
        def matchDate = LocalDateTime.now().plusDays(1)
        def match1 = matchRepository.save(new Match(queue, team1, team2, matchDate, null, null, null))
        def match2 = matchRepository.save(new Match(queue, team3, team4, matchDate, null, null, null))

        when: "the cast is generated with match1 pinned to the weak referee"
        def result = stafferService.staffReferees(queue, [new StaffingLockRequest(match1.id, weakReferee.id)])

        then: "the pinned pair survives and the other match is staffed from the remaining pool"
        def byMatchId = result.collectEntries { [(it.id()): it.refereeId()] }
        byMatchId[match1.id] == weakReferee.id
        byMatchId[match2.id] == strongReferee.id
    }

    def "should not touch central assignments when regenerating"() {
        given:
        def team1 = teamRepository.save(new Team("Team1", "City1"))
        def team2 = teamRepository.save(new Team("Team2", "City2"))
        def team3 = teamRepository.save(new Team("Team3", "City3"))
        def team4 = teamRepository.save(new Team("Team4", "City4"))
        def centralReferee = refereeRepository.save(new Referee("S", "C"))
        def realReferee = refereeRepository.save(new Referee("John", "Doe", "john@doe.com", 5))
        short queue = 1
        def centralMatch = matchRepository.save(
                new Match(queue, team1, team2, LocalDateTime.now().plusDays(1), centralReferee, null, null))
        def openMatch = matchRepository.save(
                new Match(queue, team3, team4, LocalDateTime.now().plusDays(1), null, null, null))

        when:
        def result = stafferService.staffReferees(queue)

        then: "the central assignment is kept in the database and excluded from the cast"
        matchRepository.findById(centralMatch.id).orElseThrow().referee.id == centralReferee.id
        result*.id == [openMatch.id]
        result*.refereeId == [realReferee.id]
    }

    def "should not offer a referee kept by a finished match in the same queue"() {
        given:
        def team1 = teamRepository.save(new Team("Team1", "City1"))
        def team2 = teamRepository.save(new Team("Team2", "City2"))
        def team3 = teamRepository.save(new Team("Team3", "City3"))
        def team4 = teamRepository.save(new Team("Team4", "City4"))
        // Higher experience would win on potential if the finished match did not keep them.
        def keptReferee = refereeRepository.save(new Referee("Kept", "Referee", "kept@ref.com", 99))
        def freeReferee = refereeRepository.save(new Referee("Free", "Referee", "free@ref.com", 1))
        short queue = 1
        matchRepository.save(new Match(queue, team3, team4, LocalDateTime.now().minusDays(1), keptReferee,
                (short) 2, (short) 1))
        def matchToStaff = matchRepository.save(
                new Match(queue, team1, team2, LocalDateTime.now().plusDays(1), null, null, null))

        when:
        def result = stafferService.staffReferees(queue)

        then:
        result*.id == [matchToStaff.id]
        result*.refereeId == [freeReferee.id]
    }

    def "should skip referee who already has a match on the same day in another queue"() {
        given:
        def team1 = teamRepository.save(new Team("Team1", "City1"))
        def team2 = teamRepository.save(new Team("Team2", "City2"))
        def team3 = teamRepository.save(new Team("Team3", "City3"))
        def team4 = teamRepository.save(new Team("Team4", "City4"))
        // Higher experience means the busy referee would win on potential without the
        // same-day check (data.sql: EXPERIENCE_MULTIPLIER = 0.01, grades are equal).
        def busyReferee = refereeRepository.save(new Referee("Busy", "Referee", "busy@ref.com", 99))
        def freeReferee = refereeRepository.save(new Referee("Free", "Referee", "free@ref.com", 1))
        short queue = 2
        def matchDay = LocalDateTime.of(2026, 9, 12, 15, 0)
        // A queue-1 match rescheduled onto the same day the staffed match is played
        matchRepository.save(new Match((short) 1, team3, team4, matchDay.minusHours(4), busyReferee, null, null))
        def matchToStaff = matchRepository.save(new Match(queue, team1, team2, matchDay, null, null, null))

        when:
        def result = stafferService.staffReferees(queue)

        then:
        result*.id == [matchToStaff.id]
        result*.refereeId == [freeReferee.id]
    }

    def "should assign referee whose other matches are on adjacent days"() {
        given:
        def team1 = teamRepository.save(new Team("Team1", "City1"))
        def team2 = teamRepository.save(new Team("Team2", "City2"))
        def team3 = teamRepository.save(new Team("Team3", "City3"))
        def team4 = teamRepository.save(new Team("Team4", "City4"))
        def referee = refereeRepository.save(new Referee("John", "Doe", "john@doe.com", 5))
        short queue = 2
        def matchDay = LocalDateTime.of(2026, 9, 12, 15, 0)
        // Probe both edges of the [dayStart, nextDayStart) window: a late-evening match the
        // day before and a midnight match the day after must not block the assignment.
        matchRepository.save(new Match((short) 1, team3, team4, LocalDateTime.of(2026, 9, 11, 23, 0), referee, null, null))
        matchRepository.save(new Match((short) 3, team4, team3, LocalDateTime.of(2026, 9, 13, 0, 0), referee, null, null))
        def matchToStaff = matchRepository.save(new Match(queue, team1, team2, matchDay, null, null, null))

        when:
        def result = stafferService.staffReferees(queue)

        then:
        result*.id == [matchToStaff.id]
        result*.refereeId == [referee.id]
    }

    def "should return the stored cast without regenerating it"() {
        given:
        def team1 = teamRepository.save(new Team("Team1", "City1"))
        def team2 = teamRepository.save(new Team("Team2", "City2"))
        def team3 = teamRepository.save(new Team("Team3", "City3"))
        def team4 = teamRepository.save(new Team("Team4", "City4"))
        def savedReferee = refereeRepository.save(new Referee("Saved", "Referee", "saved@ref.com", 5))
        def centralReferee = refereeRepository.save(new Referee("S", "C"))
        short queue = 3
        def matchDate = LocalDateTime.now().plusDays(1)
        def savedMatch = matchRepository.save(new Match(queue, team1, team2, matchDate, savedReferee, null, null))
        matchRepository.save(new Match(queue, team3, team4, matchDate, centralReferee, null, null))
        matchRepository.save(new Match((short) 4, team1, team3, matchDate, null, null, null))

        when:
        def result = stafferService.getStoredCast(queue)

        then: "the stored assignment comes back as-is, central matches stay out of the cast"
        result*.id == [savedMatch.id]
        result*.refereeId == [savedReferee.id]

        and: "and reading a cast changes nothing"
        matchRepository.findById(savedMatch.id).orElseThrow().referee.id == savedReferee.id
    }

    def "should report an empty cast for a queue that has been played"() {
        given:
        def team1 = teamRepository.save(new Team("Team1", "City1"))
        def team2 = teamRepository.save(new Team("Team2", "City2"))
        def referee = refereeRepository.save(new Referee("John", "Doe", "john@doe.com", 5))
        short queue = 5
        def playedMatch = matchRepository.save(new Match(queue, team1, team2, LocalDateTime.now().minusDays(7),
                referee, (short) 1, (short) 1))

        when:
        def result = stafferService.getStoredCast(queue)

        then: "a played match is nobody's to re-decide, so it is not part of the cast"
        result.isEmpty()

        and: "its assignment stays in the database — the assignment sheet renders the whole queue"
        matchRepository.findById(playedMatch.id).orElseThrow().referee.id == referee.id
        matchRepository.findAllByQueueOrderByDateAsc(queue)*.id == [playedMatch.id]
    }
}
