package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.converter.TeamConverter;
import com.jamex.refereestaffer.model.dto.TeamDto;
import com.jamex.refereestaffer.model.exception.TeamNotFoundException;
import com.jamex.refereestaffer.model.request.IDRequest;
import com.jamex.refereestaffer.repository.TeamRepository;
import com.jamex.refereestaffer.service.TeamService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;
    private final TeamRepository teamRepository;
    private final TeamConverter teamConverter;

    @GetMapping
    public Collection<TeamDto> getTeams() {
        log.info("Getting all teams");
        var teams = teamRepository.findAll();
        return teamConverter.convertFromEntities(teams);
    }

    @GetMapping("/{id}")
    public TeamDto getTeam(@PathVariable Long id) {
        log.info("Getting team with id " + id);
        var team = teamRepository.findById(id)
                .orElseThrow(() -> new TeamNotFoundException(id));
        return teamConverter.convertFromEntity(team);
    }

    @PostMapping
    public void createTeam(@RequestBody TeamDto teamDto) {
        log.info("Adding new team");
        var team = teamConverter.convertFromDto(teamDto);
        teamRepository.save(team);
    }

    @PutMapping
    public void updateTeam(@RequestBody TeamDto teamDto) {
        log.info("Updating team with id " + teamDto.getId());
        var team = teamConverter.convertFromDto(teamDto);
        teamRepository.save(team);
    }

    @PostMapping("/byIds")
    public Collection<TeamDto> getTeamsByIds(@RequestBody IDRequest request) {
        log.info("Getting teams with ids: " + request.getIds());
        var teams = teamRepository.findAllById(request.getIds());
        return teamConverter.convertFromEntities(teams);
    }

    @GetMapping("/standings")
    public Collection<TeamDto> getStandings() {
        log.info("Calculating standings");
        var teams = teamService.getStandings();
        return teamConverter.convertFromEntities(teams);
    }

    @DeleteMapping
    public void deleteAll() {
        log.info("Deleting all teams");
        teamRepository.deleteAll();
    }

    @DeleteMapping("/{id}")
    public void deleteTeam(@PathVariable Long id) {
        log.info("Deleting team with id = " + id);
        teamRepository.deleteById(id);
    }
}
