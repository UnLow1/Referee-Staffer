package com.jamex.refereestaffer.controller

import com.jamex.refereestaffer.model.converter.GradeConverter
import com.jamex.refereestaffer.model.dto.GradeDto
import com.jamex.refereestaffer.model.entity.Grade
import com.jamex.refereestaffer.model.exception.GradeNotFoundException
import com.jamex.refereestaffer.model.request.IDRequest
import com.jamex.refereestaffer.repository.GradeRepository
import com.jamex.refereestaffer.service.GradeService
import spock.lang.Specification
import spock.lang.Subject

class GradeControllerSpec extends Specification {

    @Subject
    GradeController gradeController

    GradeRepository gradeRepository = Mock()
    GradeConverter gradeConverter = Mock()
    GradeService gradeService = Mock()

    def setup() {
        gradeController = new GradeController(gradeRepository, gradeConverter, gradeService)
    }

    def "should return grades"() {
        given:
        def grades = [[] as Grade, [] as Grade]
        def gradesDtos = [GradeDto.builder().build(), GradeDto.builder().build()]

        when:
        def result = gradeController.getGrades()

        then:
        1 * gradeRepository.findAll() >> grades
        1 * gradeConverter.convertFromEntities(grades) >> gradesDtos
        result == gradesDtos
    }

    def "should throw GradeNotFoundException when grade not found"() {
        given:
        int gradeId = 12321l

        when:
        gradeController.getGrade(gradeId)

        then:
        def exception = thrown(GradeNotFoundException)
        exception.message == String.format(GradeNotFoundException.NOT_FOUND, gradeId)
        1 * gradeRepository.findById(gradeId) >> Optional.empty()
    }

    def "should return grade"() {
        given:
        int gradeId = 12321l
        def grade = [] as Grade
        def gradeDto = GradeDto.builder().build()

        when:
        def result = gradeController.getGrade(gradeId)

        then:
        1 * gradeRepository.findById(gradeId) >> Optional.of(grade)
        1 * gradeConverter.convertFromEntity(grade) >> gradeDto
        result == gradeDto
    }

    def "should add grade"() {
        given:
        def gradeDto = GradeDto.builder().build()
        def matchId = 4123213l

        when:
        gradeController.createGrade(gradeDto, matchId)

        then:
        1 * gradeService.addGrade(gradeDto, matchId)
    }

    def "should update grade"() {
        given:
        def gradeDto = GradeDto.builder().build()

        when:
        gradeController.updateGrade(gradeDto)

        then:
        1 * gradeService.updateGrade(gradeDto)
    }

    def "should return grades by IDs"() {
        given:
        def idsList = [123l, 555l]
        def request = [getIds: { idsList }] as IDRequest
        def grades = [[] as Grade, [] as Grade]
        def gradesDtos = [GradeDto.builder().build(), GradeDto.builder().build()]

        when:
        def result = gradeController.getGradesByIds(request)

        then:
        1 * gradeRepository.findAllById(idsList) >> grades
        1 * gradeConverter.convertFromEntities(grades) >> gradesDtos
        result == gradesDtos
    }

    def "should delete all grades"() {
        when:
        gradeController.deleteAll()

        then:
        1 * gradeRepository.deleteAll()
    }

    def "should delete grade with provided id"() {
        given:
        def gradeId = 12321l

        when:
        gradeController.deleteGrade(gradeId)

        then:
        1 * gradeRepository.deleteById(gradeId)
    }
}
