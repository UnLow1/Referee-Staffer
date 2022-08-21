package com.jamex.refereestaffer.controller

import com.jamex.refereestaffer.model.converter.VacationConverter
import com.jamex.refereestaffer.model.dto.VacationDto
import com.jamex.refereestaffer.model.entity.Vacation
import com.jamex.refereestaffer.model.exception.VacationNotFoundException
import com.jamex.refereestaffer.repository.VacationRepository
import spock.lang.Specification
import spock.lang.Subject

class VacationControllerSpec extends Specification {

    @Subject
    VacationController vacationController

    VacationRepository vacationRepository = Mock()
    VacationConverter vacationConverter = Mock()

    def setup() {
        vacationController = new VacationController(vacationRepository, vacationConverter)
    }

    def "should return vacations"() {
        given:
        def vacations = [[] as Vacation, [] as Vacation]
        def vacationsDtos = [VacationDto.builder().build(), VacationDto.builder().build()]

        when:
        def result = vacationController.getVacations()

        then:
        1 * vacationRepository.findAll() >> vacations
        1 * vacationConverter.convertFromEntities(vacations) >> vacationsDtos
        result == vacationsDtos
    }

    def "should throw VacationNotFoundException when vacation has not been found"() {
        given:
        def vacationId = 4421

        when:
        vacationController.getVacation(vacationId)

        then:
        1 * vacationRepository.findById(vacationId) >> Optional.empty()
        def exception = thrown(VacationNotFoundException)
        exception.message == String.format(VacationNotFoundException.NOT_FOUND, vacationId)
    }

    def "should return vacation"() {
        given:
        def vacationId = 12398l
        def vacation = [] as Vacation
        def vacationDto = VacationDto.builder().build()

        when:
        def result = vacationController.getVacation(vacationId)

        then:
        1 * vacationRepository.findById(vacationId) >> Optional.of(vacation)
        1 * vacationConverter.convertFromEntity(vacation) >> vacationDto
        result == vacationDto
    }

    def "should add vacation"() {
        given:
        def vacationDto = VacationDto.builder().build()
        def vacation = [] as Vacation

        when:
        vacationController.createVacation(vacationDto)

        then:
        1 * vacationConverter.convertFromDto(vacationDto) >> vacation
        1 * vacationRepository.save(vacation)
    }

    def "should update vacation"() {
        given:
        def vacationDto = VacationDto.builder().build()
        def vacation = [] as Vacation

        when:
        vacationController.updateVacation(vacationDto)

        then:
        1 * vacationConverter.convertFromDto(vacationDto) >> vacation
        1 * vacationRepository.save(vacation)
    }

    def "should delete all vacations"() {
        when:
        vacationController.deleteAll()

        then:
        1 * vacationRepository.deleteAll()
    }

    def "should delete vacation with provided id"() {
        given:
        def vacationId = 213l

        when:
        vacationController.deleteVacation(vacationId)

        then:
        1 * vacationRepository.deleteById(vacationId)
    }
}
