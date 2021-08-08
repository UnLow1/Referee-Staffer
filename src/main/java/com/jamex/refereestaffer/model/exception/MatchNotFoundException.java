package com.jamex.refereestaffer.model.exception;

public class MatchNotFoundException extends RuntimeException {

    public static final String NOT_FOUND = "Match with id = %d has not been found";

    public MatchNotFoundException(Long id) {
        super(String.format(NOT_FOUND, id));
    }
}
