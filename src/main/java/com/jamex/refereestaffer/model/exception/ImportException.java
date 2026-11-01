package com.jamex.refereestaffer.model.exception;

public class ImportException extends RuntimeException {

    public static final String ERROR_MESSAGE = "Exception occurred while importing file with name %s";

    /**
     * Message for a failure the parser can pin to a single CSV row. The row number makes an
     * otherwise opaque 400 actionable — the user can open the file and look at that line.
     */
    public static final String ROW_ERROR_MESSAGE = ERROR_MESSAGE + " (row %d: %s)";

    public ImportException(String name) {
        super(String.format(ERROR_MESSAGE, name));
    }

    public ImportException(String name, Throwable cause) {
        super(String.format(ERROR_MESSAGE, name), cause);
    }

    public ImportException(String name, long rowNumber, String detail) {
        super(String.format(ROW_ERROR_MESSAGE, name, rowNumber, detail));
    }
}
