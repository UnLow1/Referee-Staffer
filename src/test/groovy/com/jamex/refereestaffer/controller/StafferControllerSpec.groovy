package com.jamex.refereestaffer.controller

import com.jamex.refereestaffer.model.dto.MatchDto
import com.jamex.refereestaffer.model.exception.StafferException
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
