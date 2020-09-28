package com.jamex.refereestaffer.model.converter

import com.jamex.refereestaffer.model.dto.RefereeDto
import com.jamex.refereestaffer.model.entity.Referee
import spock.lang.Specification
import spock.lang.Subject

class RefereeConverterSpec extends Specification {

    @Subject
    RefereeConverter refereeConverter = new RefereeConverter()

    def "should convert from Referee entity to dto"() {
        given:
        def referee = Referee.builder()
                .id(23l)
                .firstName("John")
                .lastName("Smith")
                .email("john.smith@jamex.com")
                .experience(232)
                .build()

        when:
        def result = refereeConverter.convertFromEntity(referee)

        then:
        result.id == referee.id
        result.firstName == referee.firstName
        result.lastName == referee.lastName
        result.email == referee.email
        result.experience == referee.experience
    }

    def "should convert from dto to Referee entity"() {
        given:
        def refereeDto = RefereeDto.builder()
                .id(45l)
                .firstName("John")
                .lastName("Smith")
                .email("john.smith@jamex.com")
                .experience(55)
                .build()

        when:
        def result = refereeConverter.convertFromDto(refereeDto)

        then:
        result.id == refereeDto.id
        result.firstName == refereeDto.firstName
        result.lastName == refereeDto.lastName
        result.email == refereeDto.email
        result.experience == refereeDto.experience
    }
}
