package com.jamex.refereestaffer.integration

import com.jamex.refereestaffer.model.entity.Grade
import com.jamex.refereestaffer.model.entity.Match
import com.jamex.refereestaffer.model.entity.Referee
import com.jamex.refereestaffer.model.entity.Team
import com.jamex.refereestaffer.model.entity.Vacation
import com.jamex.refereestaffer.repository.ConfigurationRepository
import com.jamex.refereestaffer.repository.GradeRepository
import com.jamex.refereestaffer.repository.MatchRepository
import com.jamex.refereestaffer.repository.RefereeRepository
import com.jamex.refereestaffer.repository.TeamRepository
import com.jamex.refereestaffer.repository.VacationRepository
import groovy.json.JsonSlurper
import org.spockframework.runtime.model.parallel.ExecutionMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Execution
import spock.lang.Isolated
import spock.lang.Specification
import spock.lang.Unroll

import java.time.LocalDate
import java.time.LocalDateTime

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete

/**
 * Regression net for RS-112. The five old {@code DELETE /api/{entity}} endpoints each
 * called {@code repository.deleteAll()} on its own table and four of them returned 500;
 * the controller slices never caught it because they mock the repositories away, so a
 * {@code 1 * repository.deleteAll()} assertion passes no matter what the database says.
 * This spec therefore drives the real thing: full HTTP request → service → H2, over a
 * fully wired domain graph (grade → match → referee/team, vacation → referee) that trips
 * every constraint the old endpoints tripped.
 *
 * <p>{@code @Isolated} because the in-memory H2 is shared JVM-wide with the other
 * integration specs and this one wipes domain tables by design; {@code SAME_THREAD} on
 * top because {@code @Isolated} only fences off OTHER specs — features of this one would
 * still run concurrently and wipe each other's seed data.
 */
@Isolated
@Execution(ExecutionMode.SAME_THREAD)
@AutoConfigureMockMvc
@SpringBootTest
class ClearDataIntegrationSpec extends Specification {

    @Autowired MockMvc mockMvc
    @Autowired TeamRepository teamRepository
    @Autowired RefereeRepository refereeRepository
    @Autowired MatchRepository matchRepository
    @Autowired GradeRepository gradeRepository
    @Autowired VacationRepository vacationRepository
    @Autowired ConfigurationRepository configurationRepository

    def jsonSlurper = new JsonSlurper()

    def setup() {
        wipeDomainData()
        seedDomainData()
    }

    def "should wipe every domain table in one request"() {
        given:
        assert gradeRepository.count() == 2
        assert matchRepository.count() == 2

        when:
        def response = mockMvc.perform(delete("/api/data").param("confirm", "delete-all-data"))
                .andReturn().response

        then: "no 500, no referential integrity violation, nothing left behind"
        response.status == 200
        gradeRepository.count() == 0
        vacationRepository.count() == 0
        matchRepository.count() == 0
        refereeRepository.count() == 0
        teamRepository.count() == 0
    }

    def "should report how many rows were removed"() {
        when:
        def response = mockMvc.perform(delete("/api/data").param("confirm", "delete-all-data"))
                .andReturn().response

        then:
        response.status == 200
        def summary = jsonSlurper.parseText(response.contentAsString)
        summary.grades == 2
        summary.vacations == 1
        summary.matches == 2
        summary.referees == 2
        summary.teams == 4
    }

    def "should leave the configuration seeded by data.sql alone"() {
        given: "data.sql seeds the algorithm weights — staffing breaks without them"
        def configCountBefore = configurationRepository.count()
        assert configCountBefore > 0

        when:
        def response = mockMvc.perform(delete("/api/data").param("confirm", "delete-all-data"))
                .andReturn().response

        then:
        response.status == 200
        configurationRepository.count() == configCountBefore
    }

    def "should keep the data when the confirmation token is missing"() {
        when:
        def response = mockMvc.perform(delete("/api/data")).andReturn().response

        then:
        response.status == 400
        matchRepository.count() == 2
        refereeRepository.count() == 2
        teamRepository.count() == 4
    }

    def "should be idempotent on an already empty database"() {
        given:
        wipeDomainData()

        when:
        def response = mockMvc.perform(delete("/api/data").param("confirm", "delete-all-data"))
                .andReturn().response

        then: "a batch delete over an empty table is a no-op, not a 500"
        response.status == 200
        def summary = jsonSlurper.parseText(response.contentAsString)
        summary.grades == 0
        summary.vacations == 0
        summary.matches == 0
        summary.referees == 0
        summary.teams == 0
    }

    @Unroll
    def "should no longer expose the collection-level delete on /api/#entity"() {
        when:
        def response = mockMvc.perform(delete("/api/" + entity)).andReturn().response

        then: "405, not 404 — the paths still carry GET/POST/PUT mappings, only the bulk delete is gone"
        response.status == 405

        and: "the rows are still there, which is the point: the capability moved to DELETE /api/data"
        matchRepository.count() == 2
        teamRepository.count() == 4

        where:
        entity << ["grades", "matches", "referees", "teams", "vacations"]
    }

    def cleanup() {
        // Leave only the data.sql config rows behind for whoever shares the H2 instance.
        wipeDomainData()
    }

    /**
     * Two matches so both shapes are covered: one finished match carrying a grade and a
     * referee that a vacation also points at, one match with a different team pair. That
     * is enough to arm every constraint the old endpoints hit — grade → match,
     * vacation → referee, match → referee, match → home/away team.
     */
    private void seedDomainData() {
        def home = teamRepository.save(new Team("Home", "Krakow"))
        def away = teamRepository.save(new Team("Away", "Warszawa"))
        def thirdTeam = teamRepository.save(new Team("Third", "Gdansk"))
        def fourthTeam = teamRepository.save(new Team("Fourth", "Poznan"))
        def firstReferee = refereeRepository.save(new Referee("John", "Doe", "john@doe.com", 5))
        def secondReferee = refereeRepository.save(new Referee("Jane", "Roe", "jane@roe.com", 8))

        def firstMatch = matchRepository.save(new Match((short) 1, home, away,
                LocalDateTime.now().minusDays(7), firstReferee, (short) 2, (short) 1))
        def secondMatch = matchRepository.save(new Match((short) 1, thirdTeam, fourthTeam,
                LocalDateTime.now().minusDays(7), secondReferee, (short) 0, (short) 0))
        gradeRepository.save(new Grade(firstMatch, 8.2d))
        gradeRepository.save(new Grade(secondMatch, 7.9d, 8.3d))

        vacationRepository.save(Vacation.builder()
                .referee(firstReferee)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .build())
    }

    private void wipeDomainData() {
        // Deliberately duplicates DataService's delete order instead of autowiring it: the
        // fixture has to hold when the subject is broken. Arranging through clearAllData()
        // would turn a regression in it into a failure in setup()/cleanup() of every
        // feature at once, which reads as a broken spec rather than a broken endpoint.
        // Batch variants so the teardown cannot fail on the flush-time transient-reference
        // check that deleteAll() trips on the bidirectional Grade <-> Match one-to-one.
        gradeRepository.deleteAllInBatch()
        vacationRepository.deleteAllInBatch()
        matchRepository.deleteAllInBatch()
        refereeRepository.deleteAllInBatch()
        teamRepository.deleteAllInBatch()
    }
}
