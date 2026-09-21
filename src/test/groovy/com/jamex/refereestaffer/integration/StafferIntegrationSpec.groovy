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
 * algorithm with mocks — this one's job is to prove that referee assignment actually
 * persists to the database, which is the part dependency-injected mocks can never verify.
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

    def "should persist referee assignment to database after staffing"() {
        given:
        def team1 = teamRepository.save(new Team("Team1", "City1"))
        def team2 = teamRepository.save(new Team("Team2", "City2"))
        def referee = refereeRepository.save(new Referee("John", "Doe", "john@doe.com", 5))
        short queue = 1
        def matchToStaff = matchRepository.save(
                new Match(queue, team1, team2, LocalDateTime.now().plusDays(1), null, null, null))

        when:
        stafferService.staffReferees(queue)

        then:
        // Re-read from the DB rather than trusting the in-memory entity. Without
        // @Transactional on staffReferees the entity returned by the match query becomes
        // detached after the repository call commits, and the subsequent setReferee
        // mutation never gets flushed. Re-fetching forces us to read the persisted state.
        def persisted = matchRepository.findById(matchToStaff.id).orElseThrow()
        persisted.referee != null
        persisted.referee.id == referee.id
    }

    def "should preserve locked assignment and re-staff the rest on regenerate"() {
        given:
        def team1 = teamRepository.save(new Team("Team1", "City1"))
        def team2 = teamRepository.save(new Team("Team2", "City2"))
        def team3 = teamRepository.save(new Team("Team3", "City3"))
        def team4 = teamRepository.save(new Team("Team4", "City4"))
        def strongReferee = refereeRepository.save(new Referee("Strong", "Referee", "strong@referees.com", 10))
        def weakReferee = refereeRepository.save(new Referee("Weak", "Referee", "weak@referees.com", 0))
        short queue = 1
        def match1 = matchRepository.save(
                new Match(queue, team1, team2, LocalDateTime.now().plusDays(1), null, null, null))
        def match2 = matchRepository.save(
                new Match(queue, team3, team4, LocalDateTime.now().plusDays(1), null, null, null))

        and: "a first staffing run has already persisted a full cast"
        stafferService.staffReferees(queue)

        when: "the cast is regenerated with match1 pinned to the weak referee"
        stafferService.staffReferees(queue, [new StaffingLockRequest(match1.id, weakReferee.id)])

        then: "the pinned pair survives and the other match is re-staffed from the remaining pool"
        matchRepository.findById(match1.id).orElseThrow().referee.id == weakReferee.id
        matchRepository.findById(match2.id).orElseThrow().referee.id == strongReferee.id
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

        then: "the central assignment is kept and excluded from the returned cast"
        matchRepository.findById(centralMatch.id).orElseThrow().referee.id == centralReferee.id
        matchRepository.findById(openMatch.id).orElseThrow().referee.id == realReferee.id
        result*.id == [openMatch.id]
    }

    def "should report exactly the assignments a staffing run then clears"() {
        given:
        def team1 = teamRepository.save(new Team("Team1", "City1"))
        def team2 = teamRepository.save(new Team("Team2", "City2"))
        def team3 = teamRepository.save(new Team("Team3", "City3"))
        def team4 = teamRepository.save(new Team("Team4", "City4"))
        def team5 = teamRepository.save(new Team("Team5", "City5"))
        def team6 = teamRepository.save(new Team("Team6", "City6"))
        def team7 = teamRepository.save(new Team("Team7", "City7"))
        def team8 = teamRepository.save(new Team("Team8", "City8"))
        def centralReferee = refereeRepository.save(new Referee("S", "C"))
        def manualReferee = refereeRepository.save(new Referee("Manual", "Pick", "manual@ref.com", 5))
        def finishedReferee = refereeRepository.save(new Referee("Past", "Referee", "past@ref.com", 5))
        // Two clearly stronger referees for the two assignable matches, so the manual pick
        // cannot be re-drawn by chance and the overwrite is unambiguous.
        refereeRepository.save(new Referee("Strong", "One", "strong1@ref.com", 99))
        refereeRepository.save(new Referee("Strong", "Two", "strong2@ref.com", 98))
        short queue = 1
        def matchDay = LocalDateTime.now().plusDays(1)
        def manuallyAssigned = matchRepository.save(
                new Match(queue, team1, team2, matchDay, manualReferee, null, null))
        def centralMatch = matchRepository.save(
                new Match(queue, team3, team4, matchDay, centralReferee, null, null))
        def finishedMatch = matchRepository.save(
                new Match(queue, team5, team6, matchDay, finishedReferee, (short) 2, (short) 1))
        def unassigned = matchRepository.save(
                new Match(queue, team7, team8, matchDay, null, null, null))

        when: "the screen asks what a regenerate would take away"
        def preview = stafferService.getOverwrittenAssignments(queue)

        then: "only the hand-made assignment is at risk"
        preview.queue() == queue
        preview.assignedMatchIds() == [manuallyAssigned.id]

        when: "the regenerate actually runs"
        stafferService.staffReferees(queue)

        then: "exactly the previewed assignment was cleared and re-cast; the rest kept theirs"
        // The point of the feature: the warning and the run read the same set, against a
        // real database rather than a mocked MatchService.
        matchRepository.findById(manuallyAssigned.id).orElseThrow().referee.id != manualReferee.id
        matchRepository.findById(centralMatch.id).orElseThrow().referee.id == centralReferee.id
        matchRepository.findById(finishedMatch.id).orElseThrow().referee.id == finishedReferee.id
        matchRepository.findById(unassigned.id).orElseThrow().referee != null
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
        stafferService.staffReferees(queue)

        then:
        def persisted = matchRepository.findById(matchToStaff.id).orElseThrow()
        persisted.referee != null
        persisted.referee.id == freeReferee.id
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
        stafferService.staffReferees(queue)

        then:
        def persisted = matchRepository.findById(matchToStaff.id).orElseThrow()
        persisted.referee != null
        persisted.referee.id == referee.id
    }
}
