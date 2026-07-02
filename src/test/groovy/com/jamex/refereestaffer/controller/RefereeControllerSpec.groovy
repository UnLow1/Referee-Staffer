package com.jamex.refereestaffer.controller

import com.jamex.refereestaffer.model.converter.RefereeConverter
import com.jamex.refereestaffer.model.dto.RefereeDto
import com.jamex.refereestaffer.model.entity.Referee
import com.jamex.refereestaffer.model.exception.RefereeNotFoundException
import com.jamex.refereestaffer.repository.RefereeRepository
import com.jamex.refereestaffer.service.RefereeService
import groovy.json.JsonSlurper
import org.spockframework.runtime.model.parallel.ExecutionMode
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Execution
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put

// Features must run on one thread: the @SpringBean mocks live in the shared Spring
// context, so concurrent features would attach/stub the same mock instances at once.
@Execution(ExecutionMode.SAME_THREAD)
@WebMvcTest(RefereeController)
class RefereeControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @SpringBean
    RefereeRepository refereeRepository = Mock()

    @SpringBean
    RefereeConverter refereeConverter = Mock()

    @SpringBean
    RefereeService refereeService = Mock()

    def "should return referees enriched with stats"() {
        given:
        def referees = [[] as Referee, [] as Referee]
        def refereesDtos = [RefereeDto.builder().id(1l).firstName("John").build(),
                            RefereeDto.builder().id(2l).firstName("Affan").build()]

        when:
        def response = mockMvc.perform(get("/api/referees")).andReturn().response

        then:
        1 * refereeRepository.findAll() >> referees
        1 * refereeService.enrichWithStats(referees)
        1 * refereeConverter.convertFromEntities(referees) >> refereesDtos
        response.status == 200
        def json = new JsonSlurper().parseText(response.contentAsString)
        json*.firstName == ["John", "Affan"]
    }

    def "should return referee as JSON"() {
        given:
        def refereeId = 231l
        def referee = [] as Referee
        def refereeDto = RefereeDto.builder()
                .id(refereeId)
                .firstName("John")
                .lastName("Smith")
                .averageGrade(8.25d)
                .potential(41.7d)
                .build()

        when:
        def response = mockMvc.perform(get("/api/referees/$refereeId")).andReturn().response

        then:
        1 * refereeRepository.findById(refereeId) >> Optional.of(referee)
        1 * refereeService.enrichWithStats([referee])
        1 * refereeConverter.convertFromEntity(referee) >> refereeDto
        response.status == 200
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.id == refereeId
        json.firstName == "John"
        json.averageGrade == 8.25
        json.potential == 41.7
    }

    def "should respond 404 with problem detail when referee has not been found"() {
        given:
        def refereeId = 231l

        when:
        def response = mockMvc.perform(get("/api/referees/$refereeId")).andReturn().response

        then:
        1 * refereeRepository.findById(refereeId) >> Optional.empty()
        response.status == 404
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.detail == String.format(RefereeNotFoundException.NOT_FOUND_WITH_ID, refereeId)
    }

    def "should return referees available for given queue"() {
        given:
        def referees = [[] as Referee]
        def refereesDtos = [RefereeDto.builder().potential(39.5d).build()]

        when:
        def response = mockMvc.perform(get("/api/referees/available/21")).andReturn().response

        then:
        1 * refereeService.getAvailableRefereesForQueue(21 as Short) >> referees
        1 * refereeService.enrichWithStats(referees)
        1 * refereeConverter.convertFromEntities(referees) >> refereesDtos
        response.status == 200
        def json = new JsonSlurper().parseText(response.contentAsString)
        json[0].potential == 39.5
    }

    def "should add referee from JSON body"() {
        given:
        def referee = [] as Referee

        when:
        def response = mockMvc.perform(post("/api/referees")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"firstName": "John", "lastName": "Smith", "experience": 5}'))
                .andReturn().response

        then:
        1 * refereeConverter.convertFromDto({ RefereeDto dto ->
            dto.firstName == "John" && dto.lastName == "Smith" && dto.experience == 5
        }) >> referee
        1 * refereeRepository.save(referee)
        response.status == 200
    }

    def "should update referee from JSON body"() {
        given:
        def referee = [] as Referee

        when:
        def response = mockMvc.perform(put("/api/referees")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"id": 231, "firstName": "John"}'))
                .andReturn().response

        then:
        1 * refereeConverter.convertFromDto({ RefereeDto dto -> dto.id == 231l }) >> referee
        1 * refereeRepository.save(referee)
        response.status == 200
    }

    def "should return referees by ids"() {
        given:
        def referees = [[] as Referee, [] as Referee]
        def refereesDtos = [RefereeDto.builder().id(223l).build(), RefereeDto.builder().id(554l).build()]

        when:
        def response = mockMvc.perform(post("/api/referees/byIds")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"ids": [223, 554]}'))
                .andReturn().response

        then:
        1 * refereeRepository.findAllById([223l, 554l]) >> referees
        1 * refereeConverter.convertFromEntities(referees) >> refereesDtos
        response.status == 200
        def json = new JsonSlurper().parseText(response.contentAsString)
        json*.id == [223, 554]
    }

    def "should delete all referees"() {
        when:
        def response = mockMvc.perform(delete("/api/referees")).andReturn().response

        then:
        1 * refereeRepository.deleteAll()
        response.status == 200
    }

    def "should delete referee with provided id"() {
        when:
        def response = mockMvc.perform(delete("/api/referees/241")).andReturn().response

        then:
        1 * refereeRepository.deleteById(241l)
        response.status == 200
    }
}
