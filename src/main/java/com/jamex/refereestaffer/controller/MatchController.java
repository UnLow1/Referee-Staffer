package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.converter.MatchConverter;
import com.jamex.refereestaffer.model.dto.MatchDto;
import com.jamex.refereestaffer.model.exception.MatchNotFoundException;
import com.jamex.refereestaffer.repository.MatchRepository;
import com.jamex.refereestaffer.service.MatchService;
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
    private final MatchService matchService;

    @GetMapping
    public Collection<MatchDto> getMatches() {
        log.info("Getting all matches");
        var matches = matchRepository.findAll();
        return matchConverter.convertFromEntities(matches);
    }

    @GetMapping("/{id}")
    public MatchDto getMatch(@PathVariable Long id) {
        log.info("Getting match with id " + id);
        var match = matchRepository.findById(id)
                .orElseThrow(() -> new MatchNotFoundException(id));
        return matchConverter.convertFromEntity(match);
    }

    @PostMapping
    public MatchDto addMatch(@RequestBody MatchDto matchDto) {
        log.info("Adding new match");
        var match = matchConverter.convertFromDto(matchDto);
        var savedMatch = matchRepository.save(match);
        return matchConverter.convertFromEntity(savedMatch);
    }

    // TODO is this id needed?
    @PutMapping("/{id}")
    public MatchDto updateMatch(@RequestBody MatchDto matchDto, @PathVariable Long id) {
        log.info("Updating match with id " + matchDto.getId());
        var match = matchConverter.convertFromDto(matchDto);
        var updatedMatch = matchRepository.save(match);
        return matchConverter.convertFromEntity(updatedMatch);
    }

    @PutMapping
    public void updateMatches(@RequestBody List<MatchDto> matchesDtos) {
        var matchIds = matchesDtos.stream()
                .map(MatchDto::getId)
                .toList();
        log.info("Updating matches with ids: " + matchIds);
        var matches = matchConverter.convertFromDtos(matchesDtos);
        matchRepository.saveAll(matches);
    }

    @DeleteMapping
    public void deleteAll() {
        log.info("Deleting all matches");
        matchRepository.deleteAll();
    }

    @DeleteMapping("/{id}")
    public void deleteMatch(@PathVariable Long id) {
        log.info("Deleting match with id = " + id);
        matchService.deleteMatch(id);
    }
}
