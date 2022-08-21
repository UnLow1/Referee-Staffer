package com.jamex.refereestaffer.model.converter

import com.jamex.refereestaffer.model.dto.VacationDto
import com.jamex.refereestaffer.model.entity.Referee
import com.jamex.refereestaffer.model.entity.Vacation
import com.jamex.refereestaffer.repository.RefereeRepository
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDate

class VacationConverterSpec extends Specification {

    @Subject
    VacationConverter vacationConverter

    RefereeRepository refereeRepository = Mock()

    def setup() {
        vacationConverter = new VacationConverter(refereeRepository)
    }

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

    def "should convert from dto to Vacation entity"() {
        given:
        def refereeId = 213l
        def referee = Referee.builder().id(refereeId).build()
        def vacationDto = VacationDto.builder()
                .id(65l)
                .refereeId(refereeId)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now())
                .build()

        when:
        def result = vacationConverter.convertFromDto(vacationDto)

        then:
        1 * refereeRepository.findById(refereeId) >> Optional.of(referee)
        result.id == vacationDto.id
        result.referee.id == vacationDto.refereeId
        result.startDate == vacationDto.startDate
        result.endDate == vacationDto.endDate
    }
}
