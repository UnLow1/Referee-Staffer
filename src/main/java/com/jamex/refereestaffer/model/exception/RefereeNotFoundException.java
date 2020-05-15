package com.jamex.refereestaffer.model.exception;

public class RefereeNotFoundException extends RuntimeException {

    private static final String NOT_FOUND = "Referee with name = \"%s %s\" has not been found";

    public RefereeNotFoundException(String firstName, String lastName) {
        super(String.format(NOT_FOUND, firstName, lastName));
    }
}
