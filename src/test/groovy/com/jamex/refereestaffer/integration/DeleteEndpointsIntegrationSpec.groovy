package com.jamex.refereestaffer.integration

import com.jamex.refereestaffer.model.entity.Grade
import com.jamex.refereestaffer.model.entity.Match
import com.jamex.refereestaffer.model.entity.Referee
import com.jamex.refereestaffer.model.entity.Team
import com.jamex.refereestaffer.model.entity.Vacation
import com.jamex.refereestaffer.model.exception.EntityInUseException
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

import java.time.LocalDate
import java.time.LocalDateTime

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete

/**
 * Per-id delete endpoints against real H2 (RS-117).
 *
 * The controller slices mock the repositories away, so they can never observe a
 * referential integrity violation — which is exactly why {@code DELETE /api/referees/{id}}
 * and {@code DELETE /api/teams/{id}} answered 500 for months. This spec drives full HTTP
 * requests through the real repositories, so a dependency check that only looks right
 * against a mock still fails here.
 *
 * {@code @Isolated} because the in-memory H2 is shared JVM-wide with the other integration
 * specs and setup() wipes the domain tables; {@code SAME_THREAD} on top because
 * {@code @Isolated} only fences off OTHER specs — features of this one would still run
 * concurrently and wipe each other's data.
 */
@Isolated
@Execution(ExecutionMode.SAME_THREAD)
@AutoConfigureMockMvc
@SpringBootTest
class DeleteEndpointsIntegrationSpec extends Specification {

    @Autowired MockMvc mockMvc
    @Autowired TeamRepository teamRepository
    @Autowired RefereeRepository refereeRepository
    @Autowired MatchRepository matchRepository
    @Autowired GradeRepository gradeRepository
    @Autowired VacationRepository vacationRepository

    def jsonSlurper = new JsonSlurper()

    def setup() {
        wipeDomainData()
    }

    def cleanup() {
        // The H2 instance is shared JVM-wide. A feature that fails mid-way would otherwise
        // leave grade/vacation rows behind, and StafferIntegrationSpec.setup() does not
        // touch those two tables - the failure would resurface there as an unrelated
        // FK error. Leave only the data.sql config rows for whoever runs next.
        wipeDomainData()
    }

    def "should reject deleting a referee with assigned matches instead of failing on the foreign key"() {
        given:
        def home = teamRepository.save(new Team("Team1", "City1"))
        def away = teamRepository.save(new Team("Team2", "City2"))
        def referee = refereeRepository.save(new Referee("John", "Doe", "john@doe.com", 5))
        matchRepository.save(new Match((short) 1, home, away,
                LocalDateTime.of(2026, 3, 1, 12, 0), referee, (short) 2, (short) 1))
        matchRepository.save(new Match((short) 2, away, home,
                LocalDateTime.of(2026, 3, 8, 12, 0), referee, null, null))

        when:
        def response = mockMvc.perform(delete("/api/referees/$referee.id")).andReturn().response

        then: "409 naming both blocking matches, and the referee is still there"
        response.status == 409
        jsonSlurper.parseText(response.contentAsString).detail ==
                String.format(EntityInUseException.REFEREE_HAS_MATCHES, referee.id, 2L)
        refereeRepository.findById(referee.id).isPresent()
        matchRepository.count() == 2
    }

    def "should delete a referee with no matches together with their vacations"() {
        given:
        def referee = refereeRepository.save(new Referee("Jane", "Roe", "jane@roe.com", 3))
        vacationRepository.save(new Vacation(null, referee,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 14)))

        when:
        def response = mockMvc.perform(delete("/api/referees/$referee.id")).andReturn().response

        then: "the vacation goes with the referee — without that the FK would reject the delete"
        response.status == 200
        refereeRepository.findById(referee.id).isEmpty()
        vacationRepository.count() == 0
    }

    def "should reject deleting a team that takes part in matches"() {
        given:
        def home = teamRepository.save(new Team("Team1", "City1"))
        def away = teamRepository.save(new Team("Team2", "City2"))
        matchRepository.save(new Match((short) 1, home, away,
                LocalDateTime.of(2026, 3, 1, 12, 0), null, null, null))

        when: "the team is deleted from the away side, which is the other half of the FK pair"
        def response = mockMvc.perform(delete("/api/teams/$away.id")).andReturn().response

        then:
        response.status == 409
        jsonSlurper.parseText(response.contentAsString).detail ==
                String.format(EntityInUseException.TEAM_HAS_MATCHES, away.id, 1L)
        teamRepository.findById(away.id).isPresent()
    }

    def "should delete a team that takes part in no match"() {
        given:
        def team = teamRepository.save(new Team("Spare", "City3"))

        when:
        def response = mockMvc.perform(delete("/api/teams/$team.id")).andReturn().response

        then:
        response.status == 200
        teamRepository.findById(team.id).isEmpty()
    }

    def "should answer 404 for a per-id delete of an entity that does not exist"() {
        when:
        def response = mockMvc.perform(delete(path)).andReturn().response

        then:
        response.status == 404

        where:
        path << ["/api/referees/999999", "/api/teams/999999", "/api/grades/999999"]
    }

    def "should keep the vacations of a referee whose delete is rejected"() {
        given: "the one case where the cascade and the refusal meet in the same method"
        def home = teamRepository.save(new Team("Team1", "City1"))
        def away = teamRepository.save(new Team("Team2", "City2"))
        def referee = refereeRepository.save(new Referee("John", "Doe", "john@doe.com", 5))
        matchRepository.save(new Match((short) 1, home, away,
                LocalDateTime.of(2026, 3, 1, 12, 0), referee, (short) 2, (short) 1))
        vacationRepository.save(new Vacation(null, referee,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 14)))

        when:
        def response = mockMvc.perform(delete("/api/referees/$referee.id")).andReturn().response

        then: "409 wins over the cascade and the transaction rolls back - the vacation is still there"
        response.status == 409
        jsonSlurper.parseText(response.contentAsString).detail ==
                String.format(EntityInUseException.REFEREE_HAS_MATCHES, referee.id, 1L)
        refereeRepository.findById(referee.id).isPresent()
        vacationRepository.count() == 1
    }

    def "should delete a grade and leave its match untouched"() {
        given: "grade is the owning side of the one-to-one, so nothing points at it"
        def home = teamRepository.save(new Team("Team1", "City1"))
        def away = teamRepository.save(new Team("Team2", "City2"))
        def referee = refereeRepository.save(new Referee("John", "Doe", "john@doe.com", 5))
        def match = matchRepository.save(new Match((short) 1, home, away,
                LocalDateTime.of(2026, 3, 1, 12, 0), referee, (short) 2, (short) 1))
        def grade = gradeRepository.save(Grade.builder().value(8.5d).match(match).build())

        when:
        def response = mockMvc.perform(delete("/api/grades/$grade.id")).andReturn().response

        then: "no constraint violation, and the match reads back with no grade"
        response.status == 200
        gradeRepository.findById(grade.id).isEmpty()
        matchRepository.findById(match.id).orElseThrow().grade == null
    }

    private void wipeDomainData() {
        // FK order: grade → match, match → team/referee, vacation → referee. Batch variants
        // issue plain DELETEs without loading entities — deleteAll() would pull each Grade
        // (and, through the EAGER one-to-one, its Match pointing back at the doomed Grade)
        // into the session and fail the flush-time transient-reference check.
        gradeRepository.deleteAllInBatch()
        vacationRepository.deleteAllInBatch()
        matchRepository.deleteAllInBatch()
        refereeRepository.deleteAllInBatch()
        teamRepository.deleteAllInBatch()
    }
}
