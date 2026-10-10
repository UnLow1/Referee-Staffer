package com.jamex.refereestaffer.integration

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.ApplicationContext
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

/**
 * The other half of the RS-121 contract: without the {@code mcp} profile there must be no
 * MCP endpoint at all. Spring AI's HTTP transport carries no authentication, so an
 * accidentally-enabled {@code /mcp} would hand the whole database to anything that can
 * reach the port — authorizing it is RS-125, and until then "off by default" is the only
 * protection there is.
 *
 * <p>No {@code @Isolated} here: this spec neither seeds nor wipes anything, it only asks
 * the context what it did not wire up.
 */
@AutoConfigureMockMvc
@SpringBootTest
class McpDisabledByDefaultIntegrationSpec extends Specification {

    @Autowired MockMvc mockMvc
    @Autowired ApplicationContext applicationContext

    def "should not map the MCP endpoint without the mcp profile"() {
        expect:
        !applicationContext.environment.activeProfiles.contains("mcp")

        when: "a client tries the endpoint anyway"
        def response = mockMvc.perform(post("/mcp")
                .contentType("application/json")
                .content('{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}'))
                .andReturn().response

        then:
        response.status == 404
    }

    def "should not create an MCP server bean without the mcp profile"() {
        expect: "the whole MCP layer is absent, tool beans included"
        applicationContext.getBeanNamesForType(io.modelcontextprotocol.server.McpSyncServer).length == 0
        !applicationContext.containsBean("staffingMcpTools")

        and: "the transport-agnostic logic bean, on the other hand, is always available"
        applicationContext.containsBean("staffingInsights")
    }
}
