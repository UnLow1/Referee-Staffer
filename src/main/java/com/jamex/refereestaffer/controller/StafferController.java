package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.dto.MatchDto;
import com.jamex.refereestaffer.service.StafferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/staffer")
public class StafferController {

    private final StafferService stafferService;

    @GetMapping("/{queue}")
    public Collection<MatchDto> staffReferees(@PathVariable short queue) {
        log.info("Generating cast for queue " + queue);
        return stafferService.staffReferees(queue);
    }
}
