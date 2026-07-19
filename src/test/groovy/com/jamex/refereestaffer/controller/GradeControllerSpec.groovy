package com.jamex.refereestaffer.controller

import com.jamex.refereestaffer.model.converter.GradeConverter
import com.jamex.refereestaffer.model.dto.GradeDto
import com.jamex.refereestaffer.model.entity.Grade
import com.jamex.refereestaffer.model.exception.GradeNotFoundException
import com.jamex.refereestaffer.repository.GradeRepository
import com.jamex.refereestaffer.service.GradeService
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
@WebMvcTest(GradeController)
class GradeControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @SpringBean
    GradeRepository gradeRepository = Mock()

    @SpringBean
    GradeConverter gradeConverter = Mock()

    @SpringBean
    GradeService gradeService = Mock()

    def "should return grades"() {
        given:
        def grades = [[] as Grade, [] as Grade]
        def gradesDtos = [GradeDto.builder().id(1l).value(8.5d).build(),
                          GradeDto.builder().id(2l).value(7.9d).build()]

        when:
        def response = mockMvc.perform(get("/api/grades")).andReturn().response

        then:
        1 * gradeRepository.findAll() >> grades
        1 * gradeConverter.convertFromEntities(grades) >> gradesDtos
        response.status == 200
        def json = new JsonSlurper().parseText(response.contentAsString)
        json*.value == [8.5, 7.9]
    }

    def "should return grade as JSON"() {
        given:
        def gradeId = 77l
        def grade = [] as Grade
        def gradeDto = GradeDto.builder().id(gradeId).value(8.5d).build()

        when:
        def response = mockMvc.perform(get("/api/grades/$gradeId")).andReturn().response

        then:
        1 * gradeRepository.findById(gradeId) >> Optional.of(grade)
        1 * gradeConverter.convertFromEntity(grade) >> gradeDto
        response.status == 200
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.id == gradeId
        json.value == 8.5
    }

    def "should return split grade as JSON"() {
        given:
        def gradeId = 77l
        def grade = [] as Grade
        def gradeDto = GradeDto.builder().id(gradeId).value(7.9d).secondValue(8.3d).build()

        when:
        def response = mockMvc.perform(get("/api/grades/$gradeId")).andReturn().response

        then:
        1 * gradeRepository.findById(gradeId) >> Optional.of(grade)
        1 * gradeConverter.convertFromEntity(grade) >> gradeDto
        response.status == 200
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.id == gradeId
        json.value == 7.9
        json.secondValue == 8.3
    }

    def "should respond 404 with problem detail when grade has not been found"() {
        given:
        def gradeId = 77l

        when:
        def response = mockMvc.perform(get("/api/grades/$gradeId")).andReturn().response

        then:
        1 * gradeRepository.findById(gradeId) >> Optional.empty()
        response.status == 404
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.detail == String.format(GradeNotFoundException.NOT_FOUND, gradeId)
    }

    def "should add grade for match"() {
        when:
        def response = mockMvc.perform(post("/api/grades/2396")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"value": 8.5}'))
                .andReturn().response

        then:
        1 * gradeService.addGrade({ GradeDto dto -> dto.value == 8.5d }, 2396l)
        response.status == 200
    }

    def "should add split grade for match"() {
        when:
        def response = mockMvc.perform(post("/api/grades/2396")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"value": 7.9, "secondValue": 8.3}'))
                .andReturn().response

        then:
        1 * gradeService.addGrade({ GradeDto dto -> dto.value == 7.9d && dto.secondValue == 8.3d }, 2396l)
        response.status == 200
    }

    def "should update grade"() {
        when:
        def response = mockMvc.perform(put("/api/grades")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"id": 77, "value": 9.0}'))
                .andReturn().response

        then:
        1 * gradeService.updateGrade({ GradeDto dto -> dto.id == 77l && dto.value == 9.0d })
        response.status == 200
    }

    def "should reject grade creation without value"() {
        when:
        def response = mockMvc.perform(post("/api/grades/2396")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{}'))
                .andReturn().response

        then:
        0 * gradeService._
        response.status == 400
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.detail == "value: must not be null"
    }

    def "should reject grade update without id"() {
        when:
        def response = mockMvc.perform(put("/api/grades")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"value": 9.0}'))
                .andReturn().response

        then:
        0 * gradeService._
        response.status == 400
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.detail == "id: must not be null"
    }

    def "should return grades by ids"() {
        given:
        def grades = [[] as Grade]
        def gradesDtos = [GradeDto.builder().id(3l).build()]

        when:
        def response = mockMvc.perform(post("/api/grades/byIds")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"ids": [3]}'))
                .andReturn().response

        then:
        1 * gradeRepository.findAllById([3l]) >> grades
        1 * gradeConverter.convertFromEntities(grades) >> gradesDtos
        response.status == 200
        def json = new JsonSlurper().parseText(response.contentAsString)
        json*.id == [3]
    }

    def "should delete all grades"() {
        when:
        def response = mockMvc.perform(delete("/api/grades")).andReturn().response

        then:
        1 * gradeRepository.deleteAll()
        response.status == 200
    }

    def "should delete grade with provided id"() {
        when:
        def response = mockMvc.perform(delete("/api/grades/77")).andReturn().response

        then:
        1 * gradeRepository.deleteById(77l)
        response.status == 200
    }
}
