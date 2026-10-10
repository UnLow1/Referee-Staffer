package com.jamex.refereestaffer.controller

import com.jamex.refereestaffer.model.dto.ClearDataSummaryDto
import com.jamex.refereestaffer.service.DataService
import groovy.json.JsonSlurper
import org.spockframework.runtime.model.parallel.ExecutionMode
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Execution
import spock.lang.Specification
import spock.lang.Unroll

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete

// Features must run on one thread: the @SpringBean mocks live in the shared Spring
// context, so concurrent features would attach/stub the same mock instances at once.
@Execution(ExecutionMode.SAME_THREAD)
@WebMvcTest(DataController)
class DataControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @SpringBean
    DataService dataService = Mock()

    def "should clear all data and return the per-table summary"() {
        when:
        def response = mockMvc.perform(delete("/api/data").param("confirm", "delete-all-data"))
                .andReturn().response

        then:
        1 * dataService.clearAllData() >> new ClearDataSummaryDto(4l, 3l, 7l, 5l, 18l)
        response.status == 200
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.grades == 4
        json.vacations == 3
        json.matches == 7
        json.referees == 5
        json.teams == 18
    }

    @Unroll
    def "should reject the wipe with 400 when confirm is #description"() {
        when:
        def response = mockMvc.perform(request).andReturn().response

        then:
        // The guard has to run before the service is touched — a 400 that still wiped
        // the database would be worse than no guard at all.
        0 * dataService._
        response.status == 400
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.detail == "confirm: must be 'delete-all-data' to wipe all data"

        where:
        description    | request
        "missing"      | delete("/api/data")
        "empty"        | delete("/api/data").param("confirm", "")
        "wrong token"  | delete("/api/data").param("confirm", "yes")
        "wrong case"   | delete("/api/data").param("confirm", "DELETE-ALL-DATA")
    }
}
