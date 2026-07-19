package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.converter.MatchConverter;
import com.jamex.refereestaffer.model.dto.DifficultyBreakdownDto;
import com.jamex.refereestaffer.model.dto.MatchDto;
import com.jamex.refereestaffer.model.exception.MatchNotFoundException;
import com.jamex.refereestaffer.model.exception.RequestValidationException;
import com.jamex.refereestaffer.model.validation.OnUpdate;
import com.jamex.refereestaffer.repository.MatchRepository;
import com.jamex.refereestaffer.service.MatchService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private static final Logger log = LoggerFactory.getLogger(MatchController.class);

    private final MatchRepository matchRepository;
    private final MatchConverter matchConverter;
    private final MatchService matchService;

    public MatchController(MatchRepository matchRepository, MatchConverter matchConverter, MatchService matchService) {
        this.matchRepository = matchRepository;
        this.matchConverter = matchConverter;
        this.matchService = matchService;
    }

    @GetMapping
    public Collection<MatchDto> getMatches() {
        log.info("Getting all matches");
        var matches = matchRepository.findAll();
        return matchConverter.convertFromEntities(matches);
    }

    @GetMapping("/{id}")
    public MatchDto getMatch(@PathVariable Long id) {
        log.info("Getting match with id {}", id);
        var match = matchRepository.findById(id)
                .orElseThrow(() -> new MatchNotFoundException(id));
        return matchConverter.convertFromEntity(match);
    }

    /**
     * Per-component difficulty for a single match. Powers the Staffer drawer breakdown
     * table and the Match detail "Difficulty breakdown" panel.
     */
    @GetMapping("/{id}/difficulty")
    public DifficultyBreakdownDto getMatchDifficulty(@PathVariable Long id) {
        log.info("Computing difficulty breakdown for match {}", id);
        return matchService.computeDifficultyBreakdown(id);
    }

    @PostMapping
    public MatchDto createMatch(@Valid @RequestBody MatchDto matchDto) {
        log.info("Adding new match");
        var match = matchConverter.convertFromDto(matchDto);
        var savedMatch = matchRepository.save(match);
        return matchConverter.convertFromEntity(savedMatch);
    }

    // TODO is this id needed?
    @PutMapping("/{id}")
    public MatchDto updateMatch(@Validated(OnUpdate.class) @RequestBody MatchDto matchDto, @PathVariable Long id) {
        log.info("Updating match with id {}", matchDto.getId());
        var match = matchConverter.convertFromDto(matchDto);
        var updatedMatch = matchRepository.save(match);
        return matchConverter.convertFromEntity(updatedMatch);
    }

    @PutMapping
    public void updateMatches(@RequestBody List<@Valid MatchDto> matchesDtos) {
        requireIds(matchesDtos);
        var matchIds = matchesDtos.stream()
                .map(MatchDto::getId)
                .toList();
        log.info("Updating matches with ids: {}", matchIds);
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
        log.info("Deleting match with id = {}", id);
        matchService.deleteMatch(id);
    }

    /**
     * Element validation on a {@code List<@Valid ...>} body always runs in the Default
     * group (container validation cannot select OnUpdate), so id presence must be checked
     * by hand — a null id would make saveAll() insert a new match instead of updating one.
     */
    private static void requireIds(List<MatchDto> matchesDtos) {
        var errors = new ArrayList<String>();
        for (int i = 0; i < matchesDtos.size(); i++) {
            if (matchesDtos.get(i).getId() == null) {
                errors.add("[" + i + "].id: must not be null");
            }
        }
        if (!errors.isEmpty()) {
            throw new RequestValidationException(String.join("; ", errors));
        }
    }
}
