package com.jamex.refereestaffer.model.exception;

public class TeamNotFoundException extends RuntimeException {

    private static final String NOT_FOUND_WITH_NAME = "Team with name = \"%s\" has not been found";
    private static final String NOT_FOUND_WITH_ID = "Team with id = %d has not been found";

    public TeamNotFoundException(String name) {
        super(String.format(NOT_FOUND_WITH_NAME, name));
    }

    public TeamNotFoundException(Long id) {
        super(String.format(NOT_FOUND_WITH_ID, id));
    }
}
