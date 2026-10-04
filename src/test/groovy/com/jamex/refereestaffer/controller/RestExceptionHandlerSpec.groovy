package com.jamex.refereestaffer.controller

import com.jamex.refereestaffer.model.converter.TeamConverter
import com.jamex.refereestaffer.model.entity.Config
import com.jamex.refereestaffer.model.entity.Team
import com.jamex.refereestaffer.repository.TeamRepository
import com.jamex.refereestaffer.service.TeamService
import groovy.json.JsonSlurper
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import org.hibernate.PropertyValueException
import org.hibernate.exception.ConstraintViolationException.ConstraintKind
import org.spockframework.runtime.model.parallel.ExecutionMode
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Execution
import spock.lang.Specification

import java.sql.SQLException

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

/**
 * Covers the constraint-violation net in {@link RestExceptionHandler}. TeamController is only a
 * vehicle: it is the thinnest controller that writes straight through a repository, so stubbing
 * the repository to throw reproduces a violation surfacing at flush time without needing a
 * database. What matters is the status and the {@code detail} the advice produces.
 */
// Features must run on one thread: the @SpringBean mocks live in the shared Spring
// context, so concurrent features would attach/stub the same mock instances at once.
@Execution(ExecutionMode.SAME_THREAD)
@WebMvcTest(TeamController)
class RestExceptionHandlerSpec extends Specification {

    // Every identifier a translated Hibernate message carries: Spring builds the
    // DataIntegrityViolationException message out of the failing SQL plus the constraint name.
    static final String LEAKY_SQL = "insert into match (away_id,date,home_id,queue) values (?,?,?,?)"
    static final String LEAKY_CONSTRAINT = "FK_MATCH_HOME_ID_TEAM_ID_INDEX_7"
    static final String LEAKY_MESSAGE =
            "could not execute statement [Referential integrity constraint violation: " +
            "\"$LEAKY_CONSTRAINT: PUBLIC.MATCH FOREIGN KEY(HOME_ID) REFERENCES PUBLIC.TEAM(ID)\"] [$LEAKY_SQL]"

    @Autowired
    MockMvc mockMvc

    @SpringBean
    TeamService teamService = Mock()

    @SpringBean
    TeamRepository teamRepository = Mock()

    @SpringBean
    TeamConverter teamConverter = Mock()

    def "should respond #status with '#expectedDetail' for a #kind constraint violation"() {
        when:
        def response = mockMvc.perform(delete("/api/teams/65")).andReturn().response

        then:
        1 * teamRepository.deleteById(65l) >> { throw translated(hibernateViolation(kind)) }
        response.status == status
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.detail == expectedDetail
        json.status == status

        and: "no database identifier reaches the client"
        !response.contentAsString.contains(LEAKY_CONSTRAINT)
        !response.contentAsString.contains("PUBLIC.MATCH")
        !response.contentAsString.contains("insert into")

        where:
        kind                       || status | expectedDetail
        ConstraintKind.FOREIGN_KEY || 409    | RestExceptionHandler.REFERENCED_RECORD
        ConstraintKind.UNIQUE      || 409    | RestExceptionHandler.DUPLICATE_RECORD
        ConstraintKind.CHECK       || 409    | RestExceptionHandler.DATA_INTEGRITY_CONFLICT
        ConstraintKind.OTHER       || 409    | RestExceptionHandler.DATA_INTEGRITY_CONFLICT
        ConstraintKind.NOT_NULL    || 400    | RestExceptionHandler.MISSING_REQUIRED_FIELD
    }

    def "should respond 400 when Hibernate rejects a null property before it reaches SQL"() {
        given: "the exception Hibernate raises for a nullable = false column left null"
        def propertyValue = new PropertyValueException(
                "not-null property references a null or transient value", "Match", "date")

        when:
        def response = mockMvc.perform(post("/api/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"name": "Legia", "city": "Warszawa"}'))
                .andReturn().response

        then:
        1 * teamConverter.convertFromDto(_) >> ([] as Team)
        1 * teamRepository.save(_) >> { throw translated(propertyValue) }
        response.status == 400
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.detail == RestExceptionHandler.MISSING_REQUIRED_FIELD

        and: "the entity and property names behind the violation stay in the log"
        !response.contentAsString.contains("Match.date")
    }

    def "should respond 409 with a generic detail when the violated constraint cannot be identified"() {
        when:
        def response = mockMvc.perform(delete("/api/teams/65")).andReturn().response

        then:
        1 * teamRepository.deleteById(65l) >> { throw new DataIntegrityViolationException(LEAKY_MESSAGE) }
        response.status == 409
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.detail == RestExceptionHandler.DATA_INTEGRITY_CONFLICT
        !response.contentAsString.contains(LEAKY_CONSTRAINT)
    }

    def "should respond 409 with the duplicate detail for a DuplicateKeyException without a named constraint"() {
        when:
        def response = mockMvc.perform(delete("/api/teams/65")).andReturn().response

        then:
        1 * teamRepository.deleteById(65l) >> { throw new DuplicateKeyException(LEAKY_MESSAGE) }
        response.status == 409
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.detail == RestExceptionHandler.DUPLICATE_RECORD
    }

    def "should respond 400 listing the offending fields when entity bean validation fails on flush"() {
        given: "the violations hibernate-validator reports for an entity with both NOT NULL fields unset"
        def validator = Validation.buildDefaultValidatorFactory().validator
        def violations = validator.validate(new Config(null, null))

        expect: "the entity really is invalid, so the spec is not asserting on an empty set"
        violations.size() == 2

        when:
        def response = mockMvc.perform(delete("/api/teams/65")).andReturn().response

        then:
        1 * teamRepository.deleteById(65l) >> { throw new ConstraintViolationException(violations) }
        response.status == 400
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.detail == "name: must not be null; value: must not be null"
    }

    def "should respond 400 when entity validation carries no violations to describe"() {
        when:
        def response = mockMvc.perform(delete("/api/teams/65")).andReturn().response

        then:
        1 * teamRepository.deleteById(65l) >> { throw new ConstraintViolationException("validation failed", [] as Set) }
        response.status == 400
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.detail == RestExceptionHandler.MISSING_REQUIRED_FIELD
    }

    private static org.hibernate.exception.ConstraintViolationException hibernateViolation(ConstraintKind kind) {
        new org.hibernate.exception.ConstraintViolationException(
                LEAKY_MESSAGE, new SQLException("integrity constraint violation", "23503"),
                LEAKY_SQL, kind, LEAKY_CONSTRAINT)
    }

    /** Mirrors what Spring's HibernateExceptionTranslator does before the advice sees the failure. */
    private static DataIntegrityViolationException translated(RuntimeException hibernateException) {
        new DataIntegrityViolationException(hibernateException.message, hibernateException)
    }
}
