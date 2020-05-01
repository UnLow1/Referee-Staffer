package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.Match;
import com.jamex.refereestaffer.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class MatchController {

    private final MatchRepository matchRepository;

    @GetMapping("/matches")
    public List<Match> getMatches() {
        return (List<Match>) matchRepository.findAll();
    }
}
