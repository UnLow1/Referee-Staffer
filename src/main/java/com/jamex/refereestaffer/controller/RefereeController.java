package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.entity.Referee;
import com.jamex.refereestaffer.repository.RefereeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class RefereeController {

    private final RefereeRepository refereeRepository;

    @GetMapping("/referees")
    public List<Referee> getReferees() {
        return (List<Referee>) refereeRepository.findAll();
    }

    @PostMapping("/referees")
    void addReferee(@RequestBody Referee referee) {
        refereeRepository.save(referee);
    }
}
