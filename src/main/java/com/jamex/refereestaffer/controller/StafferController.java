package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.dto.MatchDto;
import com.jamex.refereestaffer.model.dto.StaffingOverwriteDto;
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
     * Assignments a staffing run for this queue would clear (RS-109) — the doomed ones, not
     * the queue's cast. Read-only and cheap: the Staffer screen calls it right before
     * regenerating so it can warn — with a count — that existing (possibly hand-made)
     * assignments are about to be overwritten. Named for that narrow job on purpose, so the
     * obvious {@code /{queue}/assignments} stays free for loading the actual cast (RS-105).
     */
    @GetMapping("/{queue}/overwrite-preview")
    public StaffingOverwriteDto getOverwrittenAssignments(@PathVariable short queue) {
        log.info("Checking existing assignments in queue {}", queue);
        return stafferService.getOverwrittenAssignments(queue);
    }

    /**
     * The optional body carries locked (matchId, refereeId) pairs the algorithm must keep
     * as-is while it staffs the rest of the queue. No body / empty list means a full
     * regenerate; optional so clients that post no body still work.
     */
    @PostMapping("/{queue}")
    public Collection<MatchDto> staffReferees(@PathVariable short queue,
                                              @RequestBody(required = false) List<StaffingLockRequest> locks) {
        var lockedPairs = locks == null ? List.<StaffingLockRequest>of() : locks;
        log.info("Generating cast for queue {} with {} locked assignments", queue, lockedPairs.size());
        return stafferService.staffReferees(queue, lockedPairs);
    }
}
