package com.jamex.refereestaffer.model.converter

import com.jamex.refereestaffer.model.dto.MatchDto
import com.jamex.refereestaffer.model.entity.Grade
import com.jamex.refereestaffer.model.entity.Match
import com.jamex.refereestaffer.model.entity.Referee
import com.jamex.refereestaffer.model.entity.Team
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDateTime

class MatchConverterSpec extends Specification {

    @Subject
    MatchConverter matchConverter = new MatchConverter()

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

    def "should convert from dto and resolved entities to Match entity"() {
        given:
        def homeTeam = [] as Team
        def awayTeam = [] as Team
        def date = LocalDateTime.of(2026, 7, 12, 17, 0)
        def matchDto = MatchDto.builder()
                .id(23l)
                .queue(3 as short)
                .homeTeamId(222l)
                .awayTeamId(55l)
                .refereeId(7l)
                .gradeId(9l)
                .date(date)
                .homeScore(0 as short)
                .awayScore(1 as short)
                .build()

        when:
        def result = matchConverter.convertFromDto(matchDto, homeTeam, awayTeam, referee, grade)

        then:
        result.id == matchDto.id
        result.queue == matchDto.queue
        result.date == matchDto.date
        result.homeScore == matchDto.homeScore
        result.awayScore == matchDto.awayScore
        result.home == homeTeam
        result.away == awayTeam
        result.referee == referee
        result.grade == grade

        where:
        referee        | grade
        null           | null
        [] as Referee  | [] as Grade
    }
}
