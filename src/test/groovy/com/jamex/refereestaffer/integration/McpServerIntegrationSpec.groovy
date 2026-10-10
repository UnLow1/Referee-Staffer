package com.jamex.refereestaffer.integration

import com.jamex.refereestaffer.mcp.StaffingToolDescriptions
import groovy.json.JsonSlurper
import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport
import io.modelcontextprotocol.spec.McpSchema
import org.spockframework.runtime.model.parallel.ExecutionMode
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import spock.lang.AutoCleanup
import spock.lang.Execution
import spock.lang.Isolated
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration

/**
 * End-to-end check of the MCP server (RS-121): a real MCP client from the Java SDK speaks
 * Streamable HTTP to the running application on {@code POST /mcp}, lists what the server
 * advertises and calls a tool. This is the "test application" the ticket asks for — a
 * separate sample project would only duplicate the wiring that matters here.
 *
 * <p>Runs under the {@code mcp} profile, which is the only way the endpoint exists at all;
 * {@link McpDisabledByDefaultIntegrationSpec} covers the other half of that contract.
 *
 * <p>{@code @Isolated} + {@code SAME_THREAD} for the same reason as the other integration
 * specs: the in-memory H2 is shared JVM-wide and the others wipe domain tables in setup.
 * This spec therefore asserts only against the configuration rows seeded by
 * {@code data.sql}, which nothing wipes — the domain-level shaping is covered by
 * {@code StaffingInsightsSpec}.
 */
@Isolated
@Execution(ExecutionMode.SAME_THREAD)
@ActiveProfiles("mcp")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpServerIntegrationSpec extends Specification {

    @LocalServerPort
    int port

    @Shared
    @AutoCleanup("close")
    def client

    def jsonSlurper = new JsonSlurper()

    def setup() {
        if (client == null) {
            def transport = HttpClientStreamableHttpTransport.builder("http://localhost:$port")
                    .endpoint("/mcp")
                    .build()
            client = McpClient.sync(transport)
                    .requestTimeout(Duration.ofSeconds(20))
                    .clientInfo(new McpSchema.Implementation("referee-staffer-spec", "0.0.1"))
                    .build()
            client.initialize()
        }
    }

    def "should expose the server over streamable HTTP with its instructions"() {
        when:
        def result = client.initialize()

        then:
        result.serverInfo().name() == "referee-staffer"
        result.capabilities().tools() != null
        result.capabilities().resources() != null

        and: "instructions are a model's only orientation before it lists anything"
        result.instructions().contains("Read-only access to the Referee Staffer database")
    }

    def "should advertise exactly the six read-only tools"() {
        when:
        def tools = client.listTools().tools()

        then:
        tools*.name().toSorted() == [
                StaffingToolDescriptions.EXPLAIN_MATCH_DIFFICULTY,
                StaffingToolDescriptions.GET_ALGORITHM_CONFIG,
                StaffingToolDescriptions.GET_REFEREE_PROFILE,
                StaffingToolDescriptions.GET_STANDINGS,
                StaffingToolDescriptions.LIST_MATCHES,
                StaffingToolDescriptions.LIST_REFEREES,
        ].toSorted()

        and: "stage 1 writes nothing, and every tool says so on the wire"
        tools.every { it.annotations().readOnlyHint() }
        tools.every { !it.annotations().destructiveHint() }

        and: "the match listing's optional paging arguments reach the client's schema"
        // Tool.inputSchema() comes back as the raw JSON-Schema map, not a typed object, and
        // "properties" has to be read with an explicit get(): both `schema.properties` and
        // `schema["properties"]` resolve against Groovy's own `properties` meta-property
        // (the map's bean properties) and silently hand back an empty map instead.
        Map schema = tools.find { it.name() == StaffingToolDescriptions.LIST_MATCHES }.inputSchema()
        schema.get("properties").keySet().toSorted() == ["page", "pageSize", "queue"]
        schema.get("required").isEmpty()
    }

    def "should advertise both resources"() {
        when:
        def resources = client.listResources().resources()

        then:
        resources*.uri().toSorted() == [
                StaffingToolDescriptions.ALGORITHM_RESOURCE_URI,
                StaffingToolDescriptions.CONFIG_RESOURCE_URI,
        ].toSorted()
    }

    def "should answer a tool call with the seeded algorithm configuration"() {
        when:
        def result = client.callTool(new McpSchema.CallToolRequest(
                StaffingToolDescriptions.GET_ALGORITHM_CONFIG, [:]))

        then:
        !result.isError()

        and:
        def parameters = jsonSlurper.parseText(result.content().first().text())
        // data.sql seeds one row per ConfigName.
        parameters*.name.contains("NUMBER_OF_EDGE_TEAMS")
        parameters.every { it.group in ["potential", "difficulty", "effective"] }
        parameters.every { it.description != null && !it.description.isBlank() }
    }

    def "should read the algorithm resource as plain text"() {
        when:
        def result = client.readResource(
                new McpSchema.ReadResourceRequest(StaffingToolDescriptions.ALGORITHM_RESOURCE_URI))

        then:
        result.contents().size() == 1
        result.contents().first().text().contains("Referee potential")
    }

    def "should report an unknown referee id as a tool error"() {
        when:
        def result = client.callTool(new McpSchema.CallToolRequest(
                StaffingToolDescriptions.GET_REFEREE_PROFILE, [refereeId: 999_999L]))

        then: "the model is told the id is wrong instead of being handed an empty profile"
        result.isError()
        result.content().first().text().contains("999999")
    }
}
