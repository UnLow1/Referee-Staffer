package com.jamex.refereestaffer.model.exception;

public class ImportException extends RuntimeException {

    public static final String ERROR_MESSAGE = "Exception occurred while importing file with name %s";

    public ImportException(String name) {
        super(String.format(ERROR_MESSAGE, name));
    }

    public ImportException(String name, Throwable cause) {
        super(String.format(ERROR_MESSAGE, name), cause);
    }
}
