package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.dto.MatchDto;
import com.jamex.refereestaffer.model.request.StaffingLockRequest;
import com.jamex.refereestaffer.service.StafferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/api/staffer")
public class StafferController {

    private static final Logger log = LoggerFactory.getLogger(StafferController.class);

    private final StafferService stafferService;

    public StafferController(StafferService stafferService) {
        this.stafferService = stafferService;
    }

    /**
     * The cast stored for a queue. Read-only: this is what the Staffer screen shows on entry
     * and after a queue change, and what the assignment PDF is rendered from.
     */
    @GetMapping("/{queue}")
    public Collection<MatchDto> getStoredCast(@PathVariable short queue) {
        log.info("Getting stored cast for queue {}", queue);
        return stafferService.getStoredCast(queue);
    }

    /**
     * Generates a draft cast — since RS-105 nothing is persisted here, the client saves it
     * with PUT /api/matches ("Save cast"). Still a POST rather than a GET: the request
     * carries a body and running the algorithm is far from a cheap, cacheable lookup.
     *
     * <p>The optional body carries locked (matchId, refereeId) pairs the algorithm must keep
     * as-is while it staffs the rest of the queue. No body / empty list means a full
     * regenerate; optional so clients that post no body still work.
     */
    @PostMapping("/{queue}")
    public Collection<MatchDto> staffReferees(@PathVariable short queue,
                                              @RequestBody(required = false) List<StaffingLockRequest> locks) {
        var lockedPairs = locks == null ? List.<StaffingLockRequest>of() : locks;
        log.info("Generating draft cast for queue {} with {} locked assignments", queue, lockedPairs.size());
        return stafferService.staffReferees(queue, lockedPairs);
    }
}
