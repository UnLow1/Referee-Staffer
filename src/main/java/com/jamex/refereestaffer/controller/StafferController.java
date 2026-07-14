package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.dto.MatchDto;
import com.jamex.refereestaffer.model.request.StaffingLockRequest;
import com.jamex.refereestaffer.service.StafferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
     * POST, not GET: staffing assigns referees and persists the result, so the verb must
     * signal the state change (was GET until 2026-07, which broke HTTP semantics).
     *
     * <p>The optional body carries locked (matchId, refereeId) pairs the algorithm must
     * keep as-is while it staffs the rest of the queue. No body / empty list means a full
     * regenerate. Kept optional so pre-lock clients (plain POST with no body) still work.
     */
    @PostMapping("/{queue}")
    public Collection<MatchDto> staffReferees(@PathVariable short queue,
                                              @RequestBody(required = false) List<StaffingLockRequest> locks) {
        var lockedPairs = locks == null ? List.<StaffingLockRequest>of() : locks;
        log.info("Generating cast for queue {} with {} locked assignments", queue, lockedPairs.size());
        return stafferService.staffReferees(queue, lockedPairs);
    }
}
