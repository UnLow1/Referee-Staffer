package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.dto.MatchDto;
import com.jamex.refereestaffer.model.request.StaffingLockRequest;
import com.jamex.refereestaffer.service.StafferService;
import jakarta.validation.constraints.Min;
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
     * The optional body carries locked (matchId, refereeId) pairs the algorithm must keep
     * as-is while it staffs the rest of the queue. No body / empty list means a full
     * regenerate; optional so clients that post no body still work.
     *
     * <p>Queue numbering starts at 1, so anything below that is rejected as a bad request
     * before the service runs (RS-115) — it used to answer 200 with an empty list, which a
     * client could not tell apart from "this queue has nothing left to staff". A queue above
     * the end of the season is a 404 raised by the service, which knows the season's range.
     */
    @PostMapping("/{queue}")
    public Collection<MatchDto> staffReferees(@PathVariable @Min(1) short queue,
                                              @RequestBody(required = false) List<StaffingLockRequest> locks) {
        var lockedPairs = locks == null ? List.<StaffingLockRequest>of() : locks;
        log.info("Generating cast for queue {} with {} locked assignments", queue, lockedPairs.size());
        return stafferService.staffReferees(queue, lockedPairs);
    }
}
