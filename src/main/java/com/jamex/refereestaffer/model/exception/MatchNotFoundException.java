package com.jamex.refereestaffer.model.exception;

public class MatchNotFoundException extends RuntimeException {

    public static final String NOT_FOUND = "Match with id = %d has not been found";
    public static final String QUEUE_EMPTY = "No matches have been found for queue = %d";

    public MatchNotFoundException(Long id) {
        super(String.format(NOT_FOUND, id));
    }

    public MatchNotFoundException(short queue) {
        super(String.format(QUEUE_EMPTY, queue));
    }
}
