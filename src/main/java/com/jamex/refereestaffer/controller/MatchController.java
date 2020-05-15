package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.converter.MatchConverter;
import com.jamex.refereestaffer.model.dto.MatchDto;
import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchRepository matchRepository;
    private final MatchConverter matchConverter;

    @GetMapping
    public Collection<MatchDto> getMatches() {
        log.info("Getting all matches");
        var matches = matchRepository.findAll();
        return matchConverter.convertFromEntities(matches);
    }

    @PostMapping
    MatchDto addMatch(@RequestBody MatchDto matchDto) {
        log.info("Adding new match");
        var match = matchConverter.convertFromDto(matchDto);
        var savedMatch = matchRepository.save(match);
        return matchConverter.convertFromEntity(savedMatch);
    }

    @PutMapping
    void updateMatches(@RequestBody List<MatchDto> matchesDtos) {
        var matchIds = matchesDtos.stream()
                .map(MatchDto::getId)
                .collect(Collectors.toList());
        log.info("Updating matches with ids: " + matchIds);
        var matches = matchConverter.convertFromDtos(matchesDtos);
        matchRepository.saveAll(matches);
    }
}
