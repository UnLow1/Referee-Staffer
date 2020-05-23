package com.jamex.refereestaffer.model.exception;

public class RefereeNotFoundException extends RuntimeException {

    private static final String NOT_FOUND_WITH_FIRSTNAME_AND_LASTNAME = "Referee with name = \"%s %s\" has not been found";
    private static final String NOT_FOUND_WITH_ID = "Referee with id = %d has not been found";

    public RefereeNotFoundException(String firstName, String lastName) {
        super(String.format(NOT_FOUND_WITH_FIRSTNAME_AND_LASTNAME, firstName, lastName));
    }

    public RefereeNotFoundException(Long id) {
        super(String.format(NOT_FOUND_WITH_ID, id));
    }
}
