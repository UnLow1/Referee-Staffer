package com.jamex.refereestaffer.controller

import com.jamex.refereestaffer.model.converter.MatchConverter
import com.jamex.refereestaffer.model.dto.MatchDto
import com.jamex.refereestaffer.model.entity.Match
import com.jamex.refereestaffer.model.exception.MatchNotFoundException
import com.jamex.refereestaffer.repository.MatchRepository
import com.jamex.refereestaffer.service.MatchService
import spock.lang.Specification
import spock.lang.Subject

class MatchControllerSpec extends Specification {

    @Subject
    MatchController matchController

    MatchRepository matchRepository = Mock()
    MatchConverter matchConverter = Mock()
    MatchService matchService = Mock()

    def setup() {
        matchController = new MatchController(matchRepository, matchConverter, matchService)
    }

    def "should return matches"() {
        given:
        def matches = [[] as Match, [] as Match]
        def matchesDtos = [MatchDto.builder().build(), MatchDto.builder().build()]

        when:
        def result = matchController.getMatches()

        then:
        1 * matchRepository.findAll() >> matches
        1 * matchConverter.convertFromEntities(matches) >> matchesDtos
        result == matchesDtos
    }

    def "should throw MatchNotFoundException when match has not been found"() {
        given:
        def matchId = 12321l

        when:
        matchController.getMatch(matchId)

        then:
        1 * matchRepository.findById(matchId) >> Optional.empty()
        def exception = thrown(MatchNotFoundException)
        exception.message == String.format(MatchNotFoundException.NOT_FOUND, matchId)
    }

    def "should return match"() {
        given:
        def matchId = 12321l
        def match = [] as Match
        def matchDto = MatchDto.builder().build()

        when:
        def result = matchController.getMatch(matchId)

        then:
        1 * matchRepository.findById(matchId) >> Optional.of(match)
        1 * matchConverter.convertFromEntity(match) >> matchDto
        result == matchDto
    }

    def "should add match"() {
        given:
        def matchDto = MatchDto.builder().build()
        def matchDtoResult = MatchDto.builder().build()
        def match = [] as Match
        def savedMatch = [] as Match

        when:
        def result = matchController.createMatch(matchDto)

        then:
        1 * matchConverter.convertFromDto(matchDto) >> match
        1 * matchRepository.save(match) >> savedMatch
        1 * matchConverter.convertFromEntity(savedMatch) >> matchDtoResult
        result == matchDtoResult
    }

    def "should update match"() {
        given:
        def matchId = 3899l
        def matchDto = MatchDto.builder().build()
        def updatedMatchDto = MatchDto.builder().build()
        def match = [] as Match
        def updatedMatch = [] as Match

        when:
        def result = matchController.updateMatch(matchDto, matchId)

        then:
        1 * matchConverter.convertFromDto(matchDto) >> match
        1 * matchRepository.save(match) >> updatedMatch
        1 * matchConverter.convertFromEntity(updatedMatch) >> updatedMatchDto
        result == updatedMatchDto
    }

    def "should update matches"() {
        given:
        def matchesDtos = [MatchDto.builder().id(2139l).build(),
                           MatchDto.builder().id(442l).build()]
        def matches = [[] as Match, [] as Match]

        when:
        matchController.updateMatches(matchesDtos)

        then:
        1 * matchConverter.convertFromDtos(matchesDtos) >> matches
        1 * matchRepository.saveAll(matches)
    }

    def "should delete all"() {
        when:
        matchController.deleteAll()

        then:
        1 * matchRepository.deleteAll()
    }

    def "should delete match with provided id"() {
        given:
        def matchId = 12376l

        when:
        matchController.deleteMatch(matchId)

        then:
        matchService.deleteMatch(matchId)
    }
}
