package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.dto.ClearDataSummaryDto;
import com.jamex.refereestaffer.model.exception.RequestValidationException;
import com.jamex.refereestaffer.service.DataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The single "clear all data" entry point (RS-112). It exists because a clean database
 * is otherwise only reachable by restarting the app on an in-memory profile, which the
 * file-based {@code prod} profile does not give you.
 *
 * <p>Wiping everything is irreversible and a bare {@code DELETE /api/data} is far too
 * easy to fire by accident (a stray request from a tool, a mis-pasted curl), so the
 * confirmation token below is mandatory. It is a guard against mistakes, not against an
 * attacker — real protection needs authorization (RS-4).
 */
@RestController
@RequestMapping("/api/data")
public class DataController {

    private static final Logger log = LoggerFactory.getLogger(DataController.class);

    static final String CONFIRMATION_TOKEN = "delete-all-data";

    private final DataService dataService;

    public DataController(DataService dataService) {
        this.dataService = dataService;
    }

    @DeleteMapping
    public ClearDataSummaryDto clearAllData(@RequestParam(required = false) String confirm) {
        if (!CONFIRMATION_TOKEN.equals(confirm)) {
            throw new RequestValidationException("confirm: must be '" + CONFIRMATION_TOKEN + "' to wipe all data");
        }
        log.info("Clearing all domain data");
        return dataService.clearAllData();
    }
}
