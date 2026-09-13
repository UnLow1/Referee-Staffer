package com.jamex.refereestaffer.model.entity;

/**
 * Enum names map 1:1 to rows in {@code data.sql} and to the keys exposed via
 * {@code /api/configuration}. The {@link #group()} + {@link #description()} pair drives
 * the Configuration screen's panel grouping, served from the backend so adding a new
 * key only requires editing this enum.
 */
public enum ConfigName {
    DIFFICULTY_LEVEL_MULTIPLIER(
            "difficulty",
            "Multiplier on the closeness-of-table base term for match difficulty."),
    DIFFICULTY_LEVEL_INCREMENTER(
            "difficulty",
            "Constant added before subtracting the points difference (sets a ceiling)."),
    DIFFICULTY_LEVEL_SAME_CITY_INCREMENTER(
            "difficulty",
            "Bonus difficulty when both teams are from the same city (a derby)."),
    DIFFICULTY_LEVEL_MATCH_ON_TOP_INCREMENTER(
            "difficulty",
            "Bonus difficulty when both teams are in the top edge of the table."),
    DIFFICULTY_LEVEL_MATCH_ON_BOTTOM_INCREMENTER(
            "difficulty",
            "Bonus difficulty when both teams are in the relegation zone."),
    NUMBER_OF_EDGE_TEAMS(
            "difficulty",
            "Defines the size of the top / bottom edges (e.g. 3 → top-3, bottom-3)."),
    AVERAGE_GRADE_MULTIPLIER(
            "potential",
            "Weight on the referee's average observer grade in the potential formula."),
    EXPERIENCE_MULTIPLIER(
            "potential",
            "Weight on years of experience. Small values mean experience barely moves the score."),
    NUMBER_OF_MATCHES_MULTIPLIER(
            "effective",
            "Penalty proportional to how many matches the referee has done already."),
    HOME_TEAM_REFEREED_MULTIPLIER(
            "effective",
            "Penalty when the candidate has refereed the home team before."),
    AWAY_TEAM_REFEREED_MULTIPLIER(
            "effective",
            "Penalty when the candidate has refereed the away team before.");

    private final String group;
    private final String description;

    ConfigName(String group, String description) {
        this.group = group;
        this.description = description;
    }

    /** UI grouping — one of "potential", "difficulty", "effective". */
    public String group() {
        return group;
    }

    /** One-line human-readable description shown under the input on the Configuration screen. */
    public String description() {
        return description;
    }
}
