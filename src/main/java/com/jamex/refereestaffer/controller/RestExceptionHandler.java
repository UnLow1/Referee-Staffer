package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.exception.DownloadFileException;
import com.jamex.refereestaffer.model.exception.GradeNotFoundException;
import com.jamex.refereestaffer.model.exception.ImportException;
import com.jamex.refereestaffer.model.exception.MatchNotFoundException;
import com.jamex.refereestaffer.model.exception.RefereeNotFoundException;
import com.jamex.refereestaffer.model.exception.RequestValidationException;
import com.jamex.refereestaffer.model.exception.StafferException;
import com.jamex.refereestaffer.model.exception.TeamNotFoundException;
import com.jamex.refereestaffer.model.exception.VacationNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.stream.Collectors;

/**
 * Centralized HTTP error mapping for domain exceptions.
 *
 * <p>Without this advice all custom exceptions propagate as 500 with a stacktrace, which leaks
 * internals and gives clients no way to distinguish "no such match" (404) from "not enough referees
 * to staff this round" (409) from "your CSV is malformed" (400). The body is a {@link ProblemDetail}
 * (RFC 7807) — Spring's built-in error format, no custom DTO needed.
 */
@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler({
            MatchNotFoundException.class,
            RefereeNotFoundException.class,
            TeamNotFoundException.class,
            GradeNotFoundException.class,
            VacationNotFoundException.class
    })
    public ProblemDetail handleNotFound(RuntimeException ex) {
        log.debug("Resource not found: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(StafferException.class)
    public ProblemDetail handleStafferConflict(StafferException ex) {
        // Business rule violation — the request itself is fine, but the system can't fulfil it
        // in its current state (e.g. not enough available referees for the queue).
        log.info("Staffing conflict: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ImportException.class)
    public ProblemDetail handleImport(ImportException ex) {
        // Caller-supplied CSV failed to parse / convert — treat as bad request.
        log.warn("Import failed: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DownloadFileException.class)
    public ProblemDetail handleDownload(DownloadFileException ex) {
        // Server-side IO failure (file missing on disk / unreadable) — not the client's fault.
        log.error("Download failed: {}", ex.getMessage(), ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    /**
     * Bean validation failure on a single {@code @Valid}/{@code @Validated} request body.
     * The field list goes into {@code detail} because that's the one property the frontend
     * error toast renders — a structured errors map would be invisible to the user.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        var detail = ex.getBindingResult().getFieldErrors().stream()
                .map(RestExceptionHandler::describe)
                .sorted()
                .collect(Collectors.joining("; "));
        log.debug("Request body validation failed: {}", detail);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    /**
     * Validation failure on container elements — {@code List<@Valid Dto>} bodies (bulk match
     * update, configuration update). Spring routes those through built-in method validation,
     * which throws this instead of {@link MethodArgumentNotValidException}.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        var detail = ex.getParameterValidationResults().stream()
                .flatMap(result -> {
                    var index = result.getContainerIndex();
                    var prefix = index != null ? "[" + index + "]." : "";
                    return result.getResolvableErrors().stream().map(error -> prefix + describe(error));
                })
                .sorted()
                .collect(Collectors.joining("; "));
        log.debug("Request body validation failed: {}", detail);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    /**
     * Hand-written request checks that bean validation cannot express (e.g. the id
     * presence check on bulk list bodies). The message already follows the same
     * {@code field: message} format as the two handlers above.
     */
    @ExceptionHandler(RequestValidationException.class)
    public ProblemDetail handleRequestValidation(RequestValidationException ex) {
        log.debug("Request body validation failed: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    private static String describe(MessageSourceResolvable error) {
        return error instanceof FieldError fieldError
                ? fieldError.getField() + ": " + fieldError.getDefaultMessage()
                : String.valueOf(error.getDefaultMessage());
    }
}
