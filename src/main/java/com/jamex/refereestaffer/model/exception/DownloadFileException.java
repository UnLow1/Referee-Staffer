package com.jamex.refereestaffer.model.exception;

public class DownloadFileException extends RuntimeException {

    public static final String ERROR_MESSAGE = "Exception occurred while downloading file with name %s";

    public DownloadFileException(String filename) {
        super(String.format(ERROR_MESSAGE, filename));
    }
}
