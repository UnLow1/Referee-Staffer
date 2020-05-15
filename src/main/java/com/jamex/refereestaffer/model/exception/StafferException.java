package com.jamex.refereestaffer.model.exception;

public class StafferException extends RuntimeException {

    private static final String NOT_ENOUGH_REFEREES = "Not enough referees to generate cast";

    public StafferException() {
        super(NOT_ENOUGH_REFEREES);
    }
}
