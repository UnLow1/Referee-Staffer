package com.jamex.refereestaffer.service

import com.jamex.refereestaffer.model.converter.MatchConverter
import com.jamex.refereestaffer.model.dto.MatchDto
import com.jamex.refereestaffer.model.entity.*
import com.jamex.refereestaffer.model.exception.MatchNotFoundException
import com.jamex.refereestaffer.model.exception.TeamNotFoundException
import com.jamex.refereestaffer.repository.ConfigurationRepository
import com.jamex.refereestaffer.repository.GradeRepository
import com.jamex.refereestaffer.repository.MatchRepository
import com.jamex.refereestaffer.repository.RefereeRepository
import com.jamex.refereestaffer.repository.TeamRepository
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDateTime

class MatchServiceSpec extends Specification {

    @Subject
    MatchService matchService

    MatchRepository matchRepository = Mock()
    GradeRepository gradeRepository = Mock()
    ConfigurationRepository configurationRepository = Mock()
    TeamRepository teamRepository = Mock()
    RefereeRepository refereeRepository = Mock()

    def setup() {
        // The converter is a pure mapper since RS-71, so the real one is used instead of a mock —
        // these features then cover the whole resolve-references + convert path end to end.
        // TeamService is real for the same reason: since RS-99 it owns the only league-table
        // computation, so a mock here would let the table's ranking rules (which decide the
        // zone bonuses asserted below) drift away from what production scores against. The
        // mocks stay at the repository boundary.
        matchService = new MatchService(matchRepository, gradeRepository, configurationRepository,
                teamRepository, refereeRepository, new MatchConverter(),
                new TeamService(teamRepository, matchRepository))
    }

    def "should save match with references resolved by bulk queries"() {
        given:
        def homeTeam = [getId: { 1l }] as Team
        def awayTeam = [getId: { 2l }] as Team
        def referee = [getId: { 7l }] as Referee
        def grade = [getId: { 9l }] as Grade
        def matchDto = MatchDto.builder()
                .id(23l)
                .queue(3 as short)
                .homeTeamId(1l)
                .awayTeamId(2l)
                .refereeId(7l)
                .gradeId(9l)
                .build()

        when:
        def result = matchService.saveMatch(matchDto)

        then:
        1 * teamRepository.findAllById([1l, 2l]) >> [homeTeam, awayTeam]
        1 * refereeRepository.findAllById([7l]) >> [referee]
        1 * gradeRepository.findAllById([9l]) >> [grade]
        1 * matchRepository.save({ Match match ->
            match.home == homeTeam && match.away == awayTeam && match.referee == referee && match.grade == grade
        }) >> { Match match -> match }
        result.id == matchDto.id
        result.queue == matchDto.queue
        result.homeTeamId == 1l
        result.awayTeamId == 2l
        result.refereeId == 7l
        result.gradeId == 9l
    }

    def "should save match without optional references and skip their queries"() {
        given:
        def homeTeam = [getId: { 1l }] as Team
        def awayTeam = [getId: { 2l }] as Team
        def matchDto = MatchDto.builder()
                .queue(3 as short)
                .homeTeamId(1l)
                .awayTeamId(2l)
                .build()

        when:
        def result = matchService.saveMatch(matchDto)

        then:
        1 * teamRepository.findAllById([1l, 2l]) >> [homeTeam, awayTeam]
        0 * refereeRepository.findAllById(_)
        0 * gradeRepository.findAllById(_)
        1 * matchRepository.save({ Match match -> match.referee == null && match.grade == null }) >> { Match match -> match }
        result.refereeId == null
        result.gradeId == null
    }

    def "should resolve unknown referee and grade ids to null when saving match"() {
        given:
        def homeTeam = [getId: { 1l }] as Team
        def awayTeam = [getId: { 2l }] as Team
        def matchDto = MatchDto.builder()
                .queue(3 as short)
                .homeTeamId(1l)
                .awayTeamId(2l)
                .refereeId(7l)
                .gradeId(9l)
                .build()

        when:
        matchService.saveMatch(matchDto)

        then:
        1 * teamRepository.findAllById([1l, 2l]) >> [homeTeam, awayTeam]
        1 * refereeRepository.findAllById([7l]) >> []
        1 * gradeRepository.findAllById([9l]) >> []
        1 * matchRepository.save({ Match match -> match.referee == null && match.grade == null }) >> { Match match -> match }
    }

    def "should throw TeamNotFoundException when home or away team has not been found"() {
        given:
        def correctTeamId = 1l
        def wrongTeamId = 987l
        def correctTeam = [getId: { correctTeamId }] as Team
        def matchDto = MatchDto.builder()
                .homeTeamId(homeTeamId)
                .awayTeamId(awayTeamId)
                .build()

        when:
        matchService.saveMatch(matchDto)

        then:
        1 * teamRepository.findAllById({ it.toSet() == [correctTeamId, wrongTeamId].toSet() }) >> [correctTeam]
        def exception = thrown(TeamNotFoundException)
        exception.message == String.format(TeamNotFoundException.NOT_FOUND_WITH_ID, wrongTeamId)

        where:
        homeTeamId | awayTeamId
        1l         | 987l
        987l       | 1l
    }

    def "should bulk update matches with a single query per repository"() {
        given:
        def team1 = [getId: { 1l }] as Team
        def team2 = [getId: { 2l }] as Team
        def team3 = [getId: { 3l }] as Team
        def referee = [getId: { 7l }] as Referee
        def matchesDtos = [
                MatchDto.builder().id(31l).queue(3 as short).homeTeamId(1l).awayTeamId(2l).refereeId(7l).build(),
                MatchDto.builder().id(32l).queue(3 as short).homeTeamId(2l).awayTeamId(3l).build()
        ]

        when:
        matchService.updateMatches(matchesDtos)

        then:
        1 * teamRepository.findAllById([1l, 2l, 3l]) >> [team1, team2, team3]
        1 * refereeRepository.findAllById([7l]) >> [referee]
        0 * gradeRepository.findAllById(_)
        1 * matchRepository.saveAll({ List<Match> matches ->
            matches*.id == [31l, 32l] &&
                    matches[0].home == team1 && matches[0].away == team2 && matches[0].referee == referee &&
                    matches[1].home == team2 && matches[1].away == team3 && matches[1].referee == null
        })
    }

    def "should throw MatchNotFoundException when match has not been found"() {
        given:
        def matchId = 2396l

        when:
        matchService.deleteMatch(matchId)

        then:
        1 * matchRepository.findById(matchId) >> Optional.empty()
        def exception = thrown(MatchNotFoundException)
        exception.message == String.format(MatchNotFoundException.NOT_FOUND, matchId)
    }

    def "should delete match with provided id"() {
        given:
        def matchId = 2396l
        def match = [] as Match

        when:
        matchService.deleteMatch(matchId)

        then:
        1 * matchRepository.findById(matchId) >> Optional.of(match)
        1 * matchRepository.delete(match)
    }

    def "should delete match with provided id and grade for this match"() {
        given:
        def matchId = 2396l
        def grade = [] as Grade
        def match = Match.builder().grade(grade).build()

        when:
        matchService.deleteMatch(matchId)

        then:
        1 * matchRepository.findById(matchId) >> Optional.of(match)
        1 * gradeRepository.delete(grade)
        1 * matchRepository.delete(match)
    }

    def "should not apply edge-match bonus when a team is missing from the table"() {
        given: "the away team is absent from the standings — deleted between the two queries"
        short queue = 2
        def home = team(1L, "Alfa", "city1")
        def away = team(2L, "Beta", "city2")
        def matchToAssign = Match.builder().home(home).away(away).build()

        when:
        def result = matchService.getMatchesToAssignInQueue(queue)

        then:
        1 * matchRepository.findAllByHomeScoreNotNullAndAwayScoreNotNull() >> []
        1 * teamRepository.findAll() >> [home]
        1 * matchRepository.findAllByQueue(queue) >> [matchToAssign]
        // Deliberately no edge/top/bottom keys in the map — the missing-team guard must
        // return before those values are ever read (a lookup would NPE and fail the test).
        1 * configurationRepository.findAllAsMap() >> [
                (ConfigName.DIFFICULTY_LEVEL_MULTIPLIER) : 1.0d,
                (ConfigName.DIFFICULTY_LEVEL_INCREMENTER): 100.0d
        ]

        and: "only the base part is scored"
        result.get(0).hardnessLvl == 100.0d
    }

    def "should get matches to assign for given queue and set hardness level from the computed table"() {
        given: "team1 beat team2 2:0, so the table reads 3 pts / place 1 against 0 pts / place 2"
        short queue = 2
        def team1 = team(1L, "Alfa", "city1")
        def team2 = team(2L, "Beta", "city2")
        def finishedMatches = [finished(team1, team2, 2, 0)]
        def matchToAssign = Match.builder().home(team1).away(team2).build()
        def matchHardnessLvlMultiplier = 2.5d
        def matchHardnessIncrementer = 100.0d

        when:
        def result = matchService.getMatchesToAssignInQueue(queue)

        then:
        1 * matchRepository.findAllByHomeScoreNotNullAndAwayScoreNotNull() >> finishedMatches
        1 * matchRepository.findAllByQueue(queue) >> [matchToAssign]
        1 * configurationRepository.findAllAsMap() >> [
                (ConfigName.DIFFICULTY_LEVEL_MULTIPLIER)              : matchHardnessLvlMultiplier,
                (ConfigName.DIFFICULTY_LEVEL_INCREMENTER)             : matchHardnessIncrementer,
                (ConfigName.NUMBER_OF_EDGE_TEAMS)                     : 1.0d,
                (ConfigName.DIFFICULTY_LEVEL_SAME_CITY_INCREMENTER)   : 15.0d,
                (ConfigName.DIFFICULTY_LEVEL_MATCH_ON_TOP_INCREMENTER): 11.0d,
                (ConfigName.DIFFICULTY_LEVEL_MATCH_ON_BOTTOM_INCREMENTER): 7.0d
        ]
        1 * teamRepository.findAll() >> [team1, team2]

        and: "hardness reflects the 3-point gap; the pair straddles the single-team edge zones"
        result.get(0).hardnessLvl == (matchHardnessIncrementer - TeamService.POINTS_FOR_WIN_MATCH) * matchHardnessLvlMultiplier
    }

    def "should classify top-of-table matches by the tie-broken table order, not by encounter order"() {
        given: "three teams on 3 points each — only goal difference separates them"
        short queue = 4
        def alfa = team(1L, "Alfa", "city1")
        def beta = team(2L, "Beta", "city2")
        def gamma = team(3L, "Gamma", "city3")
        def delta = team(4L, "Delta", "city4")
        def epsilon = team(5L, "Epsilon", "city5")
        // Encounter order (gamma first, alfa last) is deliberately the reverse of the
        // goal-difference order the table sorts by.
        def finishedMatches = [finished(gamma, delta, 1, 0),   // gamma +1
                               finished(beta, epsilon, 2, 0),  // beta  +2
                               finished(alfa, delta, 3, 0)]    // alfa  +3
        def alfaVsBeta = Match.builder().id(10l).home(alfa).away(beta).build()
        def gammaVsBeta = Match.builder().id(11l).home(gamma).away(beta).build()

        when:
        def result = matchService.getMatchesToAssignInQueue(queue)

        then:
        1 * matchRepository.findAllByHomeScoreNotNullAndAwayScoreNotNull() >> finishedMatches
        1 * teamRepository.findAll() >> [alfa, beta, gamma, delta, epsilon]
        1 * matchRepository.findAllByQueue(queue) >> [gammaVsBeta, alfaVsBeta]
        1 * configurationRepository.findAllAsMap() >> configWithEdgeTeams(2.0d)

        and: "alfa (GD +3) and beta (GD +2) hold places 1-2, so only their match is a top match"
        // The pre-RS-99 ranking sorted on points alone and kept encounter order for ties,
        // which put gamma 1st and alfa 3rd — the top bonus would have landed on the other
        // match. Both fixtures are goalless in points terms (3 vs 3), so base is identical
        // and the 7.0 is the whole difference.
        result*.id == [10l, 11l]
        result[0].hardnessLvl == 107.0d
        result[1].hardnessLvl == 100.0d
    }

    def "should rank a team without a finished match and let it reach a table zone"() {
        given: "only alfa and beta have played; gamma and delta have not"
        def matchId = 31l
        def alfa = team(1L, "Alfa", "city1")
        def beta = team(2L, "Beta", "city2")
        def gamma = team(3L, "Gamma", "city3")
        def delta = team(4L, "Delta", "city4")
        def finishedMatches = [finished(alfa, beta, 2, 0)]
        // Table: alfa 1st (3 pts), then the two goalless newcomers, which are level on
        // points, goal difference and goals scored, so the name breaks the tie —
        // "Delta" < "Gamma", hence delta 2nd and gamma 3rd — and beta last on GD -2.
        def match = Match.builder().id(matchId).home(alfa).away(delta).build()

        when:
        def result = matchService.computeDifficultyBreakdown(matchId)

        then:
        1 * matchRepository.findById(matchId) >> Optional.of(match)
        1 * matchRepository.findAllByHomeScoreNotNullAndAwayScoreNotNull() >> finishedMatches
        1 * teamRepository.findAll() >> [alfa, beta, gamma, delta]
        1 * configurationRepository.findAllAsMap() >> configWithEdgeTeams(2.0d)

        and: "delta counts as a top-2 side even though it has never played"
        // Before RS-99 delta was unranked, the edge check bailed out on the null place and
        // the match scored a flat 97.0.
        result.flags().isTop()
        !result.flags().isBot()
        result.parts().base() == 97.0d
        result.parts().top() == 7.0d
        result.total() == 104.0d
    }

    def "should not apply edge-match bonuses before the season has produced a result"() {
        given: "four teams and no finished match at all"
        short queue = 1
        def alfa = team(1L, "Alfa", "city1")
        def beta = team(2L, "Beta", "city2")
        def gamma = team(3L, "Gamma", "city3")
        def delta = team(4L, "Delta", "city4")
        // Every team is level on 0 pts / 0 GD / 0 GF, so the table order is nothing but the
        // alphabet: Alfa, Beta, Delta, Gamma. Classifying the first two as a top-of-table
        // fixture and the last two as a relegation six-pointer would make the club name
        // decide the staffing order, so the zones stay shut until something has been played.
        def alphabeticallyFirst = Match.builder().id(41l).home(alfa).away(beta).build()
        def alphabeticallyLast = Match.builder().id(42l).home(delta).away(gamma).build()

        when:
        def result = matchService.getMatchesToAssignInQueue(queue)

        then:
        1 * matchRepository.findAllByHomeScoreNotNullAndAwayScoreNotNull() >> []
        1 * teamRepository.findAll() >> [alfa, beta, gamma, delta]
        1 * matchRepository.findAllByQueue(queue) >> [alphabeticallyFirst, alphabeticallyLast]
        1 * configurationRepository.findAllAsMap() >> configWithEdgeTeams(2.0d)

        and: "both fixtures score the bare base part — no top, no bottom"
        // With the zones open these would have been 107.0 (places 1-2) and 105.0 (places
        // 3-4) instead, on an empty table.
        result*.hardnessLvl == [100.0d, 100.0d]
        result.every { it.hardnessLvl == 100.0d }
    }

    def "should apply the bottom-of-table bonus early in the season"() {
        given: "eight teams and a single finished match"
        def matchId = 21l
        // Zero-padded so the name tie-break is plain lexicographic order with no surprises
        // (unpadded, "Team10" would sort ahead of "Team2" as soon as the fixture grows).
        def teams = (1..8).collect { team(it as long, "Team%02d".formatted(it), "city$it") }
        def finishedMatches = [finished(teams[0], teams[1], 2, 0)]
        // Places: Team01 1st (3 pts), then Team03..Team08 on 0 pts / GD 0 / GF 0 — a full
        // tie the table breaks by name — as 2nd..7th, and Team02 last on GD -2. So
        // Team07 = 6th, Team08 = 7th, both inside `place > 8 - 3`.
        def match = Match.builder().id(matchId).home(teams[6]).away(teams[7]).build()

        when:
        def result = matchService.computeDifficultyBreakdown(matchId)

        then:
        1 * matchRepository.findById(matchId) >> Optional.of(match)
        1 * matchRepository.findAllByHomeScoreNotNullAndAwayScoreNotNull() >> finishedMatches
        1 * teamRepository.findAll() >> teams
        1 * configurationRepository.findAllAsMap() >> configWithEdgeTeams(3.0d)

        and: "both sit inside the bottom-3 zone of an eight-team table"
        // This is the silent bug RS-99 fixes: the zone was measured against all eight teams
        // while only the two with a finished match were ranked, so `place > 8 - 3` was
        // unreachable and the bonus never fired this early. Total used to be 100.0.
        result.flags().isBot()
        !result.flags().isTop()
        result.parts().base() == 100.0d
        result.parts().bottom() == 5.0d
        result.total() == 105.0d
    }

    def "should score the breakdown from the table across derby and edge-zone permutations"() {
        given:
        def matchId = 7l
        def homeTeam = team(1L, "Alfa", "city1")
        def awayTeam = team(2L, "Beta", awayTeamCity)
        def match = Match.builder().id(matchId).home(homeTeam).away(awayTeam).build()
        def table = leagueTable([(1L): 10, (2L): 30], [(1L): 2, (2L): awayTeamPlace])
        def matchHardnessLvlMultiplier = 2.5d
        def matchHardnessIncrementer = 100.0d
        def matchHardnessDerbyIncrementer = 15.0d
        def matchHardnessTopIncrementer = 11.0d
        def matchHardnessBottomIncrementer = 7.0d
        def config = [
                (ConfigName.DIFFICULTY_LEVEL_MULTIPLIER)                : matchHardnessLvlMultiplier,
                (ConfigName.DIFFICULTY_LEVEL_INCREMENTER)               : matchHardnessIncrementer,
                (ConfigName.NUMBER_OF_EDGE_TEAMS)                       : edgeTeams as Double,
                (ConfigName.DIFFICULTY_LEVEL_SAME_CITY_INCREMENTER)     : matchHardnessDerbyIncrementer,
                (ConfigName.DIFFICULTY_LEVEL_MATCH_ON_TOP_INCREMENTER)  : matchHardnessTopIncrementer,
                (ConfigName.DIFFICULTY_LEVEL_MATCH_ON_BOTTOM_INCREMENTER): matchHardnessBottomIncrementer
        ]

        when:
        def result = matchService.computeBreakdown(match, table, config)

        then:
        result.matchId() == matchId
        result.parts().base() == (matchHardnessIncrementer - 20) * matchHardnessLvlMultiplier
        result.parts().sameCity() == (isDerby ? matchHardnessDerbyIncrementer : 0.0d)
        result.parts().top() == (isTopMatch ? matchHardnessTopIncrementer : 0.0d)
        result.parts().bottom() == (isBottomMatch ? matchHardnessBottomIncrementer : 0.0d)

        and: "the parts always add up to the total"
        result.total() == result.parts().base() + result.parts().sameCity() + result.parts().top() + result.parts().bottom()

        and: "top and bottom are mutually exclusive"
        !(result.parts().top() > 0 && result.parts().bottom() > 0)

        and: "flags mirror the parts"
        result.flags().sameCity() == isDerby
        result.flags().isTop() == isTopMatch
        result.flags().isBot() == isBottomMatch
        result.flags().pointsDiff() == 20

        where:
        // The table lists two teams, so `size()` is 2 and the bottom zone is `place > 2 - edge`.
        awayTeamPlace | awayTeamCity | edgeTeams | isDerby | isTopMatch | isBottomMatch
        1             | "city2"      | 0         | false   | false      | false
        1             | "city1"      | 0         | true    | false      | false
        1             | "city2"      | 2         | false   | true       | false
        3             | "city2"      | 2         | false   | false      | true
        1             | "city1"      | 2         | true    | true       | false
        3             | "city1"      | 2         | true    | false      | true
    }

    def "should include previously assigned matches but keep central and finished assignments"() {
        given:
        short queue = 2
        def homeTeam = team(1L, "Alfa", "city1")
        def awayTeam = team(2L, "Beta", "city2")
        def unassigned = Match.builder().home(homeTeam).away(awayTeam).build()
        def assignedUnfinished = Match.builder()
                .home(homeTeam).away(awayTeam)
                .referee(new Referee("John", "Doe"))
                .build()
        def centralAssigned = Match.builder()
                .home(homeTeam).away(awayTeam)
                .referee(new Referee("S", "C"))
                .build()
        def finishedAssigned = Match.builder()
                .home(homeTeam).away(awayTeam)
                .referee(new Referee("Jane", "Smith"))
                .homeScore((short) 2).awayScore((short) 1)
                .build()
        def finishedUnassigned = Match.builder()
                .home(homeTeam).away(awayTeam)
                .homeScore((short) 0).awayScore((short) 0)
                .build()

        when:
        def result = matchService.getMatchesToAssignInQueue(queue)

        then:
        result.size() == 3
        result.containsAll([unassigned, assignedUnfinished, finishedUnassigned])
        !result.contains(centralAssigned)
        !result.contains(finishedAssigned)
        1 * matchRepository.findAllByHomeScoreNotNullAndAwayScoreNotNull() >> []
        1 * teamRepository.findAll() >> [homeTeam, awayTeam]
        1 * matchRepository.findAllByQueue(queue) >> [unassigned, assignedUnfinished, centralAssigned, finishedAssigned, finishedUnassigned]
        // Deliberately no edge/top/bottom keys in the map — nothing has been played, so the
        // unranked-table guard must return before those values are ever read.
        1 * configurationRepository.findAllAsMap() >> [
                (ConfigName.DIFFICULTY_LEVEL_MULTIPLIER) : 1.0d,
                (ConfigName.DIFFICULTY_LEVEL_INCREMENTER): 100.0d
        ]
    }

    def "should throw MatchNotFoundException when computing breakdown for missing match"() {
        given:
        def matchId = 44l

        when:
        matchService.computeDifficultyBreakdown(matchId)

        then:
        1 * matchRepository.findById(matchId) >> Optional.empty()
        def exception = thrown(MatchNotFoundException)
        exception.message == String.format(MatchNotFoundException.NOT_FOUND, matchId)
    }

    def "should score the breakdown from the table even when the match carries teams from another persistence context"() {
        given: "the match and the standings return distinct Team instances for the same ids"
        def matchId = 7l
        // With open-in-view: false the two repository calls run in separate persistence
        // contexts, so these are different objects than the ones the table was built from
        // and Team has no equals/hashCode — the scoring must go through the id-keyed
        // lookups (RS-75).
        def staleHome = team(1L, "Alfa", "city1")
        def staleAway = team(2L, "Beta", "city2")
        def match = Match.builder().id(matchId).home(staleHome).away(staleAway).build()

        def rankedHome = team(1L, "Alfa", "city1")
        def rankedAway = team(2L, "Beta", "city2")
        def finishedMatches = [finished(rankedHome, rankedAway, 2, 0)]
        def matchHardnessLvlMultiplier = 2.0d
        def matchHardnessIncrementer = 100.0d

        when:
        def result = matchService.computeDifficultyBreakdown(matchId)

        then:
        1 * matchRepository.findById(matchId) >> Optional.of(match)
        1 * matchRepository.findAllByHomeScoreNotNullAndAwayScoreNotNull() >> finishedMatches
        1 * configurationRepository.findAllAsMap() >> [
                (ConfigName.DIFFICULTY_LEVEL_MULTIPLIER) : matchHardnessLvlMultiplier,
                (ConfigName.DIFFICULTY_LEVEL_INCREMENTER): matchHardnessIncrementer,
                (ConfigName.NUMBER_OF_EDGE_TEAMS)        : 1.0d
        ]
        1 * teamRepository.findAll() >> [rankedHome, rankedAway]

        and: "the 3-point gap is still reflected in the score"
        result.flags().pointsDiff() == TeamService.POINTS_FOR_WIN_MATCH
        result.parts().base() == (matchHardnessIncrementer - TeamService.POINTS_FOR_WIN_MATCH) * matchHardnessLvlMultiplier
    }

    def "should not include top or bottom parts in breakdown when a team is missing from the table"() {
        given:
        def matchId = 8l
        def homeTeam = team(1L, "Alfa", "city1")
        def awayTeam = team(2L, "Beta", "city2")
        def match = Match.builder().id(matchId).home(homeTeam).away(awayTeam).build()
        // A team the table does not list has no place, so it cannot be classified into a
        // table zone — the whole edge check bails out.
        def table = leagueTable(pointsByTeamId, placeByTeamId)
        def matchHardnessLvlMultiplier = 1.0d
        def matchHardnessIncrementer = 100.0d
        // Deliberately no edge/top/bottom keys in the map — the unranked guard must return
        // before those values are ever read (a lookup would NPE and fail the test).
        def config = [
                (ConfigName.DIFFICULTY_LEVEL_MULTIPLIER) : matchHardnessLvlMultiplier,
                (ConfigName.DIFFICULTY_LEVEL_INCREMENTER): matchHardnessIncrementer
        ]

        when:
        def result = matchService.computeBreakdown(match, table, config)

        then:
        result.parts().top() == 0.0d
        result.parts().bottom() == 0.0d
        result.total() == (matchHardnessIncrementer - expectedPointsDiff) * matchHardnessLvlMultiplier
        !result.flags().isTop()
        !result.flags().isBot()

        where:
        pointsByTeamId          | placeByTeamId | expectedPointsDiff
        [:]                     | [:]           | 0
        [(2L): 30]              | [(2L): 1]     | 30
        [(1L): 30]              | [(1L): 1]     | 30
    }

    def "should refresh standings from finished matches before computing breakdown"() {
        given:
        def matchId = 5l
        def team1 = team(1L, "Alfa", "city1")
        def team2 = team(2L, "Beta", "city2")
        def finishedMatches = [finished(team1, team2, 2, 0)]
        def match = Match.builder().id(matchId).home(team1).away(team2).build()
        def matchHardnessLvlMultiplier = 2.0d
        def matchHardnessIncrementer = 100.0d

        when:
        def result = matchService.computeDifficultyBreakdown(matchId)

        then:
        1 * matchRepository.findById(matchId) >> Optional.of(match)
        1 * matchRepository.findAllByHomeScoreNotNullAndAwayScoreNotNull() >> finishedMatches
        1 * configurationRepository.findAllAsMap() >> [
                (ConfigName.DIFFICULTY_LEVEL_MULTIPLIER) : matchHardnessLvlMultiplier,
                (ConfigName.DIFFICULTY_LEVEL_INCREMENTER): matchHardnessIncrementer,
                (ConfigName.NUMBER_OF_EDGE_TEAMS)        : 1.0d
        ]
        1 * teamRepository.findAll() >> [team1, team2]

        and: "the breakdown is based on the table recomputed inside the call"
        result.flags().pointsDiff() == TeamService.POINTS_FOR_WIN_MATCH
        result.parts().base() == (matchHardnessIncrementer - TeamService.POINTS_FOR_WIN_MATCH) * matchHardnessLvlMultiplier
        result.total() == result.parts().base()
    }

    /**
     * Builds a {@link MatchService.LeagueTable} from plain int maps. The values must reach
     * the record as Shorts — the record's accessors return a primitive short, so an Integer
     * slipping in would blow up on unboxing rather than fail an assertion. The table counts
     * as ranked, i.e. backed by a played match — the not-ranked case is driven end to end
     * through getStandings() instead, since that is where the flag comes from.
     */
    private static MatchService.LeagueTable leagueTable(Map<Long, Integer> points, Map<Long, Integer> places) {
        new MatchService.LeagueTable(
                points.collectEntries { id, value -> [(id): value as Short] },
                places.collectEntries { id, value -> [(id): value as Short] },
                true)
    }

    private static Team team(long id, String name, String city) {
        Team.builder().id(id).name(name).city(city).build()
    }

    private static Match finished(Team home, Team away, int homeScore, int awayScore) {
        new Match(1 as short, home, away, LocalDateTime.now(), null, homeScore as Short, awayScore as Short)
    }

    /** The weights seeded by data.sql, with the edge-zone size left to the caller. */
    private static Map<ConfigName, Double> configWithEdgeTeams(double edgeTeams) {
        [(ConfigName.DIFFICULTY_LEVEL_MULTIPLIER)                 : 1.0d,
         (ConfigName.DIFFICULTY_LEVEL_INCREMENTER)                : 100.0d,
         (ConfigName.NUMBER_OF_EDGE_TEAMS)                        : edgeTeams,
         (ConfigName.DIFFICULTY_LEVEL_SAME_CITY_INCREMENTER)      : 10.0d,
         (ConfigName.DIFFICULTY_LEVEL_MATCH_ON_TOP_INCREMENTER)   : 7.0d,
         (ConfigName.DIFFICULTY_LEVEL_MATCH_ON_BOTTOM_INCREMENTER): 5.0d]
    }
}
