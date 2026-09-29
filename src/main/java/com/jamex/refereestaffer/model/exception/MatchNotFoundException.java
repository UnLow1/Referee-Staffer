package com.jamex.refereestaffer.model.exception;

public class MatchNotFoundException extends RuntimeException {

    public static final String NOT_FOUND = "Match with id = %d has not been found";
    public static final String QUEUE_EMPTY = "No matches have been found for queue = %d";
    public static final String QUEUE_OUTSIDE_SEASON = "Queue %d is not part of the season";

    public MatchNotFoundException(Long id) {
        super(String.format(NOT_FOUND, id));
    }

    public MatchNotFoundException(short queue) {
        super(String.format(QUEUE_EMPTY, queue));
    }

    private MatchNotFoundException(String message) {
        super(message);
    }

    /**
     * Distinct wording for the staffing guard (RS-115): "no matches found for queue N" is the
     * one sentence that must not be confused with the deliberate empty cast, which also means
     * "no matches found (to staff)". This one says the queue itself does not exist.
     */
    public static MatchNotFoundException queueOutsideSeason(short queue) {
        return new MatchNotFoundException(String.format(QUEUE_OUTSIDE_SEASON, queue));
    }
}
