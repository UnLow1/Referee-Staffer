package com.jamex.refereestaffer.service

import com.jamex.refereestaffer.model.converter.GradeConverter
import com.jamex.refereestaffer.model.dto.GradeDto
import com.jamex.refereestaffer.model.entity.Grade
import com.jamex.refereestaffer.model.entity.Match
import com.jamex.refereestaffer.model.exception.GradeNotFoundException
import com.jamex.refereestaffer.model.exception.MatchNotFoundException
import com.jamex.refereestaffer.repository.GradeRepository
import com.jamex.refereestaffer.repository.MatchRepository
import spock.lang.Specification
import spock.lang.Subject

class GradeServiceSpec extends Specification {

    @Subject
    GradeService gradeService

    GradeRepository gradeRepository = Mock()
    GradeConverter gradeConverter = Mock()
    MatchRepository matchRepository = Mock()

    def setup() {
        gradeService = new GradeService(gradeRepository, gradeConverter, matchRepository)
    }

    def "should throw MatchNotFoundException when match not found"() {
        given:
        def gradeDto = GradeDto.builder().build()
        def matchId = 12363l

        when:
        gradeService.addGrade(gradeDto, matchId)

        then:
        def exception = thrown(MatchNotFoundException)
        exception.message == String.format(MatchNotFoundException.NOT_FOUND, matchId)
        1 * matchRepository.findById(matchId) >> Optional.empty()
        0 * gradeConverter.convertFromDto(_, _)
        0 * gradeRepository.save(_)
    }

    def "should add grade for the resolved match"() {
        given:
        def gradeDto = GradeDto.builder().value(8.1 as double).build()
        def matchId = 12363l
        def grade = [] as Grade
        def match = [] as Match

        when:
        gradeService.addGrade(gradeDto, matchId)

        then:
        1 * matchRepository.findById(matchId) >> Optional.of(match)
        1 * gradeConverter.convertFromDto(gradeDto, match) >> grade
        1 * gradeRepository.save(grade)
        // the match comes from this service, not from a lookup inside the converter
        0 * gradeRepository.findById(_)
    }

    def "should update grade keeping the match of the stored grade"() {
        given:
        def gradeDto = GradeDto.builder().id(23l).value(8.1 as double).build()
        def match = [] as Match
        def storedGrade = [getMatch: { match }] as Grade
        def convertedGrade = [] as Grade

        when:
        gradeService.updateGrade(gradeDto)

        then:
        1 * gradeRepository.findById(gradeDto.id) >> Optional.of(storedGrade)
        1 * gradeConverter.convertFromDto(gradeDto, match) >> convertedGrade
        1 * gradeRepository.save(convertedGrade)
    }

    def "should update grade that is not attached to any match"() {
        given:
        def gradeDto = GradeDto.builder().id(23l).value(8.1 as double).build()
        def storedGrade = [] as Grade
        def convertedGrade = [] as Grade

        when:
        gradeService.updateGrade(gradeDto)

        then:
        1 * gradeRepository.findById(gradeDto.id) >> Optional.of(storedGrade)
        1 * gradeConverter.convertFromDto(gradeDto, null) >> convertedGrade
        1 * gradeRepository.save(convertedGrade)
    }

    def "should throw GradeNotFoundException when updated grade does not exist"() {
        given:
        def gradeDto = GradeDto.builder().id(213l).value(8.1 as double).build()

        when:
        gradeService.updateGrade(gradeDto)

        then:
        1 * gradeRepository.findById(gradeDto.id) >> Optional.empty()
        def exception = thrown(GradeNotFoundException)
        exception.message == String.format(GradeNotFoundException.NOT_FOUND, gradeDto.id)
        0 * gradeConverter.convertFromDto(_, _)
        0 * gradeRepository.save(_)
    }
}
