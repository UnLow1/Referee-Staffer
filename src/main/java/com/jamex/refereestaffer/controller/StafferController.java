package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.dto.MatchDto;
import com.jamex.refereestaffer.service.StafferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("/api/staffer")
public class StafferController {

    private static final Logger log = LoggerFactory.getLogger(StafferController.class);

    private final StafferService stafferService;

    public StafferController(StafferService stafferService) {
        this.stafferService = stafferService;
    }

    @GetMapping("/{queue}")
    public Collection<MatchDto> staffReferees(@PathVariable short queue) {
        log.info("Generating cast for queue " + queue);
        return stafferService.staffReferees(queue);
    }
}
