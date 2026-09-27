package com.jamex.refereestaffer.controller

import com.jamex.refereestaffer.model.dto.CandidateViolationsDto
import com.jamex.refereestaffer.model.dto.MatchDto
import com.jamex.refereestaffer.model.exception.StafferException
import com.jamex.refereestaffer.model.staffing.StaffingRule
import com.jamex.refereestaffer.model.staffing.StaffingViolation
import com.jamex.refereestaffer.model.request.StaffingLockRequest
import com.jamex.refereestaffer.service.StafferService
import groovy.json.JsonSlurper
import org.spockframework.runtime.model.parallel.ExecutionMode
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Execution
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

// Features must run on one thread: the @SpringBean mocks live in the shared Spring
// context, so concurrent features would attach/stub the same mock instances at once.
@Execution(ExecutionMode.SAME_THREAD)
@WebMvcTest(StafferController)
class StafferControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @SpringBean
    StafferService stafferService = Mock()

    def "should staff referees to matches in provided queue"() {
        given:
        def matchesInQueue = [MatchDto.builder().id(1l).refereeId(4l).build(),
                              MatchDto.builder().id(2l).refereeId(7l).build()]

        when:
        def response = mockMvc.perform(post("/api/staffer/12")).andReturn().response

        then:
        // No body means no locks — the controller must default to an empty list.
        1 * stafferService.staffReferees(12 as short, []) >> matchesInQueue
        response.status == 200
        def json = new JsonSlurper().parseText(response.contentAsString)
        json*.id == [1, 2]
        json*.refereeId == [4, 7]
    }

    def "should pass locked assignments from the request body to the service"() {
        given:
        def body = '[{"matchId":1,"refereeId":4},{"matchId":2,"refereeId":7}]'
        def expectedLocks = [new StaffingLockRequest(1l, 4l), new StaffingLockRequest(2l, 7l)]

        when:
        def response = mockMvc.perform(post("/api/staffer/12")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andReturn().response

        then:
        1 * stafferService.staffReferees(12 as short, expectedLocks) >> []
        response.status == 200
    }

    // Staffing persists referee assignments, so the endpoint must not be reachable via a
    // safe method — a GET-triggering crawler or browser prefetch would mutate the DB.
    def "should reject GET with method not allowed"() {
        when:
        def response = mockMvc.perform(get("/api/staffer/7")).andReturn().response

        then:
        0 * stafferService.staffReferees(_, _)
        response.status == 405
    }

    def "should serve the staffing rule violations for a queue"() {
        given:
        def violations = [new CandidateViolationsDto(11l, 1l,
                [new StaffingViolation(StaffingRule.SAME_DAY_MATCH, "Anna Nowak already has a match on 2026-05-04 at 11:00 (queue 9)"),
                 new StaffingViolation(StaffingRule.VACATION, "Anna Nowak is on vacation from 2026-05-04 to 2026-05-06")])]

        when:
        def response = mockMvc.perform(get("/api/staffer/12/violations")).andReturn().response

        then:
        1 * stafferService.findCandidateViolationsForQueue(12 as short) >> violations
        response.status == 200
        def json = new JsonSlurper().parseText(response.contentAsString)
        json*.matchId == [11]
        json*.refereeId == [1]
        // The rule code is the wire contract the frontend switches on for its chip label.
        json[0].violations*.rule == ["SAME_DAY_MATCH", "VACATION"]
        json[0].violations[0].message == "Anna Nowak already has a match on 2026-05-04 at 11:00 (queue 9)"
    }

    def "should serve an empty violations list for a clean queue"() {
        when:
        def response = mockMvc.perform(get("/api/staffer/3/violations")).andReturn().response

        then:
        1 * stafferService.findCandidateViolationsForQueue(3 as short) >> []
        response.status == 200
        response.contentAsString == "[]"
    }

    def "should respond 409 with problem detail when there are not enough referees"() {
        when:
        def response = mockMvc.perform(post("/api/staffer/7")).andReturn().response

        then:
        1 * stafferService.staffReferees(7 as short, []) >> { throw new StafferException() }
        response.status == 409
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.detail == new StafferException().message
    }
}
