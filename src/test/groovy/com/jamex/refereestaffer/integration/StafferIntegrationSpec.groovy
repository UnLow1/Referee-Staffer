package com.jamex.refereestaffer.integration

import com.jamex.refereestaffer.model.entity.Match
import com.jamex.refereestaffer.model.entity.Referee
import com.jamex.refereestaffer.model.entity.Team
import com.jamex.refereestaffer.repository.MatchRepository
import com.jamex.refereestaffer.repository.RefereeRepository
import com.jamex.refereestaffer.repository.TeamRepository
import com.jamex.refereestaffer.service.StafferService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

import java.time.LocalDateTime

/**
 * Integration test for the staffing flow that exercises the real Spring context against H2.
 * The unit tests in {@link com.jamex.refereestaffer.service.StafferServiceSpec} cover the
 * algorithm with mocks — this one's job is to prove that referee assignment actually
 * persists to the database, which is the part dependency-injected mocks can never verify.
 */
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
}
