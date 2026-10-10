package com.jamex.refereestaffer.mcp

import com.jamex.refereestaffer.mcp.view.AlgorithmParameter
import com.jamex.refereestaffer.mcp.view.MatchPage
import com.jamex.refereestaffer.mcp.view.RefereeProfile
import com.jamex.refereestaffer.mcp.view.RefereeSummary
import com.jamex.refereestaffer.model.dto.DifficultyBreakdownDto
import com.jamex.refereestaffer.model.dto.StandingsDto
import org.springframework.ai.mcp.annotation.McpResource
import org.springframework.ai.mcp.annotation.McpTool
import spock.lang.Specification
import spock.lang.Subject

/**
 * The {@code @McpTool} layer is delegation only — {@link StaffingInsightsSpec} covers the
 * behaviour. What is worth asserting here is that every exposed method really does nothing
 * but forward (no logic creeping into the annotated class), and that the protocol-facing
 * metadata the model relies on is actually attached: tool names, descriptions from the
 * shared constants, and the read-only annotations that let a client call these without
 * prompting the user.
 */
class StaffingMcpToolsSpec extends Specification {

    @Subject
    StaffingMcpTools tools

    StaffingInsights insights = Mock()

    def setup() {
        tools = new StaffingMcpTools(insights)
    }

    def "should delegate every tool call to the insights bean and return its result unchanged"() {
        given:
        def referees = [new RefereeSummary(1L, "John Smith", "x@y.z", 5, 8.4d, 420.0d, (short) 7, (short) 3, false)]
        def profile = new RefereeProfile(referees[0], (short) 2, (short) 1, [], [])
        def matchPage = new MatchPage((short) 1, 0, 25, 3L, 1, [])
        def breakdown = new DifficultyBreakdownDto(10L, 104.0d,
                new DifficultyBreakdownDto.Parts(97.0d, 0.0d, 7.0d, 0.0d),
                new DifficultyBreakdownDto.Flags(false, true, false, 3))
        def standings = new StandingsDto((short) 3, [])
        def config = [new AlgorithmParameter("NUMBER_OF_EDGE_TEAMS", 3.0d, "difficulty", "...")]

        when:
        def results = [
                tools.listReferees(),
                tools.getRefereeProfile(1L),
                tools.listMatches((short) 1, 0, 25),
                tools.explainMatchDifficulty(10L),
                tools.getStandings(),
                tools.getAlgorithmConfig(),
                tools.algorithmResource(),
                tools.configResource(),
        ]

        then:
        1 * insights.listReferees() >> referees
        1 * insights.getRefereeProfile(1L) >> profile
        1 * insights.listMatches((short) 1, 0, 25) >> matchPage
        1 * insights.explainMatchDifficulty(10L) >> breakdown
        1 * insights.getStandings() >> standings
        1 * insights.getAlgorithmConfig() >> config
        1 * insights.describeAlgorithm() >> "algorithm text"
        1 * insights.describeConfiguration() >> "config text"
        0 * _

        and: "identity, not equality — anything else would mean the layer reshapes the result"
        results[0].is(referees)
        results[1].is(profile)
        results[2].is(matchPage)
        results[3].is(breakdown)
        results[4].is(standings)
        results[5].is(config)
        results[6] == "algorithm text"
        results[7] == "config text"
    }

    def "should pass optional match-listing arguments through as nulls"() {
        when:
        tools.listMatches(null, null, null)

        then: "clamping defaults belong to the logic bean, not to the protocol layer"
        1 * insights.listMatches(null, null, null)
    }

    def "should annotate #method as an MCP tool named #toolName, read-only and non-destructive"() {
        given:
        def annotation = StaffingMcpTools.getDeclaredMethod(method, parameterTypes as Class[])
                .getAnnotation(McpTool)

        expect:
        annotation != null
        annotation.name() == toolName
        annotation.description() == description
        // Stage 1 is read-only; these hints are what tells a client it may call the tool
        // without asking the user first.
        annotation.annotations().readOnlyHint()
        !annotation.annotations().destructiveHint()
        annotation.annotations().idempotentHint()
        !annotation.annotations().openWorldHint()

        where:
        method                     | parameterTypes                 || toolName                                              | description
        "listReferees"             | []                             || StaffingToolDescriptions.LIST_REFEREES                | StaffingToolDescriptions.LIST_REFEREES_DESCRIPTION
        "getRefereeProfile"        | [long]                         || StaffingToolDescriptions.GET_REFEREE_PROFILE          | StaffingToolDescriptions.GET_REFEREE_PROFILE_DESCRIPTION
        "listMatches"              | [Short, Integer, Integer]      || StaffingToolDescriptions.LIST_MATCHES                 | StaffingToolDescriptions.LIST_MATCHES_DESCRIPTION
        "explainMatchDifficulty"   | [long]                         || StaffingToolDescriptions.EXPLAIN_MATCH_DIFFICULTY     | StaffingToolDescriptions.EXPLAIN_MATCH_DIFFICULTY_DESCRIPTION
        "getStandings"             | []                             || StaffingToolDescriptions.GET_STANDINGS                | StaffingToolDescriptions.GET_STANDINGS_DESCRIPTION
        "getAlgorithmConfig"       | []                             || StaffingToolDescriptions.GET_ALGORITHM_CONFIG         | StaffingToolDescriptions.GET_ALGORITHM_CONFIG_DESCRIPTION
    }

    def "should annotate #method as the #uri MCP resource"() {
        given:
        def annotation = StaffingMcpTools.getDeclaredMethod(method).getAnnotation(McpResource)

        expect:
        annotation != null
        annotation.uri() == uri
        annotation.mimeType() == "text/plain"
        !annotation.description().isBlank()

        where:
        method              || uri
        "algorithmResource" || StaffingToolDescriptions.ALGORITHM_RESOURCE_URI
        "configResource"    || StaffingToolDescriptions.CONFIG_RESOURCE_URI
    }
}
