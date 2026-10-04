package com.jamex.refereestaffer.integration

import com.jamex.refereestaffer.controller.RestExceptionHandler
import com.jamex.refereestaffer.model.entity.Config
import com.jamex.refereestaffer.model.entity.Match
import com.jamex.refereestaffer.model.entity.Referee
import com.jamex.refereestaffer.model.entity.Team
import com.jamex.refereestaffer.repository.ConfigurationRepository
import com.jamex.refereestaffer.repository.GradeRepository
import com.jamex.refereestaffer.repository.MatchRepository
import com.jamex.refereestaffer.repository.RefereeRepository
import com.jamex.refereestaffer.repository.TeamRepository
import com.jamex.refereestaffer.repository.VacationRepository
import groovy.json.JsonSlurper
import jakarta.validation.ConstraintViolationException
import org.spockframework.runtime.model.parallel.ExecutionMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.support.TransactionTemplate
import spock.lang.Execution
import spock.lang.Isolated
import spock.lang.Specification

import java.time.LocalDateTime

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete

/**
 * Pins the contract {@link RestExceptionHandler} classifies on (RS-120). The controller-slice
 * spec hand-builds the exceptions, so it can only prove the mapping; this one provokes real
 * violations against H2 and asserts what actually comes back out of Hibernate and Spring's
 * exception translation. If a future Spring/Hibernate bump stops wrapping violations as
 * {@link DataIntegrityViolationException}, or drops the {@code ConstraintKind}, the classifier
 * would silently fall back to a generic 409 — these features are what catch that.
 *
 * {@code @Isolated} + {@code SAME_THREAD} for the same reason as the other integration specs:
 * the H2 instance is shared and setup/cleanup wipe the domain tables.
 */
@Isolated
@Execution(ExecutionMode.SAME_THREAD)
@AutoConfigureMockMvc
@SpringBootTest
class ConstraintViolationIntegrationSpec extends Specification {

    @Autowired MockMvc mockMvc
    @Autowired TeamRepository teamRepository
    @Autowired RefereeRepository refereeRepository
    @Autowired MatchRepository matchRepository
    @Autowired GradeRepository gradeRepository
    @Autowired VacationRepository vacationRepository
    @Autowired ConfigurationRepository configurationRepository
    @Autowired TransactionTemplate transactionTemplate

    def jsonSlurper = new JsonSlurper()

    // The advice registered in the context, so these features classify through the same bean
    // the DispatcherServlet uses. They call it directly rather than over MockMvc because no HTTP
    // endpoint can currently produce these two shapes: TeamDto.name is @NotBlank, and Config —
    // the only entity carrying bean-validation mirrors — is only written through the validated
    // PUT /api/configuration. The FK feature above is the one that does go over HTTP.
    @Autowired RestExceptionHandler handler

    Long referencedTeamId

    def setup() {
        wipeDomainData()

        def home = teamRepository.save(new Team("Team1", "City1"))
        def away = teamRepository.save(new Team("Team2", "City2"))
        def referee = refereeRepository.save(new Referee("John", "Doe", "john@doe.com", 5))
        matchRepository.save(new Match((short) 1, home, away,
                LocalDateTime.of(2026, 3, 1, 12, 0), referee, (short) 2, (short) 1))

        referencedTeamId = home.id
    }

    def "should respond 409 instead of 500 when deleting a team a match still points at"() {
        when: "the team is referenced by match.home_id, so H2 rejects the delete"
        def response = mockMvc.perform(delete("/api/teams/$referencedTeamId")).andReturn().response

        then:
        response.status == 409
        def problem = jsonSlurper.parseText(response.contentAsString)
        problem.detail == RestExceptionHandler.RELATED_RECORD_CONFLICT

        and: "the H2 message naming the schema, constraint and referenced columns stays out of the response"
        !response.contentAsString.toUpperCase().contains("PUBLIC.")
        !response.contentAsString.toUpperCase().contains("FOREIGN KEY")
        !response.contentAsString.contains("constraint")

        and: "the team is still there — the 409 reports a refusal, not a partial delete"
        teamRepository.findById(referencedTeamId).present
    }

    def "should classify a real missing NOT NULL value as 400"() {
        when: "Team.name is nullable = false and carries no bean-validation mirror, so nothing stops it earlier"
        teamRepository.saveAndFlush(new Team(null, "City3"))

        then:
        def ex = thrown(DataIntegrityViolationException)

        when: "the violation H2/Hibernate actually produced is fed to the real advice"
        // Asserting through the handler rather than on the cause chain: whether Hibernate
        // pre-checks nullability (PropertyValueException) or lets H2 reject the INSERT
        // (ConstraintViolationException with ConstraintKind.NOT_NULL) depends on
        // hibernate.check_nullability, which bean validation on the classpath flips.
        // Both shapes must come out as 400, and that is what the feature pins.
        def problem = handler.handleDataIntegrityViolation(ex)

        then:
        problem.status == 400
        problem.detail == RestExceptionHandler.MISSING_REQUIRED_FIELD
    }

    def "should classify a real entity bean-validation failure as 400 naming the fields"() {
        when: "Config mirrors its NOT NULL columns, so hibernate-validator rejects it before SQL"
        configurationRepository.saveAndFlush(new Config(null, null))

        then: "a jakarta ConstraintViolationException, which Spring does not translate to a DataAccessException"
        def ex = thrown(ConstraintViolationException)

        when:
        def problem = handler.handleEntityConstraintViolation(ex)

        then:
        problem.status == 400
        problem.detail == "name: must not be null; value: must not be null"
    }

    def "should still classify a NOT NULL violation that only surfaces at transaction commit"() {
        when: "save() alone, so the flush happens when the transaction commits, not inside the call"
        transactionTemplate.executeWithoutResult { teamRepository.save(new Team(null, "City4")) }

        then: "the commit path translates it the same way an in-request flush does"
        def ex = thrown(DataIntegrityViolationException)
        handler.handleDataIntegrityViolation(ex).status == 400
    }

    def "should still classify entity bean validation that only surfaces at transaction commit"() {
        when:
        transactionTemplate.executeWithoutResult { configurationRepository.save(new Config(null, null)) }

        then: "it arrives untranslated as itself — JpaTransactionManager has nothing to map it to,"
        // so it reaches the advice as a plain jakarta ConstraintViolationException rather than a
        // TransactionSystemException. That is what makes handleEntityConstraintViolation enough to
        // cover the @Transactional write paths (GradeService, StafferService.staffReferees) and not
        // just the ones that flush inside the request.
        def ex = thrown(ConstraintViolationException)
        def problem = handler.handleEntityConstraintViolation(ex)
        problem.status == 400
        problem.detail == "name: must not be null; value: must not be null"
    }

    def cleanup() {
        wipeDomainData()
    }

    private void wipeDomainData() {
        // Same FK order as the other integration specs: grade → match, match → team/referee,
        // vacation → referee. Config rows from data.sql are left alone.
        gradeRepository.deleteAllInBatch()
        vacationRepository.deleteAllInBatch()
        matchRepository.deleteAllInBatch()
        refereeRepository.deleteAllInBatch()
        teamRepository.deleteAllInBatch()
    }
}
