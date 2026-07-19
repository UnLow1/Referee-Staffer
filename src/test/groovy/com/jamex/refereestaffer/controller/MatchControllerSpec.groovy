package com.jamex.refereestaffer.controller

import com.jamex.refereestaffer.model.converter.MatchConverter
import com.jamex.refereestaffer.model.dto.DifficultyBreakdownDto
import com.jamex.refereestaffer.model.dto.MatchDto
import com.jamex.refereestaffer.model.entity.Match
import com.jamex.refereestaffer.model.exception.MatchNotFoundException
import com.jamex.refereestaffer.repository.MatchRepository
import com.jamex.refereestaffer.service.MatchService
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
@WebMvcTest(MatchController)
class MatchControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @SpringBean
    MatchRepository matchRepository = Mock()

    @SpringBean
    MatchConverter matchConverter = Mock()

    @SpringBean
    MatchService matchService = Mock()

    def "should return matches"() {
        given:
        def matches = [[] as Match, [] as Match]
        def matchesDtos = [MatchDto.builder().id(1l).queue(3 as Short).build(),
                           MatchDto.builder().id(2l).queue(4 as Short).build()]

        when:
        def response = mockMvc.perform(get("/api/matches")).andReturn().response

        then:
        1 * matchRepository.findAll() >> matches
        1 * matchConverter.convertFromEntities(matches) >> matchesDtos
        response.status == 200
        def json = new JsonSlurper().parseText(response.contentAsString)
        json*.id == [1, 2]
        json*.queue == [3, 4]
    }

    def "should return match as JSON"() {
        given:
        def matchId = 2396l
        def match = [] as Match
        def matchDto = MatchDto.builder()
                .id(matchId)
                .queue(7 as Short)
                .homeScore(2 as Short)
                .awayScore(1 as Short)
                .build()

        when:
        def response = mockMvc.perform(get("/api/matches/$matchId")).andReturn().response

        then:
        1 * matchRepository.findById(matchId) >> Optional.of(match)
        1 * matchConverter.convertFromEntity(match) >> matchDto
        response.status == 200
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.id == matchId
        json.homeScore == 2
        json.awayScore == 1
    }

    def "should respond 404 with problem detail when match has not been found"() {
        given:
        def matchId = 2396l

        when:
        def response = mockMvc.perform(get("/api/matches/$matchId")).andReturn().response

        then:
        1 * matchRepository.findById(matchId) >> Optional.empty()
        response.status == 404
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.detail == String.format(MatchNotFoundException.NOT_FOUND, matchId)
    }

    def "should return difficulty breakdown for match"() {
        given:
        def matchId = 5l
        def breakdown = new DifficultyBreakdownDto(matchId, 42.5d,
                new DifficultyBreakdownDto.Parts(40.0d, 2.5d, 0.0d, 0.0d),
                new DifficultyBreakdownDto.Flags(true, false, false, 3))

        when:
        def response = mockMvc.perform(get("/api/matches/$matchId/difficulty")).andReturn().response

        then:
        1 * matchService.computeDifficultyBreakdown(matchId) >> breakdown
        response.status == 200
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.matchId == matchId
        json.total == 42.5
        json.parts.base == 40.0
        json.parts.sameCity == 2.5
        json.flags.sameCity == true
        json.flags.pointsDiff == 3
    }

    def "should respond 404 when computing difficulty for a missing match"() {
        given:
        def matchId = 404l

        when:
        def response = mockMvc.perform(get("/api/matches/$matchId/difficulty")).andReturn().response

        then:
        1 * matchService.computeDifficultyBreakdown(matchId) >> { throw new MatchNotFoundException(matchId) }
        response.status == 404
    }

    def "should create match and return it as JSON"() {
        given:
        def match = [] as Match
        def savedMatch = [] as Match
        def savedDto = MatchDto.builder().id(11l).queue(2 as Short).build()

        when:
        def response = mockMvc.perform(post("/api/matches")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"queue": 2, "homeTeamId": 1, "awayTeamId": 2}'))
                .andReturn().response

        then:
        1 * matchConverter.convertFromDto({ MatchDto dto ->
            dto.queue == 2 as Short && dto.homeTeamId == 1l && dto.awayTeamId == 2l
        }) >> match
        1 * matchRepository.save(match) >> savedMatch
        1 * matchConverter.convertFromEntity(savedMatch) >> savedDto
        response.status == 200
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.id == 11
    }

    def "should update match"() {
        given:
        def match = [] as Match
        def updatedMatch = [] as Match
        def updatedDto = MatchDto.builder().id(11l).build()

        when:
        def response = mockMvc.perform(put("/api/matches/11")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"id": 11, "queue": 2, "homeTeamId": 1, "awayTeamId": 2}'))
                .andReturn().response

        then:
        1 * matchConverter.convertFromDto({ MatchDto dto -> dto.id == 11l }) >> match
        1 * matchRepository.save(match) >> updatedMatch
        1 * matchConverter.convertFromEntity(updatedMatch) >> updatedDto
        response.status == 200
    }

    def "should update matches in bulk"() {
        given:
        def matches = [[] as Match, [] as Match]

        when:
        def response = mockMvc.perform(put("/api/matches")
                .contentType(MediaType.APPLICATION_JSON)
                .content('[{"id": 1, "queue": 2, "homeTeamId": 1, "awayTeamId": 2}, {"id": 2, "queue": 2, "homeTeamId": 3, "awayTeamId": 4}]'))
                .andReturn().response

        then:
        1 * matchConverter.convertFromDtos({ List<MatchDto> dtos -> dtos*.id == [1l, 2l] }) >> matches
        1 * matchRepository.saveAll(matches)
        response.status == 200
    }

    def "should reject match creation when fixture fields are missing"() {
        when:
        def response = mockMvc.perform(post("/api/matches")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"queue": 2}'))
                .andReturn().response

        then:
        0 * matchConverter._
        0 * matchRepository._
        response.status == 400
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.detail == "awayTeamId: must not be null; homeTeamId: must not be null"
    }

    def "should reject match update without id"() {
        when:
        def response = mockMvc.perform(put("/api/matches/11")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"queue": 2, "homeTeamId": 1, "awayTeamId": 2}'))
                .andReturn().response

        then:
        0 * matchConverter._
        0 * matchRepository._
        response.status == 400
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.detail == "id: must not be null"
    }

    def "should reject bulk match update when an element is incomplete"() {
        when:
        def response = mockMvc.perform(put("/api/matches")
                .contentType(MediaType.APPLICATION_JSON)
                .content('[{"id": 1, "queue": 2, "homeTeamId": 1, "awayTeamId": 2}, {"id": 2}]'))
                .andReturn().response

        then:
        0 * matchConverter._
        0 * matchRepository._
        response.status == 400
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.detail == "[1].awayTeamId: must not be null; [1].homeTeamId: must not be null; [1].queue: must not be null"
    }

    def "should reject bulk match update when an element has no id"() {
        when:
        def response = mockMvc.perform(put("/api/matches")
                .contentType(MediaType.APPLICATION_JSON)
                .content('[{"id": 1, "queue": 2, "homeTeamId": 1, "awayTeamId": 2}, {"queue": 2, "homeTeamId": 3, "awayTeamId": 4}]'))
                .andReturn().response

        then:
        0 * matchConverter._
        0 * matchRepository._
        response.status == 400
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.detail == "[1].id: must not be null"
    }

    def "should delete all matches"() {
        when:
        def response = mockMvc.perform(delete("/api/matches")).andReturn().response

        then:
        1 * matchRepository.deleteAll()
        response.status == 200
    }

    def "should delete match through the service so its grade goes too"() {
        when:
        def response = mockMvc.perform(delete("/api/matches/2396")).andReturn().response

        then:
        1 * matchService.deleteMatch(2396l)
        response.status == 200
    }
}
