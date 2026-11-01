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
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.hibernate.PropertyValueException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
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

    /**
     * Client-facing replacements for constraint-violation messages. The originals name tables,
     * columns and constraints (Spring builds the {@code DataIntegrityViolationException} message
     * from {@code ConstraintViolationException.getSQL()} plus the constraint name), so they are
     * logged and never sent to the browser.
     */
    public static final String MISSING_REQUIRED_FIELD = "A required field is missing";
    public static final String DUPLICATE_RECORD = "A record with these values already exists";
    // ConstraintKind.FOREIGN_KEY covers both directions — a parent still referenced by a child
    // (H2 23503) and a child pointing at a missing parent (23506) — so the wording has to hold
    // either way. "Referenced by other data" would state the opposite of the truth for the second.
    public static final String RELATED_RECORD_CONFLICT = "The change conflicts with a related record";
    public static final String DATA_INTEGRITY_CONFLICT = "The request conflicts with the stored data";
    public static final String INVALID_FIELD_VALUES = "One or more field values are not valid";

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

    /**
     * Defence-in-depth net for constraints that only the database enforces. Mirroring every
     * {@code nullable = false} column with a bean-validation annotation on the DTO is a manual
     * convention, and some write paths skip controller validation entirely ({@code ImporterService}
     * builds entities directly, {@code StafferService} mutates managed ones inside a transaction),
     * so a violation can still reach the Hibernate flush. Without this handler it surfaces as a
     * bare 500 with no {@code detail} for the frontend toast to show.
     *
     * <p>A missing required value is the caller's mistake and maps to 400; everything else
     * (unique, foreign key, check) is a conflict with already-stored data and maps to 409.
     * The {@code detail} is a fixed constant — see {@link #MISSING_REQUIRED_FIELD}.
     *
     * <p>{@link PropertyValueException} is registered alongside the translated exception because
     * Hibernate raises it directly when it pre-checks nullability, and nothing guarantees every
     * such raise crosses a boundary that wraps it in a {@link DataIntegrityViolationException}.
     */
    @ExceptionHandler({DataIntegrityViolationException.class, PropertyValueException.class})
    public ProblemDetail handleDataIntegrityViolation(RuntimeException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage(), ex);
        var kind = constraintKind(ex);
        if (kind == null) {
            // Nothing in the chain names the constraint. DuplicateKeyException still tells us the
            // shape of the problem (Spring raises it for a detected duplicate without a SQL-level
            // violation, e.g. Hibernate's NonUniqueObjectException).
            var detail = ex instanceof DuplicateKeyException ? DUPLICATE_RECORD : DATA_INTEGRITY_CONFLICT;
            return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
        }
        return switch (kind) {
            case NOT_NULL -> ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, MISSING_REQUIRED_FIELD);
            case UNIQUE -> ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, DUPLICATE_RECORD);
            case FOREIGN_KEY -> ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, RELATED_RECORD_CONFLICT);
            default -> ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, DATA_INTEGRITY_CONFLICT);
        };
    }

    /**
     * Entity-level bean validation (hibernate-validator is on the classpath, so Hibernate runs the
     * entity annotations on pre-insert/pre-update). This fires instead of a SQL-level violation for
     * the {@code @NotNull} mirrors on entities, and is not translated into a
     * {@link DataIntegrityViolationException}, so it needs its own mapping.
     *
     * <p>Property paths are entity field names, not database identifiers, so they are safe to
     * return and give the same {@code field: message} detail as the request-body handlers.
     *
     * <p>Registration is by type, so this would also catch the exception Spring's method
     * validation raises for a {@code @Validated} bean, whose paths read {@code method.arg0.field}.
     * No bean in this project is {@code @Validated}, so that cannot happen today; if one is added,
     * the paths want trimming before they go out, and such a failure is a 500, not a 400.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleEntityConstraintViolation(ConstraintViolationException ex) {
        var violations = ex.getConstraintViolations();
        if (violations == null || violations.isEmpty()) {
            // Nothing says which constraint failed, so the detail must not claim one: a @Size or
            // @Pattern violation would make "a required field is missing" plainly wrong.
            log.warn("Entity validation failed without violations: {}", ex.getMessage(), ex);
            return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, INVALID_FIELD_VALUES);
        }
        var detail = violations.stream()
                .map(RestExceptionHandler::describe)
                .sorted()
                .collect(Collectors.joining("; "));
        log.warn("Entity validation failed: {} ({})", detail, ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    /**
     * Narrows the violation to a Hibernate {@link ConstraintKind} without parsing any message
     * (the message is exactly the part we must not read out loud). Spring wraps the Hibernate
     * exception, so the classifier walks the cause chain;
     * a {@link PropertyValueException} ("not-null property references a null or transient value")
     * counts as {@code NOT_NULL} because Hibernate catches those before reaching SQL.
     * Returns {@code null} when nothing in the chain identifies the kind.
     *
     * <p>No cycle guard: Spring's own {@code ExceptionHandlerMethodResolver} recurses this exact
     * chain to pick the handler, so a cyclic one fails with a {@code StackOverflowError} before
     * the advice is ever entered. A guard here could not be reached, let alone tested.
     */
    private static ConstraintKind constraintKind(Throwable ex) {
        for (var cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation) {
                return violation.getKind();
            }
            if (cause instanceof PropertyValueException) {
                return ConstraintKind.NOT_NULL;
            }
        }
        return null;
    }

    private static String describe(ConstraintViolation<?> violation) {
        var path = String.valueOf(violation.getPropertyPath());
        return path.isEmpty() ? violation.getMessage() : path + ": " + violation.getMessage();
    }

    private static String describe(MessageSourceResolvable error) {
        return error instanceof FieldError fieldError
                ? fieldError.getField() + ": " + fieldError.getDefaultMessage()
                : String.valueOf(error.getDefaultMessage());
    }
}
