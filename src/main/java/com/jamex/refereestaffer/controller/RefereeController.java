package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.converter.RefereeConverter;
import com.jamex.refereestaffer.model.dto.RefereeDto;
import com.jamex.refereestaffer.model.exception.RefereeNotFoundException;
import com.jamex.refereestaffer.model.request.IDRequest;
import com.jamex.refereestaffer.repository.RefereeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/referees")
public class RefereeController {

    private final RefereeRepository refereeRepository;
    private final RefereeConverter refereeConverter;

    @GetMapping
    public Collection<RefereeDto> getReferees() {
        log.info("Getting all referees");
        var referees = refereeRepository.findAll();
        return refereeConverter.convertFromEntities(referees);
    }

    @GetMapping("/{id}")
    public RefereeDto getReferee(@PathVariable Long id) {
        log.info("Getting referee with id " + id);
        var referee = refereeRepository.findById(id)
                .orElseThrow(() -> new RefereeNotFoundException(id));
        return refereeConverter.convertFromEntity(referee);
    }

    @GetMapping("/available/{queue}")
    public Collection<RefereeDto> getRefereesAvailableForQueue(@PathVariable Short queue) {
        log.info("Getting referees available for queue " + queue);
        var referees = refereeRepository.findAllWithNoMatchInQueue(queue);
        return refereeConverter.convertFromEntities(referees);
    }

    @PutMapping
    public void updateReferee(@RequestBody RefereeDto refereeDto) {
        log.info("Updating referee with id " + refereeDto.getId());
        var referee = refereeConverter.convertFromDto(refereeDto);
        refereeRepository.save(referee);
    }

    @PostMapping
    public void createReferee(@RequestBody RefereeDto refereeDto) {
        log.info("Adding new referee");
        var referee = refereeConverter.convertFromDto(refereeDto);
        refereeRepository.save(referee);
    }

    @PostMapping("/byIds")
    public Collection<RefereeDto> getRefereesByIds(@RequestBody IDRequest request) {
        log.info("Getting referees with ids: " + request.getIds());
        var referees = refereeRepository.findAllById(request.getIds());
        return refereeConverter.convertFromEntities(referees);
    }

    @DeleteMapping
    public void deleteAll() {
        log.info("Deleting all referees");
        refereeRepository.deleteAll();
    }

    @DeleteMapping("/{id}")
    public void deleteReferee(@PathVariable Long id) {
        log.info("Deleting referee with id = " + id);
        refereeRepository.deleteById(id);
    }
}
