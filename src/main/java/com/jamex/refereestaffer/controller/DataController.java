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
 * <p>There is no UI for it — the "Clear data" button died with the old header in the
 * 2026-06 redesign and has not been rebuilt. The intended way to call this is the
 * Swagger UI (springdoc is on the classpath and enabled by default) or curl.
 *
 * <p>Wiping everything is irreversible and a bare {@code DELETE /api/data} is far too
 * easy to fire by accident (a stray request from a tool, a mis-pasted curl), so the
 * confirmation token below is mandatory. It makes the call deliberate rather than
 * secret: the rejection message names the expected token on purpose, because the guard
 * is ergonomic, not a secret, and a caller who cannot discover it would simply be stuck.
 * Real protection needs authorization (RS-4).
 */
@RestController
@RequestMapping("/api/data")
public class DataController {

    private static final Logger log = LoggerFactory.getLogger(DataController.class);

    // Private on purpose: the specs assert the literal "delete-all-data" instead of
    // referencing this constant, so that renaming the token — a breaking change for every
    // existing caller — fails the build rather than silently following along.
    private static final String CONFIRMATION_TOKEN = "delete-all-data";

    private final DataService dataService;

    public DataController(DataService dataService) {
        this.dataService = dataService;
    }

    @DeleteMapping
    public ClearDataSummaryDto clearAllData(@RequestParam(required = false) String confirm) {
        if (!CONFIRMATION_TOKEN.equals(confirm)) {
            // WARN, not DEBUG: an attempted wipe of the whole database is worth a line at
            // the default log level. RestExceptionHandler logs the 400 itself at DEBUG,
            // which would leave the one destructive endpoint in the app with a silent
            // failure path and a chatty success path.
            log.warn("Rejected clear-all-data request: confirm={}", confirm);
            throw new RequestValidationException("confirm: must be '" + CONFIRMATION_TOKEN + "' to wipe all data");
        }
        // The success log lives in DataService, which can also report the row counts.
        return dataService.clearAllData();
    }
}
