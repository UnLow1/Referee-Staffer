package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.converter.MatchConverter;
import com.jamex.refereestaffer.model.dto.MatchDto;
import com.jamex.refereestaffer.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class MatchController {

    private final MatchRepository matchRepository;
    private final MatchConverter matchConverter;

    @GetMapping("/matches")
    public Collection<MatchDto> getMatches() {
        var matches = matchRepository.findAll();
        return matchConverter.convertFromEntities(matches);
    }

    @PostMapping("/matches")
    void addMatch(@RequestBody MatchDto matchDto) {
        var match = matchConverter.convertFromDto(matchDto);
        matchRepository.save(match);
    }
}
