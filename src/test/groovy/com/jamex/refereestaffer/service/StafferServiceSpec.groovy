package com.jamex.refereestaffer.service

import com.jamex.refereestaffer.model.converter.MatchConverter
import com.jamex.refereestaffer.model.dto.MatchDto
import com.jamex.refereestaffer.model.entity.ConfigName
import com.jamex.refereestaffer.model.entity.Match
import com.jamex.refereestaffer.model.entity.Referee
import com.jamex.refereestaffer.model.entity.Team
import com.jamex.refereestaffer.repository.ConfigurationRepository
import spock.lang.Specification
import spock.lang.Subject

class StafferServiceSpec extends Specification {

    @Subject
    StafferService stafferService

    ConfigurationRepository configurationRepository = Mock()
    MatchConverter matchConverter = Mock()
    MatchService matchService = Mock()
    RefereeService refereeService = Mock()

    def setup() {
        stafferService = new StafferService(configurationRepository, matchConverter, matchService, refereeService)
    }

    def "should staff referees to matches in queue"() {
        given:
        short queue = 2
        def team1 = Team.builder()
                .name("test team")
                .build()
        def team2 = Team.builder()
                .name("test team 123213")
                .build()
        def ref1 = [averageGrade: 8.1, teamsRefereed: Map.of(team1, (short) 1, team2, (short) 1), numberOfMatchesInRound: 7] as Referee
        def ref2 = [experience: 100, teamsRefereed: [:], numberOfMatchesInRound: 0] as Referee
        def match1 = Match.builder()
                .home(team1)
                .away(team2)
                .build()
        def match2 = Match.builder()
                .home(team2)
                .away(team1)
                .build()
        def matches = [match1, match2]
        List<MatchDto> matchesDtos = []

        when:
        def result = stafferService.staffReferees(queue)

        then:
        result == matchesDtos
        match1.referee == ref2
        match2.referee == ref1
        1 * refereeService.getAvailableRefereesForQueue(queue) >> [ref1, ref2]
        1 * matchService.getMatchesToAssignInQueue(queue) >> matches
        1 * matchConverter.convertFromEntities(matches) >> matchesDtos
        3 * configurationRepository.findByName(ConfigName.AVERAGE_GRADE_MULTIPLIER) >> [value: gradeMultiplier]
        3 * configurationRepository.findByName(ConfigName.EXPERIENCE_MULTIPLIER) >> [value: expMultiplier]
        3 * configurationRepository.findByName(ConfigName.NUMBER_OF_MATCHES_MULTIPLIER) >> [value: noOfMatchesMultiplier]
        3 * configurationRepository.findByName(ConfigName.HOME_TEAM_REFEREED_MULTIPLIER) >> [value: homeTeamMultiplier]
        3 * configurationRepository.findByName(ConfigName.AWAY_TEAM_REFEREED_MULTIPLIER) >> [value: awayTeamMultiplier]

        where:
        gradeMultiplier | expMultiplier | noOfMatchesMultiplier | homeTeamMultiplier | awayTeamMultiplier
        1               | 0             | 0                     | 0                  | 0
        0               | 1             | 0                     | 0                  | 0
        0               | 0             | 1                     | 0                  | 0
        0               | 0             | 0                     | 1                  | 0
        0               | 0             | 0                     | 0                  | 1
        50              | 0.01          | 3                     | 1.3                | 1.3
    }
}
