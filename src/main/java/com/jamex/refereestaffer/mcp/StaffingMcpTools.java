package com.jamex.refereestaffer.mcp;

import com.jamex.refereestaffer.mcp.view.AlgorithmParameter;
import com.jamex.refereestaffer.mcp.view.MatchPage;
import com.jamex.refereestaffer.mcp.view.RefereeProfile;
import com.jamex.refereestaffer.mcp.view.RefereeSummary;
import com.jamex.refereestaffer.model.dto.DifficultyBreakdownDto;
import com.jamex.refereestaffer.model.dto.StandingsDto;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The MCP surface of the application: six read-only tools and two resources, each one a
 * one-line delegation to {@link StaffingInsights}. No logic lives here on purpose — see
 * that class for why the split exists.
 *
 * <p>Gated behind the {@code mcp} profile together with
 * {@code spring.ai.mcp.server.enabled}. Spring AI's HTTP transport ships no
 * authentication of any kind, so the endpoint must not exist in a default run; adding it
 * is RS-125. {@link StaffingInsights} itself is unconditional — it is just a read-only
 * service.
 *
 * <p>Every tool is marked read-only and idempotent in its MCP annotations, and that is
 * load-bearing for stage 1: a client may call any of them without asking the user.
 */
@Service
@Profile("mcp")
public class StaffingMcpTools {

    private final StaffingInsights insights;

    public StaffingMcpTools(StaffingInsights insights) {
        this.insights = insights;
    }

    @McpTool(name = StaffingToolDescriptions.LIST_REFEREES,
            description = StaffingToolDescriptions.LIST_REFEREES_DESCRIPTION,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public List<RefereeSummary> listReferees() {
        return insights.listReferees();
    }

    @McpTool(name = StaffingToolDescriptions.GET_REFEREE_PROFILE,
            description = StaffingToolDescriptions.GET_REFEREE_PROFILE_DESCRIPTION,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public RefereeProfile getRefereeProfile(
            @McpToolParam(description = "Referee id, as returned by list_referees.", required = true)
            long refereeId) {
        return insights.getRefereeProfile(refereeId);
    }

    @McpTool(name = StaffingToolDescriptions.LIST_MATCHES,
            description = StaffingToolDescriptions.LIST_MATCHES_DESCRIPTION,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public MatchPage listMatches(
            @McpToolParam(description = "Queue (round) number to filter by. Omit for all queues.",
                    required = false) Short queue,
            @McpToolParam(description = "Zero-based page index. Defaults to 0.", required = false)
            Integer page,
            @McpToolParam(description = "Matches per page, 1-100. Defaults to 25.", required = false)
            Integer pageSize) {
        return insights.listMatches(queue, page, pageSize);
    }

    @McpTool(name = StaffingToolDescriptions.EXPLAIN_MATCH_DIFFICULTY,
            description = StaffingToolDescriptions.EXPLAIN_MATCH_DIFFICULTY_DESCRIPTION,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public DifficultyBreakdownDto explainMatchDifficulty(
            @McpToolParam(description = "Match id, as returned by list_matches.", required = true)
            long matchId) {
        return insights.explainMatchDifficulty(matchId);
    }

    @McpTool(name = StaffingToolDescriptions.GET_STANDINGS,
            description = StaffingToolDescriptions.GET_STANDINGS_DESCRIPTION,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public StandingsDto getStandings() {
        return insights.getStandings();
    }

    @McpTool(name = StaffingToolDescriptions.GET_ALGORITHM_CONFIG,
            description = StaffingToolDescriptions.GET_ALGORITHM_CONFIG_DESCRIPTION,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public List<AlgorithmParameter> getAlgorithmConfig() {
        return insights.getAlgorithmConfig();
    }

    @McpResource(uri = StaffingToolDescriptions.ALGORITHM_RESOURCE_URI,
            name = "algorithm",
            title = "Staffing algorithm explained",
            description = StaffingToolDescriptions.ALGORITHM_RESOURCE_DESCRIPTION,
            mimeType = "text/plain")
    public String algorithmResource() {
        return insights.describeAlgorithm();
    }

    @McpResource(uri = StaffingToolDescriptions.CONFIG_RESOURCE_URI,
            name = "config",
            title = "Current staffing weights",
            description = StaffingToolDescriptions.CONFIG_RESOURCE_DESCRIPTION,
            mimeType = "text/plain")
    public String configResource() {
        return insights.describeConfiguration();
    }
}
