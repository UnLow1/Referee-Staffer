package com.jamex.refereestaffer.model.converter

import com.jamex.refereestaffer.model.dto.TeamDto
import com.jamex.refereestaffer.model.entity.Team
import spock.lang.Specification
import spock.lang.Subject

class TeamConverterSpec extends Specification {

    @Subject
    TeamConverter teamConverter = new TeamConverter()

    def "should convert from Team entity to dto"() {
        given:
        def team = Team.builder()
                .id(65l)
                .name("Korona")
                .city("Kielce")
                .points(54 as short)
                .build()

        when:
        def result = teamConverter.convertFromEntity(team)

        then:
        result.id == team.id
        result.name == team.name
        result.city == team.city
        result.points == team.points
    }

    def "should convert from dto to Team entity"() {
        given:
        def teamDto = TeamDto.builder()
                .id(65l)
                .name("Korona")
                .city("Kielce")
                .build()

        when:
        def result = teamConverter.convertFromDto(teamDto)

        then:
        result.id == teamDto.id
        result.name == teamDto.name
        result.city == teamDto.city
    }
}
