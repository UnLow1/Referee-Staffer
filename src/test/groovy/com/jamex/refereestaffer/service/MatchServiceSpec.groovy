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
        matchService = new MatchService(matchRepository, gradeRepository, configurationRepository,
                teamRepository, refereeRepository, new MatchConverter())
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

    def "should calculate points for teams"() {
        given:
        def team1 = [id: 1L] as Team
        def team2 = [id: 2L] as Team
        def team3 = [id: 3L] as Team
        def match1 = [homeScore: 2, awayScore: 0, home: team1, away: team2] as Match
        def match2 = [homeScore: 1, awayScore: 1, home: team3, away: team2] as Match
        def match3 = [homeScore: 2, awayScore: 3, home: team1, away: team3] as Match
        def matches = [match1, match2, match3]

        when:
        def table = matchService.calculatePointsForTeams(matches)

        then: "the returned table is keyed by team id"
        table.pointsOf(team1) == MatchService.POINTS_FOR_WIN_MATCH
        table.pointsOf(team2) == MatchService.POINTS_FOR_DRAW_MATCH
        table.pointsOf(team3) == (short) (MatchService.POINTS_FOR_DRAW_MATCH + MatchService.POINTS_FOR_WIN_MATCH)
        table.placeOf(team1) == (short) 2
        table.placeOf(team2) == (short) 3
        table.placeOf(team3) == (short) 1

        and: "the transient entity fields still mirror it — removing them is RS-99"
        team1.points == MatchService.POINTS_FOR_WIN_MATCH
        team2.points == MatchService.POINTS_FOR_DRAW_MATCH
        team3.points == (short) (MatchService.POINTS_FOR_DRAW_MATCH + MatchService.POINTS_FOR_WIN_MATCH)
        team1.place == (short) 2
        team2.place == (short) 3
        team3.place == (short) 1
    }

    def "should rank teams on equal points in the order they were first encountered"() {
        given: "team2 and team3 both finish on 3 points"
        def team1 = [id: 1L] as Team
        def team2 = [id: 2L] as Team
        def team3 = [id: 3L] as Team
        def matches = [
                [homeScore: 0, awayScore: 1, home: team1, away: team2] as Match,
                [homeScore: 1, awayScore: 0, home: team3, away: team1] as Match
        ]

        when:
        def table = matchService.calculatePointsForTeams(matches)

        then: "the tie keeps encounter order — team2 appears before team3 in the match list"
        table.pointsOf(team2) == table.pointsOf(team3)
        table.placeOf(team2) == (short) 1
        table.placeOf(team3) == (short) 2
        table.placeOf(team1) == (short) 3
    }

    def "should not apply edge-match bonus when at least one team is unranked"() {
        given: "only the winner and loser have played — the other two are absent from the table"
        short queue = 2
        def winner = [id: 1L, city: "city1"] as Team
        def loser = [id: 2L, city: "city2"] as Team
        def unranked = [id: 3L, city: "city3"] as Team
        def alsoUnranked = [id: 4L, city: "city4"] as Team
        def finishedMatches = [[homeScore: 2, awayScore: 0, home: winner, away: loser] as Match]
        // All four cities differ, so no permutation accidentally becomes a derby and reaches
        // for a same-city incrementer that is deliberately absent from the config below.
        def matchToAssign = Match.builder()
                .home(homeIsRanked ? winner : unranked)
                .away(awayIsRanked ? loser : alsoUnranked)
                .build()
        def matchHardnessLvlMultiplier = 1.0d
        def matchHardnessIncrementer = 100.0d

        when:
        def result = matchService.getMatchesToAssignInQueue(queue)

        then:
        1 * matchRepository.findAllByHomeScoreNotNullAndAwayScoreNotNull() >> finishedMatches
        1 * matchRepository.findAllByQueue(queue) >> [matchToAssign]
        // Deliberately no edge/top/bottom keys in the map — the unranked guard must return
        // before those values are ever read (a lookup would NPE and fail the test).
        1 * configurationRepository.findAllAsMap() >> [
                (ConfigName.DIFFICULTY_LEVEL_MULTIPLIER) : matchHardnessLvlMultiplier,
                (ConfigName.DIFFICULTY_LEVEL_INCREMENTER): matchHardnessIncrementer
        ]
        1 * teamRepository.count() >> 4

        and: "only the winner carries points, so the gap is 3 when it is in the fixture"
        result.get(0).hardnessLvl == (matchHardnessIncrementer - expectedPointsDiff) * matchHardnessLvlMultiplier

        where:
        homeIsRanked | awayIsRanked | expectedPointsDiff
        false        | false        | 0
        false        | true         | 0
        true         | false        | MatchService.POINTS_FOR_WIN_MATCH
    }

    def "should get matches to assign for given queue and set hardness level from the computed table"() {
        given: "team1 beat team2 2:0, so the table reads 3 pts / place 1 against 0 pts / place 2"
        short queue = 2
        def team1 = [id: 1L, city: "city1"] as Team
        def team2 = [id: 2L, city: "city2"] as Team
        def finishedMatches = [[homeScore: 2, awayScore: 0, home: team1, away: team2] as Match]
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
        1 * teamRepository.count() >> 2

        and: "hardness reflects the 3-point gap; the pair straddles the single-team edge zones"
        result.get(0).hardnessLvl == (matchHardnessIncrementer - MatchService.POINTS_FOR_WIN_MATCH) * matchHardnessLvlMultiplier
    }

    def "should score the breakdown from the table across derby and edge-zone permutations"() {
        given:
        def matchId = 7l
        def homeTeam = [id: 1L, city: "city1"] as Team
        def awayTeam = [id: 2L, city: awayTeamCity] as Team
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
        def result = matchService.computeBreakdown(match, table, config, 3)

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
        def homeTeam = [points: 0, place: 0, city: "city1"] as Team
        def awayTeam = [points: 0, place: 0, city: "city2"] as Team
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
        1 * matchRepository.findAllByQueue(queue) >> [unassigned, assignedUnfinished, centralAssigned, finishedAssigned, finishedUnassigned]
        1 * configurationRepository.findAllAsMap() >> [
                (ConfigName.DIFFICULTY_LEVEL_MULTIPLIER) : 1.0d,
                (ConfigName.DIFFICULTY_LEVEL_INCREMENTER): 100.0d
        ]
        1 * teamRepository.count() >> 3
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
        given: "the match and the finished matches return distinct Team instances for the same ids"
        def matchId = 7l
        // What findById returns: never went through a ranking pass, so its transient fields
        // are still 0 / null. Reading points and place off these instances is exactly the
        // RS-75 regression — with open-in-view: false this is what production hands us.
        def staleHome = [id: 1L, city: "city1"] as Team
        def staleAway = [id: 2L, city: "city2"] as Team
        def match = Match.builder().id(matchId).home(staleHome).away(staleAway).build()

        def rankedHome = [id: 1L, city: "city1"] as Team
        def rankedAway = [id: 2L, city: "city2"] as Team
        def finishedMatches = [[homeScore: 2, awayScore: 0, home: rankedHome, away: rankedAway] as Match]
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
        1 * teamRepository.count() >> 2

        and: "the match's own instances stayed untouched — proving the numbers came from the table"
        staleHome.points == (short) 0
        staleHome.place == null

        and: "the 3-point gap is still reflected in the score"
        result.flags().pointsDiff() == MatchService.POINTS_FOR_WIN_MATCH
        result.parts().base() == (matchHardnessIncrementer - MatchService.POINTS_FOR_WIN_MATCH) * matchHardnessLvlMultiplier
    }

    def "should not include top or bottom parts in breakdown when a team is unranked"() {
        given:
        def matchId = 8l
        def homeTeam = [id: 1L, city: "city1"] as Team
        def awayTeam = [id: 2L, city: "city2"] as Team
        def match = Match.builder().id(matchId).home(homeTeam).away(awayTeam).build()
        // A team missing from the table has not played a finished match yet, so it has no
        // place and cannot be classified into a table zone.
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
        def result = matchService.computeBreakdown(match, table, config, 3)

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
        def team1 = [id: 1L, city: "city1"] as Team
        def team2 = [id: 2L, city: "city2"] as Team
        def finishedMatches = [[homeScore: 2, awayScore: 0, home: team1, away: team2] as Match]
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
        1 * teamRepository.count() >> 2

        and: "points and places were computed inside the call from the finished matches"
        team1.points == MatchService.POINTS_FOR_WIN_MATCH
        team2.points == (short) 0
        team1.place == (short) 1
        team2.place == (short) 2

        and: "the breakdown is based on the refreshed standings"
        result.flags().pointsDiff() == MatchService.POINTS_FOR_WIN_MATCH
        result.parts().base() == (matchHardnessIncrementer - MatchService.POINTS_FOR_WIN_MATCH) * matchHardnessLvlMultiplier
        result.total() == result.parts().base()
    }

    /**
     * Builds a {@link MatchService.LeagueTable} from plain int maps. The values must reach
     * the record as Shorts — the record's accessors return a primitive short, so an Integer
     * slipping in would blow up on unboxing rather than fail an assertion.
     */
    private static MatchService.LeagueTable leagueTable(Map<Long, Integer> points, Map<Long, Integer> places) {
        new MatchService.LeagueTable(
                points.collectEntries { id, value -> [(id): value as Short] },
                places.collectEntries { id, value -> [(id): value as Short] })
    }
}
