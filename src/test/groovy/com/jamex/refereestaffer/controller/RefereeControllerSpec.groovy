package com.jamex.refereestaffer.controller

import com.jamex.refereestaffer.model.converter.RefereeConverter
import com.jamex.refereestaffer.model.dto.RefereeDto
import com.jamex.refereestaffer.model.entity.Referee
import com.jamex.refereestaffer.model.exception.RefereeNotFoundException
import com.jamex.refereestaffer.model.request.IDRequest
import com.jamex.refereestaffer.repository.RefereeRepository
import spock.lang.Specification
import spock.lang.Subject

class RefereeControllerSpec extends Specification {

    @Subject
    RefereeController refereeController

    RefereeRepository refereeRepository = Mock()
    RefereeConverter refereeConverter = Mock()

    def setup() {
        refereeController = new RefereeController(refereeRepository, refereeConverter)
    }

    def "should return referees"() {
        given:
        def referees = [[] as Referee, [] as Referee]
        def refereesDtos = [RefereeDto.builder().build(), RefereeDto.builder().build()]

        when:
        def result = refereeController.getReferees()

        then:
        1 * refereeRepository.findAll() >> referees
        1 * refereeConverter.convertFromEntities(referees) >> refereesDtos
        result == refereesDtos
    }

    def "should throw RefereeNotFoundException when referee has not been found"() {
        given:
        def refereeId = 231l

        when:
        refereeController.getReferee(refereeId)

        then:
        1 * refereeRepository.findById(refereeId) >> Optional.empty()
        def exception = thrown(RefereeNotFoundException)
        exception.message == String.format(RefereeNotFoundException.NOT_FOUND_WITH_ID, refereeId)
    }

    def "should return referee"() {
        given:
        def refereeId = 231l
        def referee = [] as Referee
        def refereeDto = RefereeDto.builder().build()

        when:
        def result = refereeController.getReferee(refereeId)

        then:
        1 * refereeRepository.findById(refereeId) >> Optional.of(referee)
        1 * refereeConverter.convertFromEntity(referee) >> refereeDto
        result == refereeDto
    }

    def "should update referee"() {
        given:
        def refereeDto = RefereeDto.builder().build()
        def referee = [] as Referee

        when:
        refereeController.updateReferee(refereeDto)

        then:
        1 * refereeConverter.convertFromDto(refereeDto) >> referee
        1 * refereeRepository.save(referee)
    }

    def "should add referee"() {
        given:
        def refereeDto = RefereeDto.builder().build()
        def referee = [] as Referee

        when:
        refereeController.addReferee(refereeDto)

        then:
        1 * refereeConverter.convertFromDto(refereeDto) >> referee
        1 * refereeRepository.save(referee)
    }

    def "should return referees by ids"() {
        given:
        def idsList = [223l, 554l]
        def request = [getIds: { idsList }] as IDRequest
        def referees = [[] as Referee, [] as Referee]
        def refereesDtos = [RefereeDto.builder().build(), RefereeDto.builder().build()]

        when:
        def result = refereeController.getRefereesByIds(request)

        then:
        1 * refereeRepository.findAllById(idsList) >> referees
        1 * refereeConverter.convertFromEntities(referees) >> refereesDtos
        result == refereesDtos
    }

    def "should delete all"() {
        when:
        refereeController.deleteAll()

        then:
        1 * refereeRepository.deleteAll()
    }

    def "should delete referee with provided id"() {
        given:
        def refereeId = 241l

        when:
        refereeController.deleteReferee(refereeId)

        then:
        1 * refereeRepository.deleteById(refereeId)
    }
}
