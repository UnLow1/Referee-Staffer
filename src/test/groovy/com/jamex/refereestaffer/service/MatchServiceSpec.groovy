package com.jamex.refereestaffer.service

import com.jamex.refereestaffer.model.entity.*
import com.jamex.refereestaffer.model.exception.MatchNotFoundException
import com.jamex.refereestaffer.repository.ConfigurationRepository
import com.jamex.refereestaffer.repository.GradeRepository
import com.jamex.refereestaffer.repository.MatchRepository
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

    def setup() {
        matchService = new MatchService(matchRepository, gradeRepository, configurationRepository, teamRepository)
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
        def team1 = [] as Team
        def team2 = [] as Team
        def team3 = [] as Team
        def match1 = [homeScore: 2, awayScore: 0, home: team1, away: team2] as Match
        def match2 = [homeScore: 1, awayScore: 1, home: team3, away: team2] as Match
        def match3 = [homeScore: 2, awayScore: 3, home: team1, away: team3] as Match
        def matches = [match1, match2, match3]

        when:
        matchService.calculatePointsForTeams(matches)

        then:
        team1.points == MatchService.POINTS_FOR_WIN_MATCH
        team2.points == MatchService.POINTS_FOR_DRAW_MATCH
        team3.points == (short) (MatchService.POINTS_FOR_DRAW_MATCH + MatchService.POINTS_FOR_WIN_MATCH)
        team1.place == (short) 2
        team2.place == (short) 3
        team3.place == (short) 1
    }

    def "should not apply edge-match bonus when at least one team is unranked"() {
        given:
        short queue = 2
        def homeTeam = [points: homePoints, place: homePlace, city: "city1"] as Team
        def awayTeam = [points: awayPoints, place: awayPlace, city: "city2"] as Team
        def matches = [Match.builder().home(homeTeam).away(awayTeam).build()]
        def matchHardnessLvlMultiplier = 1.0d
        def matchHardnessIncrementer = 100.0d

        when:
        def result = matchService.getMatchesToAssignInQueue(queue)

        then:
        def expectedHardness = (matchHardnessIncrementer - Math.abs(awayTeam.points - homeTeam.points)) * matchHardnessLvlMultiplier
        result.get(0).hardnessLvl == expectedHardness
        1 * matchRepository.findAllByHomeScoreNotNullAndAwayScoreNotNull() >> []
        1 * matchRepository.findAllByQueueAndRefereeIsNull(queue) >> matches
        // Deliberately no edge/top/bottom keys in the map — the unranked guard must return
        // before those values are ever read (a lookup would NPE and fail the test).
        1 * configurationRepository.findAllAsMap() >> [
                (ConfigName.DIFFICULTY_LEVEL_MULTIPLIER) : matchHardnessLvlMultiplier,
                (ConfigName.DIFFICULTY_LEVEL_INCREMENTER): matchHardnessIncrementer
        ]
        1 * teamRepository.count() >> 3

        where:
        homePoints | homePlace | awayPoints | awayPlace
        0          | null      | 0          | null
        0          | null      | 30         | 1
        30         | 1         | 0          | null
    }

    def "should get matches to assign for given queue and set hardness level FULL"() {
        given:
        short queue = 2
        def homeTeam = [points: 10, place: 2, city: "city1"] as Team
        def awayTeam = [points: 30, place: awayTeamPlace, city: awayTeamCity] as Team
        def matches = [Match.builder()
                               .home(homeTeam)
                               .away(awayTeam)
                               .build()]
        def matchHardnessLvlMultiplier = 2.5d
        def matchHardnessIncrementer = 100.0d
        def matchHardnessDerbyIncrementer = 15.0d
        def matchHardnessTopIncrementer = 11.0d
        def matchHardnessBottomIncrementer = 7.0d

        when:
        def result = matchService.getMatchesToAssignInQueue(queue)

        then:
        def expectedHardnessLevel = (matchHardnessIncrementer - Math.abs(awayTeam.points - homeTeam.points)) * matchHardnessLvlMultiplier
        if (isDerby) expectedHardnessLevel += matchHardnessDerbyIncrementer
        if (isTopMatch) expectedHardnessLevel += matchHardnessTopIncrementer
        else if (isBottomMatch) expectedHardnessLevel += matchHardnessBottomIncrementer
        result.get(0).hardnessLvl == expectedHardnessLevel
        1 * matchRepository.findAllByHomeScoreNotNullAndAwayScoreNotNull() >> []
        1 * matchRepository.findAllByQueueAndRefereeIsNull(queue) >> matches
        1 * configurationRepository.findAllAsMap() >> [
                (ConfigName.DIFFICULTY_LEVEL_MULTIPLIER)                : matchHardnessLvlMultiplier,
                (ConfigName.DIFFICULTY_LEVEL_INCREMENTER)               : matchHardnessIncrementer,
                (ConfigName.NUMBER_OF_EDGE_TEAMS)                       : edgeTeams as Double,
                (ConfigName.DIFFICULTY_LEVEL_SAME_CITY_INCREMENTER)     : matchHardnessDerbyIncrementer,
                (ConfigName.DIFFICULTY_LEVEL_MATCH_ON_TOP_INCREMENTER)  : matchHardnessTopIncrementer,
                (ConfigName.DIFFICULTY_LEVEL_MATCH_ON_BOTTOM_INCREMENTER): matchHardnessBottomIncrementer
        ]
        1 * teamRepository.count() >> 3

        where:
        awayTeamPlace | awayTeamCity | edgeTeams | isDerby | isTopMatch | isBottomMatch
        1             | "city2"      | 0         | false   | false      | false
        1             | "city1"      | 0         | true    | false      | false
        1             | "city2"      | 2         | false   | true       | false
        3             | "city2"      | 2         | false   | false      | true
        1             | "city1"      | 2         | true    | true       | false
        3             | "city1"      | 2         | true    | false      | true
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

    def "should compute difficulty breakdown with parts summing to total"() {
        given:
        def matchId = 7l
        def homeTeam = [points: 10, place: 2, city: "city1"] as Team
        def awayTeam = [points: 30, place: awayTeamPlace, city: awayTeamCity] as Team
        def match = Match.builder().id(matchId).home(homeTeam).away(awayTeam).build()
        def matchHardnessLvlMultiplier = 2.5d
        def matchHardnessIncrementer = 100.0d
        def matchHardnessDerbyIncrementer = 15.0d
        def matchHardnessTopIncrementer = 11.0d
        def matchHardnessBottomIncrementer = 7.0d

        when:
        def result = matchService.computeDifficultyBreakdown(matchId)

        then:
        1 * matchRepository.findById(matchId) >> Optional.of(match)
        1 * matchRepository.findAllByHomeScoreNotNullAndAwayScoreNotNull() >> []
        1 * configurationRepository.findAllAsMap() >> [
                (ConfigName.DIFFICULTY_LEVEL_MULTIPLIER)                : matchHardnessLvlMultiplier,
                (ConfigName.DIFFICULTY_LEVEL_INCREMENTER)               : matchHardnessIncrementer,
                (ConfigName.NUMBER_OF_EDGE_TEAMS)                       : edgeTeams as Double,
                (ConfigName.DIFFICULTY_LEVEL_SAME_CITY_INCREMENTER)     : matchHardnessDerbyIncrementer,
                (ConfigName.DIFFICULTY_LEVEL_MATCH_ON_TOP_INCREMENTER)  : matchHardnessTopIncrementer,
                (ConfigName.DIFFICULTY_LEVEL_MATCH_ON_BOTTOM_INCREMENTER): matchHardnessBottomIncrementer
        ]
        1 * teamRepository.count() >> 3

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
        1             | "city2"      | 2         | false   | true       | false
        3             | "city2"      | 2         | false   | false      | true
        1             | "city1"      | 2         | true    | true       | false
        3             | "city1"      | 2         | true    | false      | true
    }

    def "should not include top or bottom parts in breakdown when a team is unranked"() {
        given:
        def matchId = 8l
        def homeTeam = [points: homePoints, place: homePlace, city: "city1"] as Team
        def awayTeam = [points: awayPoints, place: awayPlace, city: "city2"] as Team
        def match = Match.builder().id(matchId).home(homeTeam).away(awayTeam).build()
        def matchHardnessLvlMultiplier = 1.0d
        def matchHardnessIncrementer = 100.0d

        when:
        def result = matchService.computeDifficultyBreakdown(matchId)

        then:
        1 * matchRepository.findById(matchId) >> Optional.of(match)
        1 * matchRepository.findAllByHomeScoreNotNullAndAwayScoreNotNull() >> []
        // Deliberately no edge/top/bottom keys in the map — the unranked guard must return
        // before those values are ever read (a lookup would NPE and fail the test).
        1 * configurationRepository.findAllAsMap() >> [
                (ConfigName.DIFFICULTY_LEVEL_MULTIPLIER) : matchHardnessLvlMultiplier,
                (ConfigName.DIFFICULTY_LEVEL_INCREMENTER): matchHardnessIncrementer
        ]
        1 * teamRepository.count() >> 3

        result.parts().top() == 0.0d
        result.parts().bottom() == 0.0d
        result.total() == (matchHardnessIncrementer - Math.abs(awayPoints - homePoints)) * matchHardnessLvlMultiplier
        !result.flags().isTop()
        !result.flags().isBot()

        where:
        homePoints | homePlace | awayPoints | awayPlace
        0          | null      | 0          | null
        0          | null      | 30         | 1
        30         | 1         | 0          | null
    }

    def "should refresh standings from finished matches before computing breakdown"() {
        given:
        def matchId = 5l
        def team1 = [city: "city1"] as Team
        def team2 = [city: "city2"] as Team
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
}
