package com.jamex.refereestaffer.service

import com.jamex.refereestaffer.model.converter.GradeConverter
import com.jamex.refereestaffer.model.dto.GradeDto
import com.jamex.refereestaffer.model.entity.Grade
import com.jamex.refereestaffer.model.entity.Match
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
        0 * gradeRepository.save(_)
    }

    def "should add grade"() {
        given:
        def gradeDto = GradeDto.builder().build()
        def matchId = 12363l
        def grade = [] as Grade
        def match = [] as Match

        when:
        gradeService.addGrade(gradeDto, matchId)

        then:
        1 * gradeConverter.convertFromDto(gradeDto) >> grade
        1 * matchRepository.findById(matchId) >> Optional.of(match)
        1 * gradeRepository.save(grade)
        grade.match == match
    }

    def "should update grade"() {
        given:
        def gradeDto = GradeDto.builder().build()
        def grade = [] as Grade

        when:
        gradeService.updateGrade(gradeDto)

        then:
        1 * gradeConverter.convertFromDto(gradeDto) >> grade
        1 * gradeRepository.save(grade)
    }
}