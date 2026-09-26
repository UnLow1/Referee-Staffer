package com.jamex.refereestaffer.model.converter

import com.jamex.refereestaffer.model.dto.VacationDto
import com.jamex.refereestaffer.model.entity.Referee
import com.jamex.refereestaffer.model.entity.Vacation
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDate

class VacationConverterSpec extends Specification {

    @Subject
    VacationConverter vacationConverter = new VacationConverter()

    def "should convert from Vacation entity to dto"() {
        given:
        def vacation = Vacation.builder()
                .id(65l)
                .referee(Referee.builder().id(221l).build())
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now())
                .build()

        when:
        def result = vacationConverter.convertFromEntity(vacation)

        then:
        result.id == vacation.id
        result.refereeId == vacation.referee.id
        result.startDate == vacation.startDate
        result.endDate == vacation.endDate
    }

    def "should convert a collection of entities to dtos"() {
        given:
        def first = Vacation.builder().id(1l).referee(Referee.builder().id(11l).build()).build()
        def second = Vacation.builder().id(2l).referee(Referee.builder().id(12l).build()).build()

        when:
        def result = vacationConverter.convertFromEntities([first, second])

        then:
        result*.id == [1l, 2l]
        result*.refereeId == [11l, 12l]
    }

    def "should convert from dto to Vacation entity using the referee handed in by the service"() {
        given:
        def referee = Referee.builder().id(213l).build()
        def vacationDto = VacationDto.builder()
                .id(65l)
                .refereeId(referee.id)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now())
                .build()

        when:
        def result = vacationConverter.convertFromDto(vacationDto, referee)

        then:
        result.id == vacationDto.id
        result.referee.is(referee)
        result.startDate == vacationDto.startDate
        result.endDate == vacationDto.endDate
    }
}
