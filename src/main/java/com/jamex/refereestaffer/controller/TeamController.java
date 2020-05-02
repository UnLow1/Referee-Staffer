package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.entity.Team;
import com.jamex.refereestaffer.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class TeamController {

    private final TeamRepository teamRepository;

    @GetMapping("/teams")
    public List<Team> getTeams() {
        return (List<Team>) teamRepository.findAll();
    }
}
