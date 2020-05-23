package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.converter.TeamConverter;
import com.jamex.refereestaffer.model.dto.TeamDto;
import com.jamex.refereestaffer.model.exception.TeamNotFoundException;
import com.jamex.refereestaffer.model.request.IDRequest;
import com.jamex.refereestaffer.repository.TeamRepository;
import com.jamex.refereestaffer.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

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
        var team = teamRepository.findById(id).orElseThrow(() -> new TeamNotFoundException(id));
        return teamConverter.convertFromEntity(team);
    }

    @PostMapping
    public void addTeam(@RequestBody TeamDto teamDto) {
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

    @DeleteMapping("/{id}")
    public void deleteTeam(@PathVariable Long id) {
        log.info("Deleting team with id = " + id);
        teamRepository.deleteById(id);
    }
}
