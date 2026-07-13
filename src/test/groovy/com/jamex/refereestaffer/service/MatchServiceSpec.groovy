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
        0          | 0         | 0          | 0
        0          | 0         | 30         | 1
        30         | 1         | 0          | 0
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
        0          | 0         | 0          | 0
        0          | 0         | 30         | 1
        30         | 1         | 0          | 0
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
