package com.jamex.refereestaffer.service

import com.jamex.refereestaffer.model.converter.MatchConverter
import com.jamex.refereestaffer.model.dto.MatchDto
import com.jamex.refereestaffer.model.entity.*
import com.jamex.refereestaffer.repository.ConfigurationRepository
import com.jamex.refereestaffer.repository.VacationRepository
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDateTime

class StafferServiceSpec extends Specification {

    @Subject
    StafferService stafferService

    ConfigurationRepository configurationRepository = Mock()
    VacationRepository vacationRepository = Mock()
    MatchConverter matchConverter = Mock()
    MatchService matchService = Mock()
    RefereeService refereeService = Mock()

    def setup() {
        stafferService = new StafferService(configurationRepository, vacationRepository, matchConverter, matchService, refereeService)
    }

    def "should assign referees to matches in queue"() {
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
        2 * vacationRepository.findAllByStartDateIsLessThanEqualAndEndDateIsGreaterThanEqual(_) >> []
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

    def "should not assign referees to matches if referee has vacation"() {
        given:
        def ref1 = [averageGrade: 8.6] as Referee
        def ref2 = [teamsRefereed: [:], numberOfMatchesInRound: 0] as Referee
        def ref3 = [teamsRefereed: [:], numberOfMatchesInRound: 0] as Referee
        def ref4 = [averageGrade: 8.6] as Referee
        def ref5 = [averageGrade: 8.6] as Referee
        def ref6 = [averageGrade: 8.6] as Referee
        def referees = [ref1, ref2, ref3, ref4, ref5, ref6]
        def matchDateTime = LocalDateTime.of(2022, 10, 12, 16, 0)
        def matchDate = matchDateTime.toLocalDate()
        def match1 = [date: matchDateTime] as Match
        def match2 = [date: matchDateTime] as Match
        def matches = [match1, match2]
        def futureDate = matchDate.plusDays(1)
        def pastDate = matchDate.minusDays(1)
        def vacations = [
                Vacation.builder().referee(ref1).startDate(matchDate).endDate(matchDate).build(),
                Vacation.builder().referee(ref4).startDate(matchDate).endDate(futureDate).build(),
                Vacation.builder().referee(ref5).startDate(pastDate).endDate(matchDate).build(),
                Vacation.builder().referee(ref6).startDate(pastDate).endDate(futureDate).build()]

        when:
        stafferService.staffReferees(2 as short)

        then:
        match1.referee == ref2 || match1.referee == ref3
        match2.referee == ref2 || match2.referee == ref3
        2 * vacationRepository.findAllByStartDateIsLessThanEqualAndEndDateIsGreaterThanEqual(matchDateTime) >> vacations
        1 * refereeService.getAvailableRefereesForQueue(_) >> referees
        1 * refereeService.calculateStats(referees)
        1 * matchService.getMatchesToAssignInQueue(_) >> matches
        1 * matchConverter.convertFromEntities(matches)
        15 * configurationRepository.findByName(_) >> [value: 1]
    }
}
