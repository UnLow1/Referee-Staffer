package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.converter.VacationConverter;
import com.jamex.refereestaffer.model.dto.VacationDto;
import com.jamex.refereestaffer.model.exception.VacationNotFoundException;
import com.jamex.refereestaffer.repository.VacationRepository;
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
@RequestMapping("/api/vacations")
public class VacationController {

    private final VacationRepository vacationRepository;
    private final VacationConverter vacationConverter;

    @GetMapping
    public Collection<VacationDto> getVacations() {
        log.info("Getting all vacations");
        var vacations = vacationRepository.findAll();
        return vacationConverter.convertFromEntities(vacations);
    }

    @GetMapping("{id}")
    public VacationDto getVacation(@PathVariable Long id) {
        log.info("Getting vacation with id " + id);
        var vacation = vacationRepository.findById(id)
                .orElseThrow(() -> new VacationNotFoundException(id));
        return vacationConverter.convertFromEntity(vacation);
    }

    @PostMapping
    public VacationDto createVacation(@RequestBody VacationDto vacationDto) {
        log.info("Adding new vacation");
        var vacation = vacationConverter.convertFromDto(vacationDto);
        var savedVacation = vacationRepository.save(vacation);
        return vacationConverter.convertFromEntity(savedVacation);
    }

    @PutMapping("/{id}")
    public VacationDto updateVacation(@RequestBody VacationDto vacationDto, @PathVariable Long id) {
        log.info("Updating vacation with id " + vacationDto.getId());
        var vacation = vacationConverter.convertFromDto(vacationDto);
        var updatedVacation = vacationRepository.save(vacation);
        return vacationConverter.convertFromEntity(updatedVacation);
    }

    @DeleteMapping()
    public void deleteAll() {
        log.info("Deleting all vacations");
        vacationRepository.deleteAll();
    }

    @DeleteMapping("/{id}")
    public void deleteVacation(@PathVariable Long id) {
        log.info("Deleting vacation with id = " + id);
        vacationRepository.deleteById(id);
    }
}
