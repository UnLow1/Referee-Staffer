package com.jamex.refereestaffer.mcp.view;

/**
 * One tunable weight of the staffing algorithm, with the human-readable grouping and
 * explanation {@link com.jamex.refereestaffer.model.entity.ConfigName} carries — the same
 * text the Configuration screen renders under each input.
 */
public record AlgorithmParameter(
        String name,
        Double value,
        String group,
        String description
) {
}
