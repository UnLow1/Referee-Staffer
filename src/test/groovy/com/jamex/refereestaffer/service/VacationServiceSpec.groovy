package com.jamex.refereestaffer.service

import com.jamex.refereestaffer.model.converter.VacationConverter
import com.jamex.refereestaffer.model.dto.VacationDto
import com.jamex.refereestaffer.model.entity.Referee
import com.jamex.refereestaffer.model.entity.Vacation
import com.jamex.refereestaffer.model.exception.RefereeNotFoundException
import com.jamex.refereestaffer.repository.RefereeRepository
import com.jamex.refereestaffer.repository.VacationRepository
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDate

class VacationServiceSpec extends Specification {

    @Subject
    VacationService vacationService

    VacationRepository vacationRepository = Mock()
    RefereeRepository refereeRepository = Mock()
    VacationConverter vacationConverter = Mock()

    def setup() {
        vacationService = new VacationService(vacationRepository, refereeRepository, vacationConverter)
    }

    def "should resolve the referee and save the vacation"() {
        given:
        def referee = Referee.builder().id(213l).build()
        def vacationDto = VacationDto.builder()
                .id(65l)
                .refereeId(referee.id)
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 7, 14))
                .build()
        def vacation = [] as Vacation
        def savedVacation = [] as Vacation
        def savedDto = VacationDto.builder().id(65l).refereeId(referee.id).build()

        when:
        def result = vacationService.saveVacation(vacationDto)

        then:
        1 * refereeRepository.findById(referee.id) >> Optional.of(referee)
        1 * vacationConverter.convertFromDto(vacationDto, referee) >> vacation
        1 * vacationRepository.save(vacation) >> savedVacation
        1 * vacationConverter.convertFromEntity(savedVacation) >> savedDto
        result == savedDto
    }

    def "should throw RefereeNotFoundException when referee has not been found"() {
        given:
        def refereeId = 213l
        def vacationDto = VacationDto.builder()
                .refereeId(refereeId)
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 7, 14))
                .build()

        when:
        vacationService.saveVacation(vacationDto)

        then:
        1 * refereeRepository.findById(refereeId) >> Optional.empty()
        def exception = thrown(RefereeNotFoundException)
        exception.message == String.format(RefereeNotFoundException.NOT_FOUND_WITH_ID, refereeId)
        0 * vacationConverter.convertFromDto(_, _)
        0 * vacationRepository.save(_)
    }
}
