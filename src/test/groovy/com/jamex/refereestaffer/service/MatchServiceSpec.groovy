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

    def "should get matches to assign for given queue and set hardness level FULL"() {
        given:
        short queue = 2
        def homeTeam = [points: 10, place: 2, city: "city1"] as Team
        def awayTeam = [points: 30, place: awayTeamPlace, city: awayTeamCity] as Team
        def matches = [Match.builder()
                               .home(homeTeam)
                               .away(awayTeam)
                               .build()]
        def matchHardnessLvlMultiplier = [value: 2.5] as Config
        def matchHardnessIncrementer = [value: 100] as Config
        def numberOfEdgeTeams = [value: edgeTeams] as Config
        def matchHardnessDerbyIncrementer = [value: 15] as Config
        def matchHardnessTopIncrementer = [value: 11] as Config
        def matchHardnessBottomIncrementer = [value: 7] as Config

        when:
        def result = matchService.getMatchesToAssignInQueue(queue)

        then:
        def expectedHardnessLevel = (matchHardnessIncrementer.value - Math.abs(awayTeam.points - homeTeam.points)) * matchHardnessLvlMultiplier.value
        if (isDerby) expectedHardnessLevel += matchHardnessDerbyIncrementer.value
        if (isTopMatch) expectedHardnessLevel += matchHardnessTopIncrementer.value
        else if (isBottomMatch) expectedHardnessLevel += matchHardnessBottomIncrementer.value
        result.get(0).hardnessLvl == expectedHardnessLevel
        1 * matchRepository.findAllByHomeScoreNotNullAndAwayScoreNotNull() >> []
        1 * matchRepository.findAllByQueueAndRefereeIsNull(queue) >> matches
        matches.size() * configurationRepository.findByName(ConfigName.HARDNESS_LEVEL_MULTIPLIER) >> matchHardnessLvlMultiplier
        matches.size() * configurationRepository.findByName(ConfigName.HARDNESS_LEVEL_INCREMENTER) >> matchHardnessIncrementer
        matches.size() * configurationRepository.findByName(ConfigName.NUMBER_OF_EDGE_TEAMS) >> numberOfEdgeTeams
        (0..matches.size()) * configurationRepository.findByName(ConfigName.HARDNESS_LEVEL_SAME_CITY_INCREMENTER) >> matchHardnessDerbyIncrementer
        (0..matches.size()) * configurationRepository.findByName(ConfigName.HARDNESS_LEVEL_MATCH_ON_TOP_INCREMENTER) >> matchHardnessTopIncrementer
        (0..matches.size()) * configurationRepository.findByName(ConfigName.HARDNESS_LEVEL_MATCH_ON_BOTTOM_INCREMENTER) >> matchHardnessBottomIncrementer
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
}
