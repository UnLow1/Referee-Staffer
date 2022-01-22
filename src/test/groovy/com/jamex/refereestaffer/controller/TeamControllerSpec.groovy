package com.jamex.refereestaffer.controller

import com.jamex.refereestaffer.model.converter.TeamConverter
import com.jamex.refereestaffer.model.dto.TeamDto
import com.jamex.refereestaffer.model.entity.Team
import com.jamex.refereestaffer.model.exception.TeamNotFoundException
import com.jamex.refereestaffer.model.request.IDRequest
import com.jamex.refereestaffer.repository.TeamRepository
import com.jamex.refereestaffer.service.TeamService
import spock.lang.Specification
import spock.lang.Subject

class TeamControllerSpec extends Specification {

    @Subject
    TeamController teamController

    TeamService teamService = Mock()
    TeamRepository teamRepository = Mock()
    TeamConverter teamConverter = Mock()

    def setup() {
        teamController = new TeamController(teamService, teamRepository, teamConverter)
    }

    def "should return teams"() {
        given:
        def teams = [[] as Team, [] as Team]
        def teamsDtos = [TeamDto.builder().build(), TeamDto.builder().build()]

        when:
        def result = teamController.getTeams()

        then:
        1 * teamRepository.findAll() >> teams
        1 * teamConverter.convertFromEntities(teams) >> teamsDtos
        result == teamsDtos
    }

    def "should throw TeamNotFoundException when team has not been found"() {
        given:
        def teamId = 12398l

        when:
        teamController.getTeam(teamId)

        then:
        1 * teamRepository.findById(teamId) >> Optional.empty()
        def exception = thrown(TeamNotFoundException)
        exception.message == String.format(TeamNotFoundException.NOT_FOUND_WITH_ID, teamId)
    }

    def "should return team"() {
        given:
        def teamId = 12398l
        def team = [] as Team
        def teamDto = TeamDto.builder().build()

        when:
        def result = teamController.getTeam(teamId)

        then:
        1 * teamRepository.findById(teamId) >> Optional.of(team)
        1 * teamConverter.convertFromEntity(team) >> teamDto
        result == teamDto
    }

    def "should add team"() {
        given:
        def teamDto = TeamDto.builder().build()
        def team = [] as Team

        when:
        teamController.createTeam(teamDto)

        then:
        1 * teamConverter.convertFromDto(teamDto) >> team
        1 * teamRepository.save(team)
    }

    def "should update team"() {
        given:
        def teamDto = TeamDto.builder().build()
        def team = [] as Team

        when:
        teamController.updateTeam(teamDto)

        then:
        1 * teamConverter.convertFromDto(teamDto) >> team
        1 * teamRepository.save(team)
    }

    def "should return teams by ids"() {
        given:
        def idsList = [23l, 55l]
        def request = [getIds: { idsList }] as IDRequest
        def teams = [[] as Team, [] as Team]
        def teamsDtos = [TeamDto.builder().build(), TeamDto.builder().build()]

        when:
        def result = teamController.getTeamsByIds(request)

        then:
        1 * teamRepository.findAllById(idsList) >> teams
        1 * teamConverter.convertFromEntities(teams) >> teamsDtos
        result == teamsDtos
    }

    def "should return standings"() {
        given:
        def teams = [[] as Team, [] as Team]
        def teamsDtos = [TeamDto.builder().build(), TeamDto.builder().build()]

        when:
        def result = teamController.getStandings()

        then:
        1 * teamService.getStandings() >> teams
        1 * teamConverter.convertFromEntities(teams) >> teamsDtos
        result == teamsDtos
    }

    def "should delete all teams"() {
        when:
        teamController.deleteAll()

        then:
        1 * teamRepository.deleteAll()
    }

    def "should delete team with provided id"() {
        given:
        def teamId = 213l

        when:
        teamController.deleteTeam(teamId)

        then:
        1 * teamRepository.deleteById(teamId)
    }
}
