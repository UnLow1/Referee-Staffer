package com.jamex.refereestaffer.model.exception;

/**
 * Request-body validation failure detected by hand-written checks that bean validation
 * cannot express — e.g. the id presence check on bulk list bodies, where container
 * element validation always runs in the Default group and cannot select OnUpdate.
 * The message follows the same {@code field: message} format the validation handlers
 * in RestExceptionHandler produce, and is mapped to a ProblemDetail 400 there.
 */
public class RequestValidationException extends RuntimeException {

    public RequestValidationException(String message) {
        super(message);
    }
}
