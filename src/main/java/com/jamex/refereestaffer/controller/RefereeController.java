package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.converter.RefereeConverter;
import com.jamex.refereestaffer.model.dto.RefereeDto;
import com.jamex.refereestaffer.repository.RefereeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class RefereeController {

    private final RefereeRepository refereeRepository;
    private final RefereeConverter refereeConverter;

    @GetMapping("/referees")
    public Collection<RefereeDto> getReferees() {
        var referees = refereeRepository.findAll();
        return refereeConverter.convertFromEntities(referees);
    }

    @PostMapping("/referees")
    void addReferee(@RequestBody RefereeDto refereeDto) {
        var referee = refereeConverter.convertFromDto(refereeDto);
        refereeRepository.save(referee);
    }
}
