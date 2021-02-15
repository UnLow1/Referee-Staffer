package com.jamex.refereestaffer.model.converter

import com.jamex.refereestaffer.model.dto.GradeDto
import com.jamex.refereestaffer.model.entity.Grade
import com.jamex.refereestaffer.model.entity.Match
import com.jamex.refereestaffer.model.exception.GradeNotFoundException
import com.jamex.refereestaffer.repository.GradeRepository
import spock.lang.Specification
import spock.lang.Subject

class GradeConverterSpec extends Specification {

    @Subject
    GradeConverter gradeConverter

    GradeRepository gradeRepository = Mock()

    def setup() {
        gradeConverter = new GradeConverter(gradeRepository)
    }

    def "should convert from Grade entity to dto"() {
        given:
        def entity = [getId: { 23l }, getValue: { 8.3 as double }] as Grade

        when:
        def result = gradeConverter.convertFromEntity(entity)

        then:
        result.id == entity.id
        result.value == entity.value
    }

    def "should throw GradeNotFoundException when Grade has not been found"() {
        given:
        def gradeDto = GradeDto.builder()
                .id(213l)
                .build()

        when:
        gradeConverter.convertFromDto(gradeDto)

        then:
        1 * gradeRepository.findById(gradeDto.id) >> Optional.empty()
        def exception = thrown(GradeNotFoundException)
        exception.message == String.format(GradeNotFoundException.NOT_FOUND, gradeDto.id)
    }

    def "should convert from dto to Grade entity"() {
        given:
        def match = [] as Match
        def grade = [getMatch: { match } ] as Grade
        def gradeDto = GradeDto.builder()
                .id(23l)
                .value(8.1 as double)
                .build()

        when:
        def result = gradeConverter.convertFromDto(gradeDto)

        then:
        1 * gradeRepository.findById(gradeDto.id) >> Optional.of(grade)
        result.id == gradeDto.id
        result.value == gradeDto.value
        result.match == grade.match
    }

    def "should convert from dto to Grade entity when id is null"() {
        given:
        def gradeDto = GradeDto.builder()
                .value(8.1 as double)
                .build()

        when:
        def result = gradeConverter.convertFromDto(gradeDto)

        then:
        0 * gradeRepository.findById(gradeDto.id)
        result.value == gradeDto.value
        result.id == null
        result.match == null
    }
}
