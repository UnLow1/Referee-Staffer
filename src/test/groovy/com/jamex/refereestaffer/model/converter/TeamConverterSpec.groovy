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
        and: "no override is reported, so the form shows an empty field"
        result.shortOverride == null
    }

    def "should convert from Team entity to dto with the stored short code override"() {
        given:
        def team = Team.builder()
                .name("Korona")
                .shortCode("KRN")
                .build()

        when:
        def result = teamConverter.convertFromEntity(team)

        then: "the computed field serves the override"
        result.shortCode == "KRN"
        and: "and the writable field exposes it so the form can pre-fill and round-trip it"
        result.shortOverride == "KRN"
    }

    def "should convert from dto to Team entity ignoring the read-only short field"() {
        given: "a computed short code echoed back by the client, with no override set"
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
        and: "nothing was stored, so the entity falls back to the name prefix"
        result.shortCodeOverride == null
        result.shortCode == "KOR"

        and: "the entity starts unranked until standings are computed"
        result.place == null
    }

    def "should convert from dto to Team entity storing the short code override"() {
        given: "the writable field carries a code the computed one disagrees with"
        def teamDto = TeamDto.builder()
                .name("Korona")
                .city("Kielce")
                .shortCode("KOR")
                .shortOverride("krn")
                .build()

        when:
        def result = teamConverter.convertFromDto(teamDto)

        then: "the override wins and is normalised on the way in"
        result.shortCodeOverride == "KRN"
        result.shortCode == "KRN"
    }

    def "should convert from dto to Team entity treating a blank override as absent"() {
        given:
        def teamDto = TeamDto.builder()
                .name("Korona")
                .city("Kielce")
                .shortCode("KOR")
                .shortOverride(blank)
                .build()

        when:
        def result = teamConverter.convertFromDto(teamDto)

        then:
        result.shortCodeOverride == null
        result.shortCode == "KOR"

        where:
        blank << [null, "", "   "]
    }
}
