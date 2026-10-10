package com.jamex.refereestaffer.mcp;

/**
 * Tool names and descriptions, kept as compile-time constants rather than written inline
 * into the {@code @McpTool} annotations in {@link StaffingMcpTools}.
 *
 * <p>Two reasons. First, how well a model picks the right tool depends almost entirely on
 * these strings, so they deserve to be read and reviewed as one block instead of being
 * scattered across annotations. Second, the same methods are meant to back an in-app AI
 * assistant later (a backend calling the Claude API directly, brainstorm item RS-A-6):
 * that layer needs the same names and descriptions, and annotation values are not
 * reachable from it without reflection.
 *
 * <p>Descriptions are in English on purpose — they are model-facing prompt text, not UI
 * copy.
 */
public final class StaffingToolDescriptions {

    public static final String LIST_REFEREES = "list_referees";
    public static final String LIST_REFEREES_DESCRIPTION = """
            List every referee with their computed statistics: average observer grade, \
            years of experience, number of matches already officiated, the last queue \
            they were assigned to and their potential score (the strength number the \
            staffing algorithm ranks candidates by). Use this to compare referees or to \
            find a referee's id before calling get_referee_profile. Referees flagged \
            central are "Sędzia z Centrali" placeholders for assignments made top-down \
            by the federation; the staffer never reassigns those.""";

    public static final String GET_REFEREE_PROFILE = "get_referee_profile";
    public static final String GET_REFEREE_PROFILE_DESCRIPTION = """
            Full history of one referee: the same statistics list_referees returns, plus \
            every match they officiated (date, queue, teams, score, observer grade) and \
            every vacation period that makes them unavailable. Use this to answer \
            questions about a single referee — which teams they have already seen, how \
            their grades developed, or whether they are available on a given date. \
            Grades may be "split" (written 7.9/8.3 by the observer); the grade field is \
            then the mean of both components, gradeAsAwarded keeps the original.""";

    public static final String LIST_MATCHES = "list_matches";
    public static final String LIST_MATCHES_DESCRIPTION = """
            List matches with their current referee assignment, newest queue last. \
            Always paged, because a full season is several hundred matches: pass a queue \
            to look at one round, and page/pageSize to walk the rest. Returns the total \
            number of matches so you can tell whether you have seen them all. A match \
            with no referee is still waiting to be staffed.""";

    public static final String EXPLAIN_MATCH_DIFFICULTY = "explain_match_difficulty";
    public static final String EXPLAIN_MATCH_DIFFICULTY_DESCRIPTION = """
            Explain how hard one match is to officiate, component by component: the base \
            term from how close the two teams are in the table, the derby bonus when both \
            are from the same city, and the bonuses for a top-of-table or relegation-zone \
            fixture. The parts always sum to the total. Difficulty drives staffing order — \
            the hardest match in a queue gets the strongest available referee. Use this to \
            justify why a match was ranked the way it was.""";

    public static final String GET_STANDINGS = "get_standings";
    public static final String GET_STANDINGS_DESCRIPTION = """
            The league table computed from finished matches: place, points, played, wins, \
            draws, losses and goals for every team. Teams without a finished match appear \
            at the bottom with zeroed stats. Positions in this table are what makes a \
            match a top-of-table or relegation fixture, so read it together with \
            explain_match_difficulty.""";

    public static final String GET_ALGORITHM_CONFIG = "get_algorithm_config";
    public static final String GET_ALGORITHM_CONFIG_DESCRIPTION = """
            The tunable weights of the staffing algorithm with their current values and a \
            one-line explanation of each, grouped into "potential" (how referee strength \
            is computed), "difficulty" (how match hardness is computed) and "effective" \
            (the fairness penalties applied when picking a referee for a specific match). \
            Use this before explaining or second-guessing any score: the formulas are \
            fixed but every weight in them is configurable.""";

    public static final String ALGORITHM_RESOURCE_URI = "refstaffer://algorithm";
    public static final String ALGORITHM_RESOURCE_DESCRIPTION = """
            How the staffer scores assignments: the three formulas (referee potential, \
            match difficulty, effective value) and what each term means. The text version \
            of the "Algorithm explainer" panel on the Staffer screen.""";

    public static final String CONFIG_RESOURCE_URI = "refstaffer://config";
    public static final String CONFIG_RESOURCE_DESCRIPTION = """
            The current values of every staffing weight, as plain text — the same data \
            get_algorithm_config returns, for a client that prefers to attach it as \
            context instead of calling a tool.""";

    private StaffingToolDescriptions() {
    }
}
