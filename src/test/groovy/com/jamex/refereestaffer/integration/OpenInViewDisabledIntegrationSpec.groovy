package com.jamex.refereestaffer.integration

import com.jamex.refereestaffer.model.entity.Grade
import com.jamex.refereestaffer.model.entity.Match
import com.jamex.refereestaffer.model.entity.Referee
import com.jamex.refereestaffer.model.entity.Team
import com.jamex.refereestaffer.model.entity.Vacation
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
import org.springframework.core.env.Environment
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Execution
import spock.lang.Isolated
import spock.lang.Specification

import java.time.LocalDate
import java.time.LocalDateTime

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

/**
 * Regression net for disabling OSIV ({@code spring.jpa.open-in-view: false}, RS-75).
 * The controller slices mock repositories away, so they can never catch a
 * LazyInitializationException — this spec drives full HTTP requests (controller →
 * converter/service → real repositories → H2 → JSON serialisation) with no
 * session-per-request to lean on. If someone reintroduces a lazy traversal outside a
 * transaction, these requests are where it blows up.
 *
 * {@code @Isolated} because it shares the H2 instance with other integration specs
 * (parallel Spock run) and both sides wipe/seed domain tables in setup. {@code @Isolated}
 * only fences off OTHER specs — features of this spec would still run concurrently and
 * wipe each other's seed data, hence {@code SAME_THREAD} on top.
 */
@Isolated
@Execution(ExecutionMode.SAME_THREAD)
@AutoConfigureMockMvc
@SpringBootTest
class OpenInViewDisabledIntegrationSpec extends Specification {

    @Autowired MockMvc mockMvc
    @Autowired Environment environment
    @Autowired TeamRepository teamRepository
    @Autowired RefereeRepository refereeRepository
    @Autowired MatchRepository matchRepository
    @Autowired GradeRepository gradeRepository
    @Autowired VacationRepository vacationRepository

    def jsonSlurper = new JsonSlurper()

    Long matchId
    Long refereeId

    def setup() {
        wipeDomainData()

        def home = teamRepository.save(new Team("Team1", "City1"))
        def away = teamRepository.save(new Team("Team2", "City2"))
        def referee = refereeRepository.save(new Referee("John", "Doe", "john@doe.com", 5))
        short queue = 1
        def match = matchRepository.save(new Match(queue, home, away,
                LocalDateTime.of(2026, 3, 1, 12, 0), referee, (short) 2, (short) 1))
        gradeRepository.save(Grade.builder().value(8.5d).match(match).build())
        vacationRepository.save(new Vacation(null, referee,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 14)))

        matchId = match.id
        refereeId = referee.id
    }

    def "should have OSIV disabled"() {
        expect:
        environment.getProperty("spring.jpa.open-in-view") == "false"
    }

    def "should serve a match with all relations resolved without OSIV"() {
        when:
        def response = mockMvc.perform(get("/api/matches/$matchId"))
                .andReturn().response

        then:
        response.status == 200
        // No with() here: bare property names inside with(parsedJson) resolve against the
        // JSON delegate before spec fields, silently comparing the JSON to itself.
        def match = jsonSlurper.parseText(response.contentAsString)
        match.homeTeamId != null
        match.awayTeamId != null
        match.refereeId == refereeId
        match.gradeId != null
    }

    def "should serve referees enriched with stats without OSIV"() {
        when:
        // enrichWithStats walks match → grade / home / away for every referee
        def response = mockMvc.perform(get("/api/referees"))
                .andReturn().response

        then:
        response.status == 200
        def referees = jsonSlurper.parseText(response.contentAsString)
        referees.size() == 1
        referees[0].averageGrade == 8.5d
        referees[0].potential != null
    }

    def "should serve standings computed from matches without OSIV"() {
        when:
        def response = mockMvc.perform(get("/api/teams/standings"))
                .andReturn().response

        then:
        response.status == 200
        def standings = jsonSlurper.parseText(response.contentAsString)
        standings.size() == 2
        standings[0].name == "Team1" // 2:1 home win → Team1 on top
    }

    def "should serve vacations with their referee resolved without OSIV"() {
        when:
        def response = mockMvc.perform(get("/api/vacations"))
                .andReturn().response

        then:
        response.status == 200
        def vacations = jsonSlurper.parseText(response.contentAsString)
        vacations.size() == 1
        vacations[0].refereeId == refereeId
    }

    def cleanup() {
        // Leave only the data.sql config rows behind for whoever shares the H2 instance.
        wipeDomainData()
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
