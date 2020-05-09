package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.dto.MatchDto;
import com.jamex.refereestaffer.service.StafferService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/staffer")
public class StafferController {

    private final StafferService stafferService;

    @GetMapping("/{queue}")
    public Collection<MatchDto> staffReferees(@PathVariable Short queue) {
        return stafferService.staffReferees(queue);
    }
}
