package com.jamex.refereestaffer.model.converter

import com.jamex.refereestaffer.model.dto.GradeDto
import com.jamex.refereestaffer.model.entity.Grade
import com.jamex.refereestaffer.model.entity.Match
import spock.lang.Specification
import spock.lang.Subject

class GradeConverterSpec extends Specification {

    @Subject
    GradeConverter gradeConverter = new GradeConverter()

    def "should convert from Grade entity to dto"() {
        given:
        def entity = [getId: { 23l }, getValue: { 8.3 as double }] as Grade

        when:
        def result = gradeConverter.convertFromEntity(entity)

        then:
        result.id == entity.id
        result.value == entity.value
        result.secondValue == null
    }

    def "should convert split grade from entity to dto"() {
        given:
        def entity = [getId: { 23l }, getValue: { 7.9 as double }, getSecondValue: { 8.3 as double }] as Grade

        when:
        def result = gradeConverter.convertFromEntity(entity)

        then:
        result.id == entity.id
        result.value == entity.value
        result.secondValue == entity.secondValue
    }

    def "should convert a collection of entities to dtos"() {
        given:
        def first = [getId: { 1l }, getValue: { 8.0 as double }] as Grade
        def second = [getId: { 2l }, getValue: { 7.5 as double }] as Grade

        when:
        def result = gradeConverter.convertFromEntities([first, second])

        then:
        result*.id == [1l, 2l]
        result*.value == [8.0, 7.5]
    }

    def "should convert from dto to Grade entity using the match handed in by the service"() {
        given:
        def match = [] as Match
        def gradeDto = GradeDto.builder()
                .id(23l)
                .value(8.1 as double)
                .build()

        when:
        def result = gradeConverter.convertFromDto(gradeDto, match)

        then:
        result.id == gradeDto.id
        result.value == gradeDto.value
        result.secondValue == null
        result.match.is(match)
    }

    def "should convert split grade from dto to entity"() {
        given:
        def match = [] as Match
        def gradeDto = GradeDto.builder()
                .id(23l)
                .value(7.9 as double)
                .secondValue(8.3 as double)
                .build()

        when:
        def result = gradeConverter.convertFromDto(gradeDto, match)

        then:
        result.value == gradeDto.value
        result.secondValue == gradeDto.secondValue
        result.effectiveValue == (gradeDto.value + gradeDto.secondValue) / 2
    }

    def "should convert from dto to Grade entity when there is no match"() {
        given:
        def gradeDto = GradeDto.builder()
                .value(8.1 as double)
                .build()

        when:
        def result = gradeConverter.convertFromDto(gradeDto, null)

        then:
        result.value == gradeDto.value
        result.id == null
        result.match == null
    }
}
