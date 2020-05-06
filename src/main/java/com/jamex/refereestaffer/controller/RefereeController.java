package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.converter.RefereeConverter;
import com.jamex.refereestaffer.model.dto.RefereeDto;
import com.jamex.refereestaffer.model.request.IDRequest;
import com.jamex.refereestaffer.repository.RefereeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/referees")
public class RefereeController {

    private final RefereeRepository refereeRepository;
    private final RefereeConverter refereeConverter;

    @GetMapping
    public Collection<RefereeDto> getReferees() {
        var referees = refereeRepository.findAll();
        return refereeConverter.convertFromEntities(referees);
    }

    @PostMapping
    void addReferee(@RequestBody RefereeDto refereeDto) {
        var referee = refereeConverter.convertFromDto(refereeDto);
        refereeRepository.save(referee);
    }

    @PostMapping("/byIds")
    public Collection<RefereeDto> getRefereesByIds(@RequestBody IDRequest request) {
        var referees = refereeRepository.findAllById(request.getIds());
        return refereeConverter.convertFromEntities(referees);
    }
}
