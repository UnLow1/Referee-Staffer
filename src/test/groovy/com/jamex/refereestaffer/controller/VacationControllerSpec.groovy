package com.jamex.refereestaffer.controller

import com.jamex.refereestaffer.model.converter.VacationConverter
import com.jamex.refereestaffer.model.dto.VacationDto
import com.jamex.refereestaffer.model.entity.Vacation
import com.jamex.refereestaffer.model.exception.VacationNotFoundException
import com.jamex.refereestaffer.repository.VacationRepository
import groovy.json.JsonSlurper
import org.spockframework.runtime.model.parallel.ExecutionMode
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Execution
import spock.lang.Specification

import java.time.LocalDate

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put

// Features must run on one thread: the @SpringBean mocks live in the shared Spring
// context, so concurrent features would attach/stub the same mock instances at once.
@Execution(ExecutionMode.SAME_THREAD)
@WebMvcTest(VacationController)
class VacationControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @SpringBean
    VacationRepository vacationRepository = Mock()

    @SpringBean
    VacationConverter vacationConverter = Mock()

    def "should return vacations"() {
        given:
        def vacations = [[] as Vacation, [] as Vacation]
        def vacationsDtos = [VacationDto.builder().id(1l).build(), VacationDto.builder().id(2l).build()]

        when:
        def response = mockMvc.perform(get("/api/vacations")).andReturn().response

        then:
        1 * vacationRepository.findAll() >> vacations
        1 * vacationConverter.convertFromEntities(vacations) >> vacationsDtos
        response.status == 200
        def json = new JsonSlurper().parseText(response.contentAsString)
        json*.id == [1, 2]
    }

    def "should return vacation as JSON with ISO dates"() {
        given:
        def vacationId = 9l
        def vacation = [] as Vacation
        def vacationDto = VacationDto.builder()
                .id(vacationId)
                .refereeId(3l)
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 7, 14))
                .build()

        when:
        def response = mockMvc.perform(get("/api/vacations/$vacationId")).andReturn().response

        then:
        1 * vacationRepository.findById(vacationId) >> Optional.of(vacation)
        1 * vacationConverter.convertFromEntity(vacation) >> vacationDto
        response.status == 200
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.id == vacationId
        json.refereeId == 3
        json.startDate == "2026-07-01"
        json.endDate == "2026-07-14"
    }

    def "should respond 404 with problem detail when vacation has not been found"() {
        given:
        def vacationId = 9l

        when:
        def response = mockMvc.perform(get("/api/vacations/$vacationId")).andReturn().response

        then:
        1 * vacationRepository.findById(vacationId) >> Optional.empty()
        response.status == 404
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.detail == String.format(VacationNotFoundException.NOT_FOUND, vacationId)
    }

    def "should create vacation and return it as JSON"() {
        given:
        def vacation = [] as Vacation
        def savedVacation = [] as Vacation
        def savedDto = VacationDto.builder().id(10l).refereeId(3l).build()

        when:
        def response = mockMvc.perform(post("/api/vacations")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"refereeId": 3, "startDate": "2026-07-01", "endDate": "2026-07-14"}'))
                .andReturn().response

        then:
        1 * vacationConverter.convertFromDto({ VacationDto dto ->
            dto.refereeId == 3l && dto.startDate == LocalDate.of(2026, 7, 1) && dto.endDate == LocalDate.of(2026, 7, 14)
        }) >> vacation
        1 * vacationRepository.save(vacation) >> savedVacation
        1 * vacationConverter.convertFromEntity(savedVacation) >> savedDto
        response.status == 200
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.id == 10
    }

    def "should update vacation"() {
        given:
        def vacation = [] as Vacation
        def updatedVacation = [] as Vacation
        def updatedDto = VacationDto.builder().id(9l).build()

        when:
        def response = mockMvc.perform(put("/api/vacations")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"id": 9, "refereeId": 3, "startDate": "2026-07-02", "endDate": "2026-07-15"}'))
                .andReturn().response

        then:
        1 * vacationConverter.convertFromDto({ VacationDto dto -> dto.id == 9l }) >> vacation
        1 * vacationRepository.save(vacation) >> updatedVacation
        1 * vacationConverter.convertFromEntity(updatedVacation) >> updatedDto
        response.status == 200
    }

    def "should delete all vacations"() {
        when:
        def response = mockMvc.perform(delete("/api/vacations")).andReturn().response

        then:
        1 * vacationRepository.deleteAll()
        response.status == 200
    }

    def "should delete vacation with provided id"() {
        when:
        def response = mockMvc.perform(delete("/api/vacations/9")).andReturn().response

        then:
        1 * vacationRepository.deleteById(9l)
        response.status == 200
    }
}
