package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.converter.TeamConverter;
import com.jamex.refereestaffer.model.dto.TeamDto;
import com.jamex.refereestaffer.model.request.IDRequest;
import com.jamex.refereestaffer.repository.TeamRepository;
import com.jamex.refereestaffer.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "https://referee-staffer.herokuapp.com")
@RequestMapping("/teams")
public class TeamController {

    private final TeamService teamService;
    private final TeamRepository teamRepository;
    private final TeamConverter teamConverter;

    @GetMapping
    public Collection<TeamDto> getTeams() {
        var teams = teamRepository.findAll();
        return teamConverter.convertFromEntities(teams);
    }

    @PostMapping
    void addTeam(@RequestBody TeamDto teamDto) {
        var team = teamConverter.convertFromDto(teamDto);
        teamRepository.save(team);
    }

    @PostMapping("/byIds")
    public Collection<TeamDto> getTeamsByIds(@RequestBody IDRequest request) {
        var teams = teamRepository.findAllById(request.getIds());
        return teamConverter.convertFromEntities(teams);
    }

    @GetMapping("/standings")
    public Collection<TeamDto> getStandings() {
        return teamService.getStandings();
    }
}
