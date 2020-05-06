package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.converter.MatchConverter;
import com.jamex.refereestaffer.model.dto.MatchDto;
import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/matches")
public class MatchController {

    private final MatchRepository matchRepository;
    private final MatchConverter matchConverter;

    @GetMapping
    public Collection<MatchDto> getMatches() {
        var matches = matchRepository.findAll();
        return matchConverter.convertFromEntities(matches);
    }

    @PostMapping
    MatchDto addMatch(@RequestBody MatchDto matchDto) {
        var match = matchConverter.convertFromDto(matchDto);
        var savedMatch = matchRepository.save(match);
        return matchConverter.convertFromEntity(savedMatch);
    }
}
