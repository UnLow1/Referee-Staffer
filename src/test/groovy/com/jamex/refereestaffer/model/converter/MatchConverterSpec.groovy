package com.jamex.refereestaffer.model.converter

import com.jamex.refereestaffer.model.dto.MatchDto
import com.jamex.refereestaffer.model.entity.Grade
import com.jamex.refereestaffer.model.entity.Match
import com.jamex.refereestaffer.model.entity.Referee
import com.jamex.refereestaffer.model.entity.Team
import com.jamex.refereestaffer.model.exception.TeamNotFoundException
import com.jamex.refereestaffer.repository.GradeRepository
import com.jamex.refereestaffer.repository.RefereeRepository
import com.jamex.refereestaffer.repository.TeamRepository
import spock.lang.Specification
import spock.lang.Subject

class MatchConverterSpec extends Specification {

    @Subject
    MatchConverter matchConverter

    TeamRepository teamRepository = Mock()
    RefereeRepository refereeRepository = Mock()
    GradeRepository gradeRepository = Mock()

    def setup() {
        matchConverter = new MatchConverter(teamRepository, refereeRepository, gradeRepository)
    }

    def "should convert from Match entity to dto"() {
        given:
        def homeTeam = [getId: 33l] as Team
        def awayTeam = [getId: 35l] as Team
        def entity = [getId       : { 42l }, getReferee: { referee }, getGrade: { grade }, getQueue: { 2 as short },
                      getHomeScore: { 2 as short }, getAwayScore: { 3 as short }, getHome: { homeTeam }, getAway: { awayTeam }] as Match

        when:
        def result = matchConverter.convertFromEntity(entity)

        then:
        result.id == entity.id
        result.queue == entity.queue
        result.homeTeamId == entity.home.id
        result.awayTeamId == entity.away.id
        result.homeScore == entity.homeScore
        result.awayScore == entity.awayScore
        if (referee)
            assert result.refereeId == referee.id
        else
            assert result.refereeId == null
        if (grade)
            assert result.gradeId == grade.id
        else
            assert result.gradeId == null

        where:
        referee                     | grade
        null                        | null
        [getId: { 11l }] as Referee | [getId: { 22l }] as Grade
    }

    def "should throw TeamNotFoundException when home or away team has not been found"() {
        given:
        def correctTeamId = 1l
        def wrongTeamId = 987l
        def matchDto = MatchDto.builder()
                .homeTeamId(homeTeamId)
                .awayTeamId(awayTeamId)
                .build()

        when:
        matchConverter.convertFromDto(matchDto)

        then:
        (0..1) * teamRepository.findById(correctTeamId) >> Optional.of([] as Team)
        1 * teamRepository.findById(wrongTeamId) >> Optional.empty()
        def exception = thrown(TeamNotFoundException)
        exception.message == String.format(TeamNotFoundException.NOT_FOUND_WITH_ID, wrongTeamId)

        where:
        homeTeamId | awayTeamId
        1l         | 987l
        987l       | 1l
    }

    def "should convert from dto to Match entity"() {
        given:
        def homeTeam = [] as Team
        def awayTeam = [] as Team
        def referee = [] as Referee
        def grade = [] as Grade
        def matchDto = MatchDto.builder()
                .id(23l)
                .queue(3 as short)
                .homeTeamId(222l)
                .awayTeamId(55l)
                .refereeId(7l)
                .gradeId(9l)
                .homeScore(0 as short)
                .awayScore(1 as short)
                .build()

        when:
        def result = matchConverter.convertFromDto(matchDto)

        then:
        1 * teamRepository.findById(matchDto.homeTeamId) >> Optional.of(homeTeam)
        1 * teamRepository.findById(matchDto.awayTeamId) >> Optional.of(awayTeam)
        1 * refereeRepository.findById(matchDto.refereeId) >> Optional.of(referee)
        1 * gradeRepository.findById(matchDto.gradeId) >> Optional.of(grade)
        result.id == matchDto.id
        result.queue == matchDto.queue
        result.homeScore == matchDto.homeScore
        result.awayScore == matchDto.awayScore
        result.home == homeTeam
        result.away == awayTeam
        result.referee == referee
        result.grade == grade
    }
}
