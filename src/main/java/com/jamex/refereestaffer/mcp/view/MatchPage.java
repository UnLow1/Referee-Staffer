package com.jamex.refereestaffer.mcp.view;

import java.util.List;

/**
 * A page of matches. Paging is not optional on the match listing: a full season is a few
 * hundred rows and would eat an AI client's context for no benefit, so every response
 * says where it sits in the whole set.
 *
 * @param queue the queue the listing was filtered to, or null for all queues
 */
public record MatchPage(
        Short queue,
        int page,
        int pageSize,
        long totalMatches,
        int totalPages,
        List<MatchSummary> matches
) {
}
