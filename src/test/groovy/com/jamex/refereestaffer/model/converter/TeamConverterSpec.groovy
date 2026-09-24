package com.jamex.refereestaffer.model.converter

import com.jamex.refereestaffer.model.dto.TeamDto
import com.jamex.refereestaffer.model.entity.Team
import spock.lang.Specification
import spock.lang.Subject

class TeamConverterSpec extends Specification {

    @Subject
    TeamConverter teamConverter = new TeamConverter()

    def "should convert from Team entity to dto with the name-derived short code fallback"() {
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
        result.shortCode == "KOR"
    }

    def "should convert the venue in both directions"() {
        given:
        def team = Team.builder()
                .id(65l)
                .name("Korona")
                .city("Kielce")
                .venueName("Suzuki Arena")
                .venueAddress("Kielce, Scegiennego 35")
                .build()

        when:
        def dto = teamConverter.convertFromEntity(team)

        then:
        dto.venueName == "Suzuki Arena"
        dto.venueAddress == "Kielce, Scegiennego 35"

        when: "the same payload comes back from the client"
        def entity = teamConverter.convertFromDto(dto)

        then: "unlike the short code, the venue is taken from the DTO"
        entity.venueName == "Suzuki Arena"
        entity.venueAddress == "Kielce, Scegiennego 35"
    }

    def "should convert a team without a venue to nulls rather than blanks"() {
        given: "a team the CSV importer created, plus a DTO with emptied inputs"
        def imported = Team.builder().name("Korona").build()
        def emptiedDto = TeamDto.builder()
                .name("Korona")
                .city("Kielce")
                .venueName("")
                .venueAddress("   ")
                .build()

        expect:
        teamConverter.convertFromEntity(imported).venueName == null
        teamConverter.convertFromEntity(imported).venueAddress == null
        teamConverter.convertFromDto(emptiedDto).venueName == null
        teamConverter.convertFromDto(emptiedDto).venueAddress == null
    }

    def "should convert from Team entity to dto with the stored short code override"() {
        given:
        def team = Team.builder()
                .name("Korona")
                .shortCode("KRN")
                .build()

        when:
        def result = teamConverter.convertFromEntity(team)

        then:
        result.shortCode == "KRN"
    }

    def "should convert from dto to Team entity ignoring the client-sent short code"() {
        given: "a short code that differs from the name-derived fallback"
        def teamDto = TeamDto.builder()
                .id(65l)
                .name("Korona")
                .city("Kielce")
                .shortCode("XXX")
                .build()

        when:
        def result = teamConverter.convertFromDto(teamDto)

        then:
        result.id == teamDto.id
        result.name == teamDto.name
        result.city == teamDto.city
        and: "the entity falls back to the name prefix, proving nothing was stored"
        result.shortCode == "KOR"

        and: "the entity starts unranked until standings are computed"
        result.place == null
    }
}
