package com.jamex.refereestaffer.controller

import com.jamex.refereestaffer.model.dto.MatchDto
import com.jamex.refereestaffer.service.StafferService
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification
import spock.lang.Subject

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

class StafferControllerSpec extends Specification {

    @Subject
    StafferController stafferController

    StafferService stafferService = Mock()

    def setup() {
        stafferController = new StafferController(stafferService)
    }

    def "should staff referees to matches in provided queue"() {
        given:
        short queue = 12
        def matchesInQueue = [MatchDto.builder().build(), MatchDto.builder().build()]

        when:
        def result = stafferController.staffReferees(queue)

        then:
        1 * stafferService.staffReferees(queue) >> matchesInQueue
        result == matchesInQueue
    }

    // Staffing persists referee assignments, so the endpoint must not be reachable via a
    // safe method — a GET-triggering crawler or browser prefetch would mutate the DB.
    def "should expose staffing as POST"() {
        given:
        def mockMvc = MockMvcBuilders.standaloneSetup(stafferController).build()

        when:
        def response = mockMvc.perform(post("/api/staffer/7")).andReturn().response

        then:
        1 * stafferService.staffReferees(7 as short) >> []
        response.status == 200
    }

    def "should reject GET with method not allowed"() {
        given:
        def mockMvc = MockMvcBuilders.standaloneSetup(stafferController).build()

        when:
        def response = mockMvc.perform(get("/api/staffer/7")).andReturn().response

        then:
        0 * stafferService.staffReferees(_)
        response.status == 405
    }
}
