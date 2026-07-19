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
import spock.lang.Specification

import java.time.LocalDateTime

/**
 * Integration test for the staffing flow that exercises the real Spring context against H2.
 * The unit tests in {@link com.jamex.refereestaffer.service.StafferServiceSpec} cover the
 * algorithm with mocks — this one's job is to prove that referee assignment actually
 * persists to the database, which is the part dependency-injected mocks can never verify.
 */
// Features must run on one thread: they share the single cached H2 instance and setup()
// wipes the domain tables, so Spock's parallel mode would let features delete each
// other's fixtures mid-flight.
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
        // @Transactional on staffReferees the entity returned from findAllByQueueAndRefereeIsNull
        // becomes detached after the repository call commits, and the subsequent setReferee
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
}
