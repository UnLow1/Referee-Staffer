package com.jamex.refereestaffer.integration

import com.jamex.refereestaffer.model.entity.Match
import com.jamex.refereestaffer.model.entity.Referee
import com.jamex.refereestaffer.model.entity.Team
import com.jamex.refereestaffer.repository.MatchRepository
import com.jamex.refereestaffer.repository.RefereeRepository
import com.jamex.refereestaffer.repository.TeamRepository
import com.jamex.refereestaffer.service.StafferService
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

import java.time.LocalDateTime

/**
 * Integration test for the staffing flow that exercises the real Spring context against H2.
 * The unit tests in {@link com.jamex.refereestaffer.service.StafferServiceSpec} cover the
 * algorithm with mocks — this one's job is to prove that referee assignment actually
 * persists to the database, which is the part dependency-injected mocks can never verify.
 *
 * <p>SAME_THREAD because all features share the one H2 instance behind the cached Spring
 * context and setup() wipes it — Spock's parallel mode would let features race on that state.
 */
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
