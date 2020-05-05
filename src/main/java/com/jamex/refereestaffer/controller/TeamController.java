package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.converter.TeamConverter;
import com.jamex.refereestaffer.model.dto.TeamDto;
import com.jamex.refereestaffer.model.request.IDRequest;
import com.jamex.refereestaffer.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class TeamController {

    private final TeamRepository teamRepository;
    private final TeamConverter teamConverter;

    @GetMapping("/teams")
    public Collection<TeamDto> getTeams() {
        var teams = teamRepository.findAll();
        return teamConverter.convertFromEntities(teams);
    }

    @PostMapping("/teams/byIds")
    public Collection<TeamDto> getTeamsByIds(@RequestBody IDRequest request) {
        var teams = teamRepository.findAllById(request.getIds());
        return teamConverter.convertFromEntities(teams);
    }
}
