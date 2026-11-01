package com.jamex.refereestaffer.integration

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification
import spock.lang.Unroll

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete

/**
 * RS-112: the five collection-level {@code DELETE /api/{entity}} endpoints were removed rather
 * than fixed. Each called {@code repository.deleteAll()} on its own table, ignoring the FK graph,
 * and four of them returned 500; nothing in the app called them since the "Clear data" button
 * went with the old header. This pins the removal against the real dispatcher — a controller
 * slice could not tell a missing mapping from a mocked one.
 */
@AutoConfigureMockMvc
@SpringBootTest
class RemovedBulkDeleteIntegrationSpec extends Specification {

    @Autowired MockMvc mockMvc

    @Unroll
    def "should not expose a collection-level delete on /api/#entity"() {
        when:
        def response = mockMvc.perform(delete("/api/" + entity)).andReturn().response

        then: "405, not 404 — the path still carries GET/POST/PUT mappings, only the bulk delete is gone"
        response.status == 405

        where:
        entity << ["grades", "matches", "referees", "teams", "vacations"]
    }
}
