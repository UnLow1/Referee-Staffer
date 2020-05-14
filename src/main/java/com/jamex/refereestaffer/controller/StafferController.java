package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.dto.MatchDto;
import com.jamex.refereestaffer.service.StafferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/staffer")
public class StafferController {

    private final StafferService stafferService;

    @GetMapping("/{queue}")
    public Collection<MatchDto> staffReferees(@PathVariable Short queue) {
        log.info("Generating cast for queue " + queue);
        return stafferService.staffReferees(queue);
    }
}
