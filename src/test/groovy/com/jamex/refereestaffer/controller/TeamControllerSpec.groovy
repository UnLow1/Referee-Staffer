package com.jamex.refereestaffer.controller

import com.jamex.refereestaffer.model.converter.TeamConverter
import com.jamex.refereestaffer.model.dto.TeamDto
import com.jamex.refereestaffer.model.entity.Team
import com.jamex.refereestaffer.model.exception.TeamNotFoundException
import com.jamex.refereestaffer.repository.TeamRepository
import com.jamex.refereestaffer.service.TeamService
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
@WebMvcTest(TeamController)
class TeamControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @SpringBean
    TeamService teamService = Mock()

    @SpringBean
    TeamRepository teamRepository = Mock()

    @SpringBean
    TeamConverter teamConverter = Mock()

    def "should return teams"() {
        given:
        def teams = [[] as Team, [] as Team]
        def teamsDtos = [TeamDto.builder().id(1l).name("Legia").build(),
                         TeamDto.builder().id(2l).name("Wisla").build()]

        when:
        def response = mockMvc.perform(get("/api/teams")).andReturn().response

        then:
        1 * teamRepository.findAll() >> teams
        1 * teamConverter.convertFromEntities(teams) >> teamsDtos
        response.status == 200
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.size() == 2
        json*.name == ["Legia", "Wisla"]
    }

    def "should return team as JSON with the wire-format short field"() {
        given:
        def teamId = 12398l
        def team = [] as Team
        def teamDto = TeamDto.builder().id(teamId).name("Legia").city("Warszawa").shortCode("LGW").build()

        when:
        def response = mockMvc.perform(get("/api/teams/$teamId")).andReturn().response

        then:
        1 * teamRepository.findById(teamId) >> Optional.of(team)
        1 * teamConverter.convertFromEntity(team) >> teamDto
        response.status == 200
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.id == teamId
        json.name == "Legia"
        json.short == "LGW"
    }

    def "should respond 404 with problem detail when team has not been found"() {
        given:
        def teamId = 12398l

        when:
        def response = mockMvc.perform(get("/api/teams/$teamId")).andReturn().response

        then:
        1 * teamRepository.findById(teamId) >> Optional.empty()
        response.status == 404
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.detail == String.format(TeamNotFoundException.NOT_FOUND_WITH_ID, teamId)
    }

    def "should add team from JSON body"() {
        given:
        def team = [] as Team

        when:
        def response = mockMvc.perform(post("/api/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"name": "Legia", "city": "Warszawa"}'))
                .andReturn().response

        then:
        1 * teamConverter.convertFromDto({ TeamDto dto -> dto.name == "Legia" && dto.city == "Warszawa" }) >> team
        1 * teamRepository.save(team)
        response.status == 200
    }

    def "should update team preserving the stored short code"() {
        given: "a rename payload echoing the previously served short code"
        def existing = Team.builder()
                .id(65l)
                .name("Legia")
                .city("Warszawa")
                .shortCode("LGW")
                .build()

        when:
        def response = mockMvc.perform(put("/api/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"id": 65, "name": "Wisla", "city": "Krakow", "short": "LEG"}'))
                .andReturn().response

        then:
        1 * teamRepository.findById(65l) >> Optional.of(existing)
        1 * teamRepository.save({ Team saved ->
            saved.name == "Wisla" && saved.city == "Krakow" && saved.shortCode == "LGW"
        })
        0 * teamConverter.convertFromDto(_)
        response.status == 200
    }

    def "should respond 404 when updating a missing team"() {
        when:
        def response = mockMvc.perform(put("/api/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"id": 404, "name": "Ghost"}'))
                .andReturn().response

        then:
        1 * teamRepository.findById(404l) >> Optional.empty()
        0 * teamRepository.save(_)
        response.status == 404
    }

    def "should return teams by ids"() {
        given:
        def teams = [[] as Team, [] as Team]
        def teamsDtos = [TeamDto.builder().id(23l).build(), TeamDto.builder().id(55l).build()]

        when:
        def response = mockMvc.perform(post("/api/teams/byIds")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"ids": [23, 55]}'))
                .andReturn().response

        then:
        1 * teamRepository.findAllById([23l, 55l]) >> teams
        1 * teamConverter.convertFromEntities(teams) >> teamsDtos
        response.status == 200
        def json = new JsonSlurper().parseText(response.contentAsString)
        json*.id == [23, 55]
    }

    def "should return standings"() {
        given:
        def teams = [[] as Team]
        def teamsDtos = [TeamDto.builder().points(30 as Short).build()]

        when:
        def response = mockMvc.perform(get("/api/teams/standings")).andReturn().response

        then:
        1 * teamService.getStandings() >> teams
        1 * teamConverter.convertFromEntities(teams) >> teamsDtos
        response.status == 200
        def json = new JsonSlurper().parseText(response.contentAsString)
        json[0].points == 30
    }

    def "should delete all teams"() {
        when:
        def response = mockMvc.perform(delete("/api/teams")).andReturn().response

        then:
        1 * teamRepository.deleteAll()
        response.status == 200
    }

    def "should delete team with provided id"() {
        when:
        def response = mockMvc.perform(delete("/api/teams/213")).andReturn().response

        then:
        1 * teamRepository.deleteById(213l)
        response.status == 200
    }
}
